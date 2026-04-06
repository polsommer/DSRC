package script.systems.npcii;

import script.*;
import script.library.ai_lib;
import script.library.chat;
import script.library.utils;

public class npcii_brain extends script.base_script
{
    public npcii_brain()
    {
    }

    private static final String OBJVAR_ROOT = "systems.npcii.brain";
    private static final String OBJVAR_RECENT_SPEAKERS = OBJVAR_ROOT + ".recentSpeakers";
    private static final String OBJVAR_TOPIC_COUNTERS = OBJVAR_ROOT + ".topicCounters";
    private static final String OBJVAR_LAST_RESPONSE_TS = OBJVAR_ROOT + ".lastResponseTs";
    private static final String OBJVAR_PLAYER_COOLDOWNS = OBJVAR_ROOT + ".playerCooldowns";
    private static final String OBJVAR_LAST_LINE_HASH = OBJVAR_ROOT + ".lastLineHash";
    private static final String OBJVAR_LAST_LINE_TS = OBJVAR_ROOT + ".lastLineTs";
    private static final String OBJVAR_LEARN_ROOT = "systems.npcii.learn";
    private static final String OBJVAR_PREFERS_NEWS = OBJVAR_LEARN_ROOT + ".prefersNews";
    private static final String OBJVAR_PREFERS_COMBAT = OBJVAR_LEARN_ROOT + ".prefersCombat";
    private static final String OBJVAR_PREFERS_TRADE = OBJVAR_LEARN_ROOT + ".prefersTrade";
    private static final String OBJVAR_PLAYER_TRUST_ROOT = OBJVAR_LEARN_ROOT + ".playerTrustScore";

    private static final String SCRIPTVAR_LINE_WINDOW_START = "systems.npcii.brain.windowStart";
    private static final String SCRIPTVAR_LINE_WINDOW_COUNT = "systems.npcii.brain.windowCount";
    private static final String SCRIPTVAR_AI_LEVEL = "ai.level";

    private static final String OBJVAR_CREATURE_LEVEL = "creature_attribs.level";

    private static final String MSG_LEVEL_INTEGRITY_TICK = "npciiLevelIntegrityTick";

    private static final int MIN_NPCII_LEVEL = 10;
    private static final int MAX_NPCII_LEVEL = npcii_profile.MAX_PROGRESSIVE_COMBAT_LEVEL;

    private static final int MAX_RECENT_SPEAKERS = 8;
    private static final int MAX_LINES_PER_MINUTE = 5;
    private static final int PLAYER_TOPIC_REPEAT_MIN_SECONDS = 90;
    private static final int PLAYER_TOPIC_REPEAT_MAX_SECONDS = 180;
    private static final int TOPIC_RESPONSE_MIN_SECONDS = 12;
    private static final int IDENTICAL_LINE_COOLDOWN_SECONDS = 120;
    private static final float LEVEL_INTEGRITY_INTERVAL_SECONDS = 120.0f;

    private static final String TOPIC_GREETING = "greeting";
    private static final String TOPIC_WORK = "work";
    private static final String TOPIC_NEWS = "news";
    private static final String TOPIC_COMBAT = "combat";
    private static final String TOPIC_GOSSIP = "gossip";
    private static final String TOPIC_UNKNOWN = "unknown";

    private static final String[] TOPICS =
    {
        TOPIC_GREETING,
        TOPIC_WORK,
        TOPIC_NEWS,
        TOPIC_COMBAT,
        TOPIC_GOSSIP,
        TOPIC_UNKNOWN
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        validateAndRepairLevelState(self, "OnAttach");
        queueLevelIntegrityTick(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        validateAndRepairLevelState(self, "OnInitialize");
        queueLevelIntegrityTick(self);
        return SCRIPT_CONTINUE;
    }

    public int npciiLevelIntegrityTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || ai_lib.isAiDead(self) || isIncapacitated(self))
        {
            return SCRIPT_CONTINUE;
        }

        validateAndRepairLevelState(self, MSG_LEVEL_INTEGRITY_TICK);
        queueLevelIntegrityTick(self);
        return SCRIPT_CONTINUE;
    }

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || !isIdValid(speaker) || !exists(speaker))
        {
            return SCRIPT_CONTINUE;
        }
        if (ai_lib.isNpc(speaker) || !isPlayer(speaker))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isChatAllowed(self))
        {
            return SCRIPT_CONTINUE;
        }
        int now = getGameTime();
        if (!canSpeakInLineWindow(self, now))
        {
            return SCRIPT_CONTINUE;
        }

        String topic = determineTopic(text);
        if (!canRespondToTopic(self, speaker, topic, now))
        {
            return SCRIPT_CONTINUE;
        }

        String responseTopic = chooseWeightedTopic(self, topic);
        String line = chooseResponseLine(self, responseTopic);
        if (line == null || line.length() == 0)
        {
            return SCRIPT_CONTINUE;
        }
        if (!passesLineRepeatGuard(self, line, now))
        {
            return SCRIPT_CONTINUE;
        }

        chat.chat(self, line);
        updateStateAfterResponse(self, speaker, topic, responseTopic, line, now);
        return SCRIPT_CONTINUE;
    }

    private boolean isChatAllowed(obj_id self) throws InterruptedException
    {
        if (ai_lib.isInCombat(self))
        {
            return false;
        }
        if (getBehavior(self) > BEHAVIOR_CALM)
        {
            return false;
        }
        return !hasObjVar(self, "ai.pathingAwayFrom");
    }

    private boolean canSpeakInLineWindow(obj_id self, int now) throws InterruptedException
    {
        int windowStart = now;
        int count = 0;
        if (utils.hasScriptVar(self, SCRIPTVAR_LINE_WINDOW_START))
        {
            windowStart = utils.getIntScriptVar(self, SCRIPTVAR_LINE_WINDOW_START);
        }
        if (utils.hasScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT))
        {
            count = utils.getIntScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT);
        }
        if ((now - windowStart) >= 60)
        {
            utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_START, now);
            utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT, 0);
            return true;
        }
        return count < MAX_LINES_PER_MINUTE;
    }

    private boolean canRespondToTopic(obj_id self, obj_id speaker, String topic, int now) throws InterruptedException
    {
        String topicLastResponsePath = OBJVAR_LAST_RESPONSE_TS + "." + topic;
        if (hasObjVar(self, topicLastResponsePath))
        {
            int lastTopicResponse = getIntObjVar(self, topicLastResponsePath);
            if ((now - lastTopicResponse) < TOPIC_RESPONSE_MIN_SECONDS)
            {
                return false;
            }
        }

        String playerKey = speaker.toString();
        String playerTopicPath = OBJVAR_PLAYER_COOLDOWNS + "." + playerKey + "." + topic;
        int minRepeat = rand(PLAYER_TOPIC_REPEAT_MIN_SECONDS, PLAYER_TOPIC_REPEAT_MAX_SECONDS);
        if (hasObjVar(self, playerTopicPath))
        {
            int lastPlayerTopic = getIntObjVar(self, playerTopicPath);
            if ((now - lastPlayerTopic) < minRepeat)
            {
                return false;
            }
        }
        return true;
    }

    private String determineTopic(String text) throws InterruptedException
    {
        if (text == null)
        {
            return TOPIC_UNKNOWN;
        }
        String normalized = toLower(text);
        if (normalized.indexOf("hello") > -1 || normalized.indexOf("hi") > -1 || normalized.indexOf("greetings") > -1)
        {
            return TOPIC_GREETING;
        }
        if (normalized.indexOf("job") > -1 || normalized.indexOf("work") > -1 || normalized.indexOf("duty") > -1)
        {
            return TOPIC_WORK;
        }
        if (normalized.indexOf("news") > -1 || normalized.indexOf("rumor") > -1 || normalized.indexOf("story") > -1)
        {
            return TOPIC_NEWS;
        }
        if (normalized.indexOf("fight") > -1 || normalized.indexOf("battle") > -1 || normalized.indexOf("war") > -1)
        {
            return TOPIC_COMBAT;
        }
        if (normalized.indexOf("cantina") > -1 || normalized.indexOf("market") > -1 || normalized.indexOf("people") > -1)
        {
            return TOPIC_GOSSIP;
        }
        return TOPIC_UNKNOWN;
    }

    private String chooseWeightedTopic(obj_id self, String requestedTopic) throws InterruptedException
    {
        int totalWeight = 0;
        int[] weights = new int[TOPICS.length];
        for (int i = 0; i < TOPICS.length; i++)
        {
            String topic = TOPICS[i];
            int learned = 1;
            String counterPath = OBJVAR_TOPIC_COUNTERS + "." + topic;
            if (hasObjVar(self, counterPath))
            {
                learned += getIntObjVar(self, counterPath);
            }
            if (topic.equals(requestedTopic))
            {
                learned += 4;
            }
            weights[i] = learned;
            totalWeight += learned;
        }
        if (totalWeight <= 0)
        {
            return requestedTopic;
        }
        int roll = rand(1, totalWeight);
        int running = 0;
        for (int j = 0; j < TOPICS.length; j++)
        {
            running += weights[j];
            if (roll <= running)
            {
                return TOPICS[j];
            }
        }
        return requestedTopic;
    }

    private String chooseResponseLine(obj_id self, String topic) throws InterruptedException
    {
        int affinity = getTopicAffinity(self, topic);
        String[] pool = getPhrasePool(topic, affinity);
        if (pool == null || pool.length == 0)
        {
            return null;
        }
        return pool[rand(0, pool.length - 1)];
    }

    private int getTopicAffinity(obj_id self, String topic) throws InterruptedException
    {
        String counterPath = OBJVAR_TOPIC_COUNTERS + "." + topic;
        if (!hasObjVar(self, counterPath))
        {
            return 0;
        }
        return getIntObjVar(self, counterPath);
    }

    private String[] getPhrasePool(String topic, int affinity) throws InterruptedException
    {
        if (topic.equals(TOPIC_GREETING))
        {
            if (affinity > 4)
            {
                return new String[]{"Back again? Good. I remember reliable company.", "You know the routine; stay sharp and we get along fine.", "Familiar faces keep this post from getting dull."};
            }
            return new String[]{"Hello there. Keep it civil and we won't have trouble.", "Greetings. Eyes open, this district shifts fast.", "Welcome. Talk quick, patrol pace is tight."};
        }
        if (topic.equals(TOPIC_WORK))
        {
            if (affinity > 4)
            {
                return new String[]{"I keep refining my routes based on what people tell me.", "Patterns matter; each report makes my rounds better.", "Work never stops, but I am getting smarter about it."};
            }
            return new String[]{"Work is watch duty, reports, and moving before trouble starts.", "I monitor streets, routes, and the mood of every corner.", "My task is simple: prevent chaos before it grows teeth."};
        }
        if (topic.equals(TOPIC_NEWS))
        {
            return new String[]{"Latest chatter says trade lanes are restless tonight.", "News changes by the minute; trust only what repeats.", "I track rumors like weather: pressure rises before storms."};
        }
        if (topic.equals(TOPIC_COMBAT))
        {
            return new String[]{"If combat starts, conversation ends. Survival first.", "Battle talk is cheap; discipline is what keeps you breathing.", "I've seen enough fights to value preparation over bravado."};
        }
        if (topic.equals(TOPIC_GOSSIP))
        {
            return new String[]{"Cantina stories travel faster than official bulletins.", "Markets hear everything first and verify nothing.", "People whisper, I listen, then I separate signal from noise."};
        }
        return new String[]{"I am still sorting what matters. Ask again a different way.", "Noted. Give me clearer context and I can answer better.", "I hear you. I just need more pattern before I commit."};
    }

    private boolean passesLineRepeatGuard(obj_id self, String line, int now) throws InterruptedException
    {
        int hash = line.hashCode();
        if (hasObjVar(self, OBJVAR_LAST_LINE_HASH) && hasObjVar(self, OBJVAR_LAST_LINE_TS))
        {
            int lastHash = getIntObjVar(self, OBJVAR_LAST_LINE_HASH);
            int lastTs = getIntObjVar(self, OBJVAR_LAST_LINE_TS);
            if (lastHash == hash && (now - lastTs) < IDENTICAL_LINE_COOLDOWN_SECONDS)
            {
                return false;
            }
        }
        return true;
    }

    private void updateStateAfterResponse(obj_id self, obj_id speaker, String requestedTopic, String responseTopic, String line, int now) throws InterruptedException
    {
        String requestedCounter = OBJVAR_TOPIC_COUNTERS + "." + requestedTopic;
        int requestedCount = hasObjVar(self, requestedCounter) ? getIntObjVar(self, requestedCounter) : 0;
        setObjVar(self, requestedCounter, requestedCount + 1);

        if (!responseTopic.equals(requestedTopic))
        {
            String responseCounter = OBJVAR_TOPIC_COUNTERS + "." + responseTopic;
            int responseCount = hasObjVar(self, responseCounter) ? getIntObjVar(self, responseCounter) : 0;
            setObjVar(self, responseCounter, responseCount + 1);
        }

        setObjVar(self, OBJVAR_LAST_RESPONSE_TS + "." + requestedTopic, now);
        setObjVar(self, OBJVAR_LAST_RESPONSE_TS + "." + responseTopic, now);

        String playerKey = speaker.toString();
        setObjVar(self, OBJVAR_PLAYER_COOLDOWNS + "." + playerKey + "." + requestedTopic, now);
        setObjVar(self, OBJVAR_PLAYER_COOLDOWNS + "." + playerKey + "." + responseTopic, now);

        setObjVar(self, OBJVAR_LAST_LINE_HASH, line.hashCode());
        setObjVar(self, OBJVAR_LAST_LINE_TS, now);

        rememberSpeaker(self, playerKey);
        updateLearnedPreferences(self, speaker, requestedTopic, responseTopic);
        incrementLineWindow(self, now);
    }

    private void updateLearnedPreferences(obj_id self, obj_id speaker, String requestedTopic, String responseTopic) throws InterruptedException
    {
        int newsDelta = 0;
        int combatDelta = 0;
        int tradeDelta = 0;
        int trustDelta = 0;

        if (TOPIC_NEWS.equals(requestedTopic) || TOPIC_NEWS.equals(responseTopic))
        {
            newsDelta += 2;
            trustDelta += 1;
        }
        if (TOPIC_COMBAT.equals(requestedTopic) || TOPIC_COMBAT.equals(responseTopic))
        {
            combatDelta += 2;
            trustDelta -= 1;
        }
        if (TOPIC_WORK.equals(requestedTopic) || TOPIC_WORK.equals(responseTopic) || TOPIC_GOSSIP.equals(requestedTopic) || TOPIC_GOSSIP.equals(responseTopic))
        {
            tradeDelta += 2;
            trustDelta += 1;
        }
        if (TOPIC_UNKNOWN.equals(requestedTopic))
        {
            trustDelta -= 1;
        }

        if (newsDelta != 0)
        {
            setObjVar(self, OBJVAR_PREFERS_NEWS, clampLearningValue(getIntObjVarSafe(self, OBJVAR_PREFERS_NEWS) + newsDelta, -30, 30));
        }
        if (combatDelta != 0)
        {
            setObjVar(self, OBJVAR_PREFERS_COMBAT, clampLearningValue(getIntObjVarSafe(self, OBJVAR_PREFERS_COMBAT) + combatDelta, -30, 30));
        }
        if (tradeDelta != 0)
        {
            setObjVar(self, OBJVAR_PREFERS_TRADE, clampLearningValue(getIntObjVarSafe(self, OBJVAR_PREFERS_TRADE) + tradeDelta, -30, 30));
        }

        String trustPath = OBJVAR_PLAYER_TRUST_ROOT + "." + speaker;
        if (trustDelta != 0)
        {
            setObjVar(self, trustPath, clampLearningValue(getIntObjVarSafe(self, trustPath) + trustDelta, 0, 100));
        }

        if (newsDelta != 0 || combatDelta != 0 || tradeDelta != 0 || trustDelta != 0)
        {
            LOG("npcii_learning_brain_update", self + ";news=" + newsDelta + ";combat=" + combatDelta + ";trade=" + tradeDelta + ";trust=" + trustDelta + ";player=" + speaker);
        }
    }

    private int getIntObjVarSafe(obj_id self, String path) throws InterruptedException
    {
        if (!hasObjVar(self, path))
        {
            return 0;
        }
        return getIntObjVar(self, path);
    }

    private int clampLearningValue(int value, int min, int max) throws InterruptedException
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    private void rememberSpeaker(obj_id self, String speakerId) throws InterruptedException
    {
        String[] recent = new String[0];
        if (hasObjVar(self, OBJVAR_RECENT_SPEAKERS))
        {
            recent = getStringArrayObjVar(self, OBJVAR_RECENT_SPEAKERS);
        }

        int existingIndex = -1;
        for (int i = 0; i < recent.length; i++)
        {
            if (recent[i] != null && recent[i].equals(speakerId))
            {
                existingIndex = i;
                break;
            }
        }

        String[] updated;
        int targetLength = recent.length + (existingIndex > -1 ? 0 : 1);
        if (targetLength > MAX_RECENT_SPEAKERS)
        {
            targetLength = MAX_RECENT_SPEAKERS;
        }
        updated = new String[targetLength];
        updated[0] = speakerId;

        int writeIndex = 1;
        for (int j = 0; j < recent.length && writeIndex < targetLength; j++)
        {
            if (j == existingIndex)
            {
                continue;
            }
            updated[writeIndex] = recent[j];
            writeIndex++;
        }

        setObjVar(self, OBJVAR_RECENT_SPEAKERS, updated);
    }

    private void incrementLineWindow(obj_id self, int now) throws InterruptedException
    {
        int windowStart = now;
        int count = 0;
        if (utils.hasScriptVar(self, SCRIPTVAR_LINE_WINDOW_START))
        {
            windowStart = utils.getIntScriptVar(self, SCRIPTVAR_LINE_WINDOW_START);
        }
        if (utils.hasScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT))
        {
            count = utils.getIntScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT);
        }

        if ((now - windowStart) >= 60)
        {
            utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_START, now);
            utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT, 1);
            return;
        }

        utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_START, windowStart);
        utils.setScriptVar(self, SCRIPTVAR_LINE_WINDOW_COUNT, count + 1);
    }

    private int getSafeProgressionCounter(obj_id self, String objvar) throws InterruptedException
    {
        if (!hasObjVar(self, objvar))
        {
            return 0;
        }
        int value = getIntObjVar(self, objvar);
        if (value < 0 || value > 1000000)
        {
            return 0;
        }
        return value;
    }

    private int computeProgressionLevel(obj_id self) throws InterruptedException
    {
        int forageTotal = getSafeProgressionCounter(self, npcii_activity_controller.OBJVAR_FORAGE_SUCCESS_TOTAL);
        int patrolTotal = getSafeProgressionCounter(self, npcii_activity_controller.OBJVAR_PATROL_SUCCESS_TOTAL);
        int combatWinTotal = getSafeProgressionCounter(self, npcii_activity_controller.OBJVAR_COMBAT_WIN_TOTAL);

        int levelFromForage = forageTotal / npcii_profile.FORAGE_SUCCESS_PER_LEVEL;
        int levelFromPatrol = patrolTotal / npcii_profile.PATROL_SUCCESS_PER_LEVEL;
        int levelFromCombat = combatWinTotal / npcii_profile.COMBAT_WIN_PER_LEVEL;

        int computed = MIN_NPCII_LEVEL + levelFromForage + levelFromPatrol + levelFromCombat;
        if (computed < MIN_NPCII_LEVEL)
        {
            return MIN_NPCII_LEVEL;
        }
        if (computed > MAX_NPCII_LEVEL)
        {
            return MAX_NPCII_LEVEL;
        }
        return computed;
    }

    private void queueLevelIntegrityTick(obj_id self) throws InterruptedException
    {
        messageTo(self, MSG_LEVEL_INTEGRITY_TICK, null, LEVEL_INTEGRITY_INTERVAL_SECONDS, false);
    }

    private void validateAndRepairLevelState(obj_id self, String source) throws InterruptedException
    {
        int currentLevel = getLevel(self);
        int computedLevel = computeProgressionLevel(self);
        int targetLevel = computedLevel;
        if (currentLevel < MIN_NPCII_LEVEL)
        {
            targetLevel = MIN_NPCII_LEVEL;
            LOG("npcII", "LEVEL_CORRECTION: source=" + source + " npc=" + self + " currentLevel=" + currentLevel + " correctedLevel=" + targetLevel);
        }

        int objVarLevel = hasObjVar(self, OBJVAR_CREATURE_LEVEL) ? getIntObjVar(self, OBJVAR_CREATURE_LEVEL) : -1;
        int scriptVarLevel = utils.hasScriptVar(self, SCRIPTVAR_AI_LEVEL) ? utils.getIntScriptVar(self, SCRIPTVAR_AI_LEVEL) : -1;

        boolean levelChanged = false;
        boolean corrected = false;
        if (currentLevel != targetLevel)
        {
            setLevel(self, targetLevel);
            corrected = true;
            levelChanged = true;
        }
        if (objVarLevel != targetLevel)
        {
            setObjVar(self, OBJVAR_CREATURE_LEVEL, targetLevel);
            corrected = true;
        }
        if (scriptVarLevel != targetLevel)
        {
            utils.setScriptVar(self, SCRIPTVAR_AI_LEVEL, targetLevel);
            corrected = true;
        }

        if (levelChanged)
        {
            // Reapply level-driven values so downstream combat systems can refresh derived stats from synced fields.
            setLevel(self, targetLevel);
        }
        if (corrected)
        {
            LOG("npcII", "LEVEL_STATE_REPAIRED: source=" + source + " npc=" + self + " level=" + targetLevel + " computedLevel=" + computedLevel + " objvarLevel=" + objVarLevel + " scriptvarLevel=" + scriptVarLevel);
        }
    }

}

package script.systems.npcii;

import script.*;
import script.library.ai_lib;
import script.library.factions;
import script.library.utils;

public class npcii_activity_controller extends script.base_script
{
    public npcii_activity_controller()
    {
    }

    public static final String OBJVAR_ROOT = "systems.npcii.profile";
    public static final String OBJVAR_HOME = OBJVAR_ROOT + ".home";
    public static final String OBJVAR_ACTIVITY = OBJVAR_ROOT + ".activity";
    public static final String OBJVAR_ACTIVITY_UNTIL = OBJVAR_ROOT + ".activityUntil";
    public static final String OBJVAR_TARGET = OBJVAR_ROOT + ".target";
    public static final String OBJVAR_CHASE_START = OBJVAR_ROOT + ".chaseStart";
    public static final String OBJVAR_FORAGE_WEIGHT = OBJVAR_ROOT + ".weights.forage";
    public static final String OBJVAR_PATROL_WEIGHT = OBJVAR_ROOT + ".weights.patrol";
    public static final String OBJVAR_COMBAT_WEIGHT = OBJVAR_ROOT + ".weights.combat";
    public static final String OBJVAR_PROGRESSION_ROOT = OBJVAR_ROOT + ".progression";
    public static final String OBJVAR_FORAGE_SUCCESS_TOTAL = OBJVAR_PROGRESSION_ROOT + ".forageSuccessTotal";
    public static final String OBJVAR_PATROL_SUCCESS_TOTAL = OBJVAR_PROGRESSION_ROOT + ".patrolSuccessTotal";
    public static final String OBJVAR_COMBAT_WIN_TOTAL = OBJVAR_PROGRESSION_ROOT + ".combatWinTotal";

    public static final String OBJVAR_LEARN_ROOT = "systems.npcii.learn";
    public static final String OBJVAR_PREFERS_NEWS = OBJVAR_LEARN_ROOT + ".prefersNews";
    public static final String OBJVAR_PREFERS_COMBAT = OBJVAR_LEARN_ROOT + ".prefersCombat";
    public static final String OBJVAR_PLAYER_TRUST_ROOT = OBJVAR_LEARN_ROOT + ".playerTrustScore";

    public static final String MSG_TICK = "npciiActivityTick";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        initializeProfile(self);
        messageTo(self, MSG_TICK, null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        initializeProfile(self);
        messageTo(self, MSG_TICK, null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int npciiActivityTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || ai_lib.isAiDead(self) || isIncapacitated(self))
        {
            return SCRIPT_CONTINUE;
        }

        initializeProfile(self);
        enforceProgressiveCombatLevel(self);
        enforceLeashAndDisengage(self);

        int now = getGameTime();
        if (!hasObjVar(self, OBJVAR_ACTIVITY) || getIntObjVar(self, OBJVAR_ACTIVITY_UNTIL) <= now)
        {
            setNewActivity(self, now);
        }

        String activity = getStringObjVar(self, OBJVAR_ACTIVITY);
        if (npcii_profile.ACTIVITY_FORAGE.equals(activity))
        {
            runForage(self);
        }
        else if (npcii_profile.ACTIVITY_COMBAT.equals(activity))
        {
            runCombatScan(self, now);
        }
        else
        {
            runPatrol(self);
        }

        messageTo(self, MSG_TICK, null, npcii_profile.TICK_SECONDS, false);
        return SCRIPT_CONTINUE;
    }

    private void initializeProfile(obj_id self) throws InterruptedException
    {
        enforceProgressiveCombatLevel(self);
        if (!hasObjVar(self, OBJVAR_HOME))
        {
            setObjVar(self, OBJVAR_HOME, getLocation(self));
        }
        if (!hasObjVar(self, OBJVAR_FORAGE_WEIGHT))
        {
            setObjVar(self, OBJVAR_FORAGE_WEIGHT, 22);
        }
        if (!hasObjVar(self, OBJVAR_PATROL_WEIGHT))
        {
            setObjVar(self, OBJVAR_PATROL_WEIGHT, 28);
        }
        if (!hasObjVar(self, OBJVAR_COMBAT_WEIGHT))
        {
            setObjVar(self, OBJVAR_COMBAT_WEIGHT, 20);
        }
    }

    private void enforceProgressiveCombatLevel(obj_id self) throws InterruptedException
    {
        int targetLevel = getProgressionCombatLevel(self);
        int currentLevel = getLevel(self);
        if (currentLevel < npcii_profile.BASELINE_COMBAT_LEVEL)
        {
            targetLevel = npcii_profile.BASELINE_COMBAT_LEVEL;
        }
        int objVarLevel = hasObjVar(self, "creature_attribs.level") ? getIntObjVar(self, "creature_attribs.level") : -1;
        int scriptVarLevel = utils.hasScriptVar(self, "ai.level") ? utils.getIntScriptVar(self, "ai.level") : -1;
        boolean levelChanged = false;
        if (currentLevel != targetLevel)
        {
            setLevel(self, targetLevel);
            levelChanged = true;
        }
        if (objVarLevel != targetLevel)
        {
            setObjVar(self, "creature_attribs.level", targetLevel);
        }
        if (scriptVarLevel != targetLevel)
        {
            utils.setScriptVar(self, "ai.level", targetLevel);
        }
        if (levelChanged)
        {
            setLevel(self, targetLevel);
        }
    }


    private int getProgressionCombatLevel(obj_id self) throws InterruptedException
    {
        int forageTotal = getSafeProgressionCounter(self, OBJVAR_FORAGE_SUCCESS_TOTAL);
        int patrolTotal = getSafeProgressionCounter(self, OBJVAR_PATROL_SUCCESS_TOTAL);
        int combatWinTotal = getSafeProgressionCounter(self, OBJVAR_COMBAT_WIN_TOTAL);

        int levelFromForage = forageTotal / npcii_profile.FORAGE_SUCCESS_PER_LEVEL;
        int levelFromPatrol = patrolTotal / npcii_profile.PATROL_SUCCESS_PER_LEVEL;
        int levelFromCombat = combatWinTotal / npcii_profile.COMBAT_WIN_PER_LEVEL;

        int computed = npcii_profile.BASELINE_COMBAT_LEVEL + levelFromForage + levelFromPatrol + levelFromCombat;
        if (computed < npcii_profile.BASELINE_COMBAT_LEVEL)
        {
            return npcii_profile.BASELINE_COMBAT_LEVEL;
        }
        return Math.min(computed, npcii_profile.MAX_PROGRESSIVE_COMBAT_LEVEL);
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

    private void setNewActivity(obj_id self, int now) throws InterruptedException
    {
        int forageWeight = getIntObjVar(self, OBJVAR_FORAGE_WEIGHT) + (isInWindow(npcii_profile.FORAGE_WINDOWS) ? 15 : -8);
        int patrolWeight = getIntObjVar(self, OBJVAR_PATROL_WEIGHT) + 6;
        int combatWeight = getIntObjVar(self, OBJVAR_COMBAT_WEIGHT) + (isInWindow(npcii_profile.COMBAT_WINDOWS) ? 18 : -12);

        int learningForageBias = clampLearningBias(getLearningPreference(self, OBJVAR_PREFERS_NEWS) / 6, -4, 4);
        int learningCombatBias = clampLearningBias((getLearningPreference(self, OBJVAR_PREFERS_COMBAT) / 5) + (getAverageTrustBias(self) / 8), -6, 6);
        int learningPatrolBias = clampLearningBias((learningForageBias + learningCombatBias) / -2, -4, 4);

        forageWeight += learningForageBias;
        patrolWeight += learningPatrolBias;
        combatWeight += learningCombatBias;

        if (learningForageBias != 0 || learningPatrolBias != 0 || learningCombatBias != 0)
        {
            LOG("npcii_learning_activity_bias", self + ";forage=" + learningForageBias + ";patrol=" + learningPatrolBias + ";combat=" + learningCombatBias);
        }

        forageWeight = Math.max(1, forageWeight);
        patrolWeight = Math.max(1, patrolWeight);
        combatWeight = Math.max(1, combatWeight);

        int total = forageWeight + patrolWeight + combatWeight;
        int roll = rand(1, total);

        String activity = npcii_profile.ACTIVITY_PATROL;
        if (roll <= forageWeight)
        {
            activity = npcii_profile.ACTIVITY_FORAGE;
        }
        else if (roll > forageWeight + patrolWeight)
        {
            activity = npcii_profile.ACTIVITY_COMBAT;
        }

        setObjVar(self, OBJVAR_ACTIVITY, activity);
        setObjVar(self, OBJVAR_ACTIVITY_UNTIL, now + rand(24, 65));
    }

    private boolean isInWindow(int[][] windows) throws InterruptedException
    {
        int hour = (getGameTime() / 3600) % 24;
        for (int i = 0; i < windows.length; i++)
        {
            if (hour >= windows[i][0] && hour <= windows[i][1])
            {
                return true;
            }
        }
        return false;
    }

    private void runForage(obj_id self) throws InterruptedException
    {
        location home = getLocationObjVar(self, OBJVAR_HOME);
        if (home == null)
        {
            home = getLocation(self);
            setObjVar(self, OBJVAR_HOME, home);
        }

        location dest = (location)home.clone();
        dest.x += rand(-npcii_profile.ROAM_RADIUS, npcii_profile.ROAM_RADIUS);
        dest.z += rand(-npcii_profile.ROAM_RADIUS, npcii_profile.ROAM_RADIUS);
        pathTo(self, dest);

        if (rand(1, 100) <= 35)
        {
            rewardProgression(self, npcii_profile.ACTIVITY_FORAGE, true);
        }
    }

    private void runPatrol(obj_id self) throws InterruptedException
    {
        location home = getLocationObjVar(self, OBJVAR_HOME);
        if (home == null)
        {
            home = getLocation(self);
            setObjVar(self, OBJVAR_HOME, home);
        }

        location waypoint = (location)home.clone();
        waypoint.x += rand(-npcii_profile.PATROL_STEP_RADIUS, npcii_profile.PATROL_STEP_RADIUS);
        waypoint.z += rand(-npcii_profile.PATROL_STEP_RADIUS, npcii_profile.PATROL_STEP_RADIUS);
        pathTo(self, waypoint);

        if (rand(1, 100) <= 25)
        {
            rewardProgression(self, npcii_profile.ACTIVITY_PATROL, true);
        }
    }

    private void runCombatScan(obj_id self, int now) throws InterruptedException
    {
        obj_id current = getCombatTarget(self);
        if (isIdValid(current) && exists(current) && isApprovedTarget(self, current))
        {
            if (!hasObjVar(self, OBJVAR_CHASE_START))
            {
                setObjVar(self, OBJVAR_CHASE_START, now);
            }
            return;
        }

        obj_id target = findApprovedTarget(self);
        if (!isIdValid(target))
        {
            rewardProgression(self, npcii_profile.ACTIVITY_COMBAT, false);
            return;
        }

        setObjVar(self, OBJVAR_TARGET, target);
        setObjVar(self, OBJVAR_CHASE_START, now);
        startCombat(self, target);
        addHate(self, target, rand(5, 20));
        rewardProgression(self, npcii_profile.ACTIVITY_COMBAT, true);
    }

    private obj_id findApprovedTarget(obj_id self) throws InterruptedException
    {
        obj_id[] nearby = getObjectsInRange(getLocation(self), npcii_profile.COMBAT_SCAN_RADIUS);
        if (nearby == null || nearby.length == 0)
        {
            return obj_id.NULL_ID;
        }

        for (int i = 0; i < nearby.length; i++)
        {
            obj_id candidate = nearby[i];
            if (isApprovedTarget(self, candidate))
            {
                return candidate;
            }
        }
        return obj_id.NULL_ID;
    }

    private boolean isApprovedTarget(obj_id self, obj_id target) throws InterruptedException
    {
        if (!isIdValid(target) || !exists(target) || target == self)
        {
            return false;
        }
        if (!isMob(target) || ai_lib.isAiDead(target) || isIncapacitated(target) || isInvulnerable(target))
        {
            return false;
        }
        if (hasObjVar(target, "systems.npcii.isManagedNpc"))
        {
            return false;
        }
        if (isProtectedQuestActor(target))
        {
            return false;
        }

        if (isApprovedFactionEnemyTarget(self, target))
        {
            return true;
        }

        return isApprovedHostileCreatureTarget(target);
    }

    private boolean isApprovedFactionEnemyTarget(obj_id self, obj_id target) throws InterruptedException
    {
        String targetFaction = factions.getFaction(target);
        if (targetFaction == null || targetFaction.length() == 0)
        {
            return false;
        }

        boolean approvedFaction = false;
        for (int i = 0; i < npcii_profile.APPROVED_FACTIONS.length; i++)
        {
            if (targetFaction.equals(npcii_profile.APPROVED_FACTIONS[i]))
            {
                approvedFaction = true;
                break;
            }
        }
        if (!approvedFaction)
        {
            return false;
        }

        int factionStatus = factions.getFactionStatus(self, target);
        return factionStatus == factions.STATUS_ENEMY;
    }

    private boolean isApprovedHostileCreatureTarget(obj_id target) throws InterruptedException
    {
        if (!ai_lib.isMonster(target) && !ai_lib.isAnimal(target))
        {
            return false;
        }

        return ai_lib.isAggro(target);
    }

    private boolean isProtectedQuestActor(obj_id target) throws InterruptedException
    {
        for (int i = 0; i < npcii_profile.PROTECTED_SCRIPTS.length; i++)
        {
            if (hasScript(target, npcii_profile.PROTECTED_SCRIPTS[i]))
            {
                return true;
            }
        }
        for (int j = 0; j < npcii_profile.SCRIPTED_PROTECTED_OBJVARS.length; j++)
        {
            if (hasObjVar(target, npcii_profile.SCRIPTED_PROTECTED_OBJVARS[j]))
            {
                return true;
            }
        }
        return false;
    }

    private void enforceLeashAndDisengage(obj_id self) throws InterruptedException
    {
        location home = getLocationObjVar(self, OBJVAR_HOME);
        if (home == null)
        {
            home = getLocation(self);
            setObjVar(self, OBJVAR_HOME, home);
            return;
        }

        float leashDistance = getDistance(getLocation(self), home);
        int now = getGameTime();
        boolean chaseTimeout = hasObjVar(self, OBJVAR_CHASE_START) && (now - getIntObjVar(self, OBJVAR_CHASE_START)) > npcii_profile.CHASE_TIMEOUT_SECONDS;

        if (leashDistance > npcii_profile.LEASH_MAX_DISTANCE || chaseTimeout)
        {
            setCombatTarget(self, obj_id.NULL_ID);
            pathTo(self, home);
            setObjVar(self, OBJVAR_ACTIVITY, npcii_profile.ACTIVITY_PATROL);
            setObjVar(self, OBJVAR_ACTIVITY_UNTIL, now + rand(14, 30));
            if (hasObjVar(self, OBJVAR_TARGET))
            {
                removeObjVar(self, OBJVAR_TARGET);
            }
            if (hasObjVar(self, OBJVAR_CHASE_START))
            {
                removeObjVar(self, OBJVAR_CHASE_START);
            }
        }
    }

    private void rewardProgression(obj_id self, String activity, boolean success) throws InterruptedException
    {
        int delta = success ? 1 : -1;
        int priorLevel = getLevel(self);
        if (npcii_profile.ACTIVITY_FORAGE.equals(activity))
        {
            setObjVar(self, OBJVAR_FORAGE_WEIGHT, clamp(getIntObjVar(self, OBJVAR_FORAGE_WEIGHT) + delta, npcii_profile.MIN_BEHAVIOR_WEIGHT, npcii_profile.FORAGE_WEIGHT_CAP));
            if (success)
            {
                setObjVar(self, OBJVAR_FORAGE_SUCCESS_TOTAL, getSafeProgressionCounter(self, OBJVAR_FORAGE_SUCCESS_TOTAL) + 1);
            }
        }
        else if (npcii_profile.ACTIVITY_PATROL.equals(activity))
        {
            setObjVar(self, OBJVAR_PATROL_WEIGHT, clamp(getIntObjVar(self, OBJVAR_PATROL_WEIGHT) + delta, npcii_profile.MIN_BEHAVIOR_WEIGHT, npcii_profile.PATROL_WEIGHT_CAP));
            if (success)
            {
                setObjVar(self, OBJVAR_PATROL_SUCCESS_TOTAL, getSafeProgressionCounter(self, OBJVAR_PATROL_SUCCESS_TOTAL) + 1);
            }
        }
        else if (npcii_profile.ACTIVITY_COMBAT.equals(activity))
        {
            setObjVar(self, OBJVAR_COMBAT_WEIGHT, clamp(getIntObjVar(self, OBJVAR_COMBAT_WEIGHT) + delta, npcii_profile.MIN_BEHAVIOR_WEIGHT, npcii_profile.COMBAT_WEIGHT_CAP));
            if (success)
            {
                setObjVar(self, OBJVAR_COMBAT_WIN_TOTAL, getSafeProgressionCounter(self, OBJVAR_COMBAT_WIN_TOTAL) + 1);
            }
        }

        int targetLevel = getProgressionCombatLevel(self);
        if (priorLevel < npcii_profile.BASELINE_COMBAT_LEVEL)
        {
            targetLevel = npcii_profile.BASELINE_COMBAT_LEVEL;
        }
        if (targetLevel != priorLevel)
        {
            setLevel(self, targetLevel);
            setObjVar(self, "creature_attribs.level", targetLevel);
            utils.setScriptVar(self, "ai.level", targetLevel);
            setLevel(self, targetLevel);
        }
    }

    private int getLearningPreference(obj_id self, String path) throws InterruptedException
    {
        if (!hasObjVar(self, path))
        {
            return 0;
        }
        return getIntObjVar(self, path);
    }

    private int getAverageTrustBias(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_LEARN_ROOT + ".playerTrustScore"))
        {
            return 0;
        }
        obj_var_list trustedIds = getObjVarList(self, OBJVAR_PLAYER_TRUST_ROOT);
        if (trustedIds == null || trustedIds.getNumItems() == 0)
        {
            return 0;
        }

        int count = 0;
        int sum = 0;
        int numItems = trustedIds.getNumItems();
        for (int i = 0; i < numItems; i++)
        {
            obj_var trustVar = trustedIds.getObjVar(i);
            if (trustVar == null)
            {
                continue;
            }
            String playerId = trustVar.getName();
            if (playerId == null || playerId.length() == 0)
            {
                continue;
            }
            String trustPath = OBJVAR_PLAYER_TRUST_ROOT + "." + playerId;
            if (!hasObjVar(self, trustPath))
            {
                continue;
            }
            sum += clampLearningBias(getIntObjVar(self, trustPath), 0, 100);
            count++;
            if (count >= 6)
            {
                break;
            }
        }
        if (count == 0)
        {
            return 0;
        }
        return clampLearningBias((sum / count) - 50, -20, 20);
    }

    private int clampLearningBias(int value, int min, int max) throws InterruptedException
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

    private int clamp(int value, int min, int max) throws InterruptedException
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return Math.min(value, npcii_profile.MAX_BEHAVIOR_WEIGHT);
    }
}

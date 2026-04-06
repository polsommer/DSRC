package script.systems.npcii;

import script.dictionary;
import script.location;
import script.obj_id;
import script.obj_var;
import script.obj_var_list;
import script.library.ai_lib;
import script.library.npc_economy;

public class npcii_bazaar_agent extends script.base_script
{
    public npcii_bazaar_agent()
    {
    }

    public static final String OBJVAR_ROOT = "systems.npcii.bazaar";
    public static final String OBJVAR_STATE = OBJVAR_ROOT + ".state";
    public static final String OBJVAR_NEXT_STATE_TS = OBJVAR_ROOT + ".nextStateAt";
    public static final String OBJVAR_NEXT_TRADE_TS = OBJVAR_ROOT + ".nextTradeAt";
    public static final String OBJVAR_DAILY_DAY = OBJVAR_ROOT + ".daily.day";
    public static final String OBJVAR_DAILY_SPEND = OBJVAR_ROOT + ".daily.spend";
    public static final String OBJVAR_DAILY_TRADES = OBJVAR_ROOT + ".daily.trades";
    public static final String OBJVAR_PATROL_ANCHOR = OBJVAR_ROOT + ".patrolAnchor";
    public static final String OBJVAR_TARGET_VENDOR = OBJVAR_ROOT + ".targetVendor";
    public static final String OBJVAR_TARGET_LISTING = OBJVAR_ROOT + ".targetListing";
    public static final String OBJVAR_TARGET_ACTION = OBJVAR_ROOT + ".targetAction";
    public static final String OBJVAR_TARGET_PRICE = OBJVAR_ROOT + ".targetPrice";
    public static final String OBJVAR_TARGET_REASON = OBJVAR_ROOT + ".targetReason";

    public static final String OBJVAR_LEARN_ROOT = "systems.npcii.learn";
    public static final String OBJVAR_PREFERS_TRADE = OBJVAR_LEARN_ROOT + ".prefersTrade";
    public static final String OBJVAR_PLAYER_TRUST_ROOT = OBJVAR_LEARN_ROOT + ".playerTrustScore";

    public static final String MSG_TICK = "npciiBazaarTick";

    public static final String STATE_PATROL = "PATROL";
    public static final String STATE_SEARCH_BAZAAR = "SEARCH_BAZAAR";
    public static final String STATE_BUY = "BUY";
    public static final String STATE_SELL = "SELL";
    public static final String STATE_RESUME_PATROL = "RESUME_PATROL";

    public static final float TICK_SECONDS = 6.0f;
    public static final float SEARCH_RADIUS = 48.0f;
    public static final float INTERACTION_RANGE = 8.0f;
    public static final int DAILY_BUDGET_LIMIT = 3000;
    public static final int DAILY_TRADE_LIMIT = 12;
    public static final int TRADE_COOLDOWN_MIN_SECONDS = 45;
    public static final int TRADE_COOLDOWN_MAX_SECONDS = 120;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        initialize(self);
        messageTo(self, MSG_TICK, null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        initialize(self);
        messageTo(self, MSG_TICK, null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int npciiBazaarTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || ai_lib.isAiDead(self) || isIncapacitated(self))
        {
            return SCRIPT_CONTINUE;
        }

        initialize(self);
        resetDailyCountersIfNeeded(self);

        String state = getStringObjVar(self, OBJVAR_STATE);
        if (state == null || state.length() == 0)
        {
            state = STATE_PATROL;
            setObjVar(self, OBJVAR_STATE, state);
        }

        if (STATE_PATROL.equals(state))
        {
            handlePatrol(self);
        }
        else if (STATE_SEARCH_BAZAAR.equals(state))
        {
            handleSearchBazaar(self);
        }
        else if (STATE_BUY.equals(state) || STATE_SELL.equals(state))
        {
            handleTradeState(self, state);
        }
        else if (STATE_RESUME_PATROL.equals(state))
        {
            handleResumePatrol(self);
        }
        else
        {
            setObjVar(self, OBJVAR_STATE, STATE_PATROL);
        }

        messageTo(self, MSG_TICK, null, TICK_SECONDS, false);
        return SCRIPT_CONTINUE;
    }

    private void initialize(obj_id self) throws InterruptedException
    {
        npc_economy.initializeEconomy(self);
        if (!hasObjVar(self, OBJVAR_STATE))
        {
            setObjVar(self, OBJVAR_STATE, STATE_PATROL);
        }
        if (!hasObjVar(self, OBJVAR_NEXT_STATE_TS))
        {
            setObjVar(self, OBJVAR_NEXT_STATE_TS, 0);
        }
        if (!hasObjVar(self, OBJVAR_NEXT_TRADE_TS))
        {
            setObjVar(self, OBJVAR_NEXT_TRADE_TS, 0);
        }
        if (!hasObjVar(self, OBJVAR_DAILY_DAY))
        {
            setObjVar(self, OBJVAR_DAILY_DAY, -1);
        }
        if (!hasObjVar(self, OBJVAR_DAILY_SPEND))
        {
            setObjVar(self, OBJVAR_DAILY_SPEND, 0);
        }
        if (!hasObjVar(self, OBJVAR_DAILY_TRADES))
        {
            setObjVar(self, OBJVAR_DAILY_TRADES, 0);
        }
        if (!hasObjVar(self, OBJVAR_PATROL_ANCHOR))
        {
            setObjVar(self, OBJVAR_PATROL_ANCHOR, getLocation(self));
        }
    }

    private void handlePatrol(obj_id self) throws InterruptedException
    {
        maybePathNearAnchor(self);
        int now = getGameTime();
        int nextStateAt = getIntObjVar(self, OBJVAR_NEXT_STATE_TS);
        if (nextStateAt > now)
        {
            return;
        }
        setObjVar(self, OBJVAR_STATE, STATE_SEARCH_BAZAAR);
    }

    private void handleSearchBazaar(obj_id self) throws InterruptedException
    {
        if (!canTradeByCadence(self))
        {
            transitionToResumePatrol(self, "cadence_guard");
            return;
        }

        dictionary opportunity = npc_economy.findBestVendorOpportunity(self, SEARCH_RADIUS);
        if (opportunity == null)
        {
            transitionToResumePatrol(self, "no_opportunity");
            return;
        }

        String action = opportunity.getString("action");
        obj_id vendor = opportunity.containsKey("vendor") ? opportunity.getObjId("vendor") : obj_id.NULL_ID;
        int price = opportunity.containsKey("price") ? opportunity.getInt("price") : 0;
        String reason = opportunity.containsKey("reason") ? opportunity.getString("reason") : "none";
        obj_id listing = opportunity.containsKey("listing") ? opportunity.getObjId("listing") : obj_id.NULL_ID;

        dictionary learnedDecision = applyLearningToTradeDecision(self, action, reason);
        action = learnedDecision.getString("action");
        reason = learnedDecision.getString("reason");

        if (!isIdValid(vendor))
        {
            transitionToResumePatrol(self, "vendor_unavailable");
            return;
        }
        if ("buy".equals(action) && !canSpend(self, price))
        {
            transitionToResumePatrol(self, "budget_guard");
            return;
        }

        setObjVar(self, OBJVAR_TARGET_VENDOR, vendor);
        setObjVar(self, OBJVAR_TARGET_ACTION, action);
        setObjVar(self, OBJVAR_TARGET_PRICE, Math.max(0, price));
        setObjVar(self, OBJVAR_TARGET_REASON, reason == null ? "unspecified" : reason);
        if (isIdValid(listing))
        {
            setObjVar(self, OBJVAR_TARGET_LISTING, listing);
        }
        else if (hasObjVar(self, OBJVAR_TARGET_LISTING))
        {
            removeObjVar(self, OBJVAR_TARGET_LISTING);
        }

        if (!isSafeToInteract(self, vendor))
        {
            pathTo(self, getLocation(vendor));
            return;
        }

        setObjVar(self, OBJVAR_STATE, "buy".equals(action) ? STATE_BUY : STATE_SELL);
    }

    private void handleTradeState(obj_id self, String state) throws InterruptedException
    {
        obj_id vendor = hasObjVar(self, OBJVAR_TARGET_VENDOR) ? getObjIdObjVar(self, OBJVAR_TARGET_VENDOR) : obj_id.NULL_ID;
        if (!isIdValid(vendor) || !exists(vendor))
        {
            transitionToResumePatrol(self, "target_lost");
            return;
        }

        if (!isSafeToInteract(self, vendor))
        {
            pathTo(self, getLocation(vendor));
            transitionToResumePatrol(self, "interaction_range_guard");
            return;
        }

        dictionary opportunity = new dictionary();
        opportunity.put("vendor", vendor);
        opportunity.put("action", STATE_BUY.equals(state) ? "buy" : "sell");
        if (hasObjVar(self, OBJVAR_TARGET_LISTING))
        {
            opportunity.put("listing", getObjIdObjVar(self, OBJVAR_TARGET_LISTING));
        }
        if (hasObjVar(self, OBJVAR_TARGET_PRICE))
        {
            opportunity.put("price", getIntObjVar(self, OBJVAR_TARGET_PRICE));
        }
        if (hasObjVar(self, OBJVAR_TARGET_REASON))
        {
            opportunity.put("reason", getStringObjVar(self, OBJVAR_TARGET_REASON));
        }

        boolean success = npc_economy.executeVendorOpportunity(self, opportunity);
        String reason = hasObjVar(self, OBJVAR_TARGET_REASON) ? getStringObjVar(self, OBJVAR_TARGET_REASON) : "unspecified";
        int price = hasObjVar(self, OBJVAR_TARGET_PRICE) ? getIntObjVar(self, OBJVAR_TARGET_PRICE) : 0;
        if (success)
        {
            registerTradeCadence(self, price);
        }

        String itemName = hasObjVar(self, npc_economy.OBJVAR_LAST_ITEM) ? getStringObjVar(self, npc_economy.OBJVAR_LAST_ITEM) : "unknown";
        int txPrice = hasObjVar(self, npc_economy.OBJVAR_LAST_PRICE) ? getIntObjVar(self, npc_economy.OBJVAR_LAST_PRICE) : price;
        String txReason = hasObjVar(self, npc_economy.OBJVAR_LAST_REASON) ? getStringObjVar(self, npc_economy.OBJVAR_LAST_REASON) : reason;
        String result = hasObjVar(self, npc_economy.OBJVAR_LAST_RESULT) ? getStringObjVar(self, npc_economy.OBJVAR_LAST_RESULT) : (success ? "ok" : "fail");
        LOG("npcii_bazaar_trade", self + ";state=" + state + ";item=" + itemName + ";price=" + txPrice + ";reason=" + txReason + ";result=" + result);

        transitionToResumePatrol(self, success ? "trade_complete" : "trade_failed");
    }

    private void handleResumePatrol(obj_id self) throws InterruptedException
    {
        maybePathNearAnchor(self);
        int now = getGameTime();
        int nextStateAt = getIntObjVar(self, OBJVAR_NEXT_STATE_TS);
        if (now < nextStateAt)
        {
            return;
        }
        setObjVar(self, OBJVAR_STATE, STATE_PATROL);
    }

    private void maybePathNearAnchor(obj_id self) throws InterruptedException
    {
        location anchor = getLocationObjVar(self, OBJVAR_PATROL_ANCHOR);
        if (anchor == null)
        {
            anchor = getLocation(self);
            setObjVar(self, OBJVAR_PATROL_ANCHOR, anchor);
        }
        if (rand(1, 100) <= 40)
        {
            location patrolTarget = (location)anchor.clone();
            patrolTarget.x += rand(-8.0f, 8.0f);
            patrolTarget.z += rand(-8.0f, 8.0f);
            pathTo(self, patrolTarget);
        }
    }

    private boolean canTradeByCadence(obj_id self) throws InterruptedException
    {
        int now = getGameTime();
        if (getIntObjVar(self, OBJVAR_NEXT_TRADE_TS) > now)
        {
            return false;
        }
        int trades = getIntObjVar(self, OBJVAR_DAILY_TRADES);
        if (trades >= DAILY_TRADE_LIMIT)
        {
            return false;
        }
        return true;
    }

    private boolean canSpend(obj_id self, int price) throws InterruptedException
    {
        if (price <= 0)
        {
            return true;
        }
        int spent = getIntObjVar(self, OBJVAR_DAILY_SPEND);
        return (spent + price) <= DAILY_BUDGET_LIMIT;
    }

    private boolean isSafeToInteract(obj_id self, obj_id terminal) throws InterruptedException
    {
        if (!isIdValid(terminal) || !exists(terminal))
        {
            return false;
        }
        location npcLoc = getLocation(self);
        location termLoc = getLocation(terminal);
        if (npcLoc == null || termLoc == null)
        {
            return false;
        }
        if (npcLoc.cell != termLoc.cell)
        {
            return false;
        }
        if (npcLoc.area == null || termLoc.area == null)
        {
            return false;
        }
        if (!npcLoc.area.equals(termLoc.area))
        {
            return false;
        }
        return getDistance(self, terminal) <= INTERACTION_RANGE;
    }

    private void registerTradeCadence(obj_id self, int spend) throws InterruptedException
    {
        int now = getGameTime();
        setObjVar(self, OBJVAR_NEXT_TRADE_TS, now + rand(TRADE_COOLDOWN_MIN_SECONDS, TRADE_COOLDOWN_MAX_SECONDS));
        setObjVar(self, OBJVAR_DAILY_TRADES, getIntObjVar(self, OBJVAR_DAILY_TRADES) + 1);
        if (spend > 0)
        {
            setObjVar(self, OBJVAR_DAILY_SPEND, getIntObjVar(self, OBJVAR_DAILY_SPEND) + spend);
        }
    }

    private dictionary applyLearningToTradeDecision(obj_id self, String baseAction, String baseReason) throws InterruptedException
    {
        dictionary decision = new dictionary();
        String action = baseAction == null ? "buy" : baseAction;
        String reason = baseReason == null ? "unspecified" : baseReason;

        int tradePref = clampLearningValue(getLearningInt(self, OBJVAR_PREFERS_TRADE), -30, 30);
        int trustBias = clampLearningValue(getAverageTrustBias(self), -12, 12);
        int learningSellBias = clampLearningValue((tradePref / 4) + (trustBias / -6), -8, 8);

        if ("buy".equals(action) && learningSellBias >= 6)
        {
            action = "sell";
            reason = reason + "|learning_pref_sell";
        }
        else if ("sell".equals(action) && learningSellBias <= -6)
        {
            action = "buy";
            reason = reason + "|learning_pref_buy";
        }
        else if (learningSellBias > 0)
        {
            reason = reason + "|learning_sell_bias_" + learningSellBias;
        }
        else if (learningSellBias < 0)
        {
            reason = reason + "|learning_buy_bias_" + Math.abs(learningSellBias);
        }

        if (learningSellBias != 0)
        {
            LOG("npcii_learning_bazaar_bias", self + ";bias=" + learningSellBias + ";action=" + action + ";reason=" + reason);
        }

        decision.put("action", action);
        decision.put("reason", reason);
        return decision;
    }

    private int getLearningInt(obj_id self, String path) throws InterruptedException
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
        obj_var_list playerIds = getObjVarList(self, OBJVAR_PLAYER_TRUST_ROOT);
        if (playerIds == null || playerIds.getNumItems() == 0)
        {
            return 0;
        }

        int sum = 0;
        int count = 0;
        int numItems = playerIds.getNumItems();
        for (int i = 0; i < numItems; i++)
        {
            obj_var trustVar = playerIds.getObjVar(i);
            if (trustVar == null)
            {
                continue;
            }
            String playerId = trustVar.getName();
            if (playerId == null || playerId.length() == 0)
            {
                continue;
            }
            String path = OBJVAR_PLAYER_TRUST_ROOT + "." + playerId;
            if (!hasObjVar(self, path))
            {
                continue;
            }
            sum += clampLearningValue(getIntObjVar(self, path), 0, 100);
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
        return (sum / count) - 50;
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

    private void transitionToResumePatrol(obj_id self, String reason) throws InterruptedException
    {
        int now = getGameTime();
        setObjVar(self, OBJVAR_STATE, STATE_RESUME_PATROL);
        setObjVar(self, OBJVAR_NEXT_STATE_TS, now + rand(12, 26));
        LOG("npcii_bazaar_state", self + ";state=" + STATE_RESUME_PATROL + ";reason=" + reason);
    }

    private void resetDailyCountersIfNeeded(obj_id self) throws InterruptedException
    {
        int now = getGameTime();
        int day = now / 86400;
        int stored = getIntObjVar(self, OBJVAR_DAILY_DAY);
        if (stored != day)
        {
            setObjVar(self, OBJVAR_DAILY_DAY, day);
            setObjVar(self, OBJVAR_DAILY_SPEND, 0);
            setObjVar(self, OBJVAR_DAILY_TRADES, 0);
        }
    }
}

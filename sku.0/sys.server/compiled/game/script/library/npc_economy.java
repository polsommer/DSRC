package script.library;

import script.dictionary;
import script.obj_id;

public class npc_economy extends script.base_script
{
    public npc_economy()
    {
    }

    public static final String OBJVAR_ROOT = "systems.npcii.economy";
    public static final String OBJVAR_LAST_ITEM = OBJVAR_ROOT + ".last.item";
    public static final String OBJVAR_LAST_PRICE = OBJVAR_ROOT + ".last.price";
    public static final String OBJVAR_LAST_REASON = OBJVAR_ROOT + ".last.reason";
    public static final String OBJVAR_LAST_RESULT = OBJVAR_ROOT + ".last.result";

    public static void initializeEconomy(obj_id self) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self))
        {
            return;
        }
        if (!hasObjVar(self, OBJVAR_LAST_ITEM))
        {
            setObjVar(self, OBJVAR_LAST_ITEM, "unknown");
        }
        if (!hasObjVar(self, OBJVAR_LAST_PRICE))
        {
            setObjVar(self, OBJVAR_LAST_PRICE, 0);
        }
        if (!hasObjVar(self, OBJVAR_LAST_REASON))
        {
            setObjVar(self, OBJVAR_LAST_REASON, "none");
        }
        if (!hasObjVar(self, OBJVAR_LAST_RESULT))
        {
            setObjVar(self, OBJVAR_LAST_RESULT, "idle");
        }
    }

    public static dictionary findBestVendorOpportunity(obj_id self, float searchRadius) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || searchRadius <= 0.0f)
        {
            return null;
        }
        return null;
    }

    public static boolean executeVendorOpportunity(obj_id self, dictionary opportunity) throws InterruptedException
    {
        initializeEconomy(self);

        if (!isIdValid(self) || !exists(self) || opportunity == null)
        {
            if (isIdValid(self) && exists(self))
            {
                setObjVar(self, OBJVAR_LAST_RESULT, "fail");
                setObjVar(self, OBJVAR_LAST_REASON, "invalid_opportunity");
            }
            return false;
        }

        String reason = opportunity.containsKey("reason") ? opportunity.getString("reason") : "unsupported";
        int price = opportunity.containsKey("price") ? Math.max(0, opportunity.getInt("price")) : 0;

        setObjVar(self, OBJVAR_LAST_ITEM, "unknown");
        setObjVar(self, OBJVAR_LAST_PRICE, price);
        setObjVar(self, OBJVAR_LAST_REASON, reason);
        setObjVar(self, OBJVAR_LAST_RESULT, "fail");

        return false;
    }
}

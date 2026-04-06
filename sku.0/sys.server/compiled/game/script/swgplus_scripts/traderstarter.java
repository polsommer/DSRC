package script.swgplus_scripts;

import script.base_script;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.utils;

public class traderstarter extends base_script
{
    private static final string_id SID_REDEEM = new string_id("sui", "REDEEM_SET_TOKEN");
    private static final String KIT_DISPLAY_NAME = "Trader Utility Starter Kit";
    private static final String SUCCESS_MESSAGE = "You receive a Personal Wind Generator Installation, a Personal Mineral Extractor Deed, and a Personal Chemical Extractor Deed.";

    private static final String[] KIT_TEMPLATES = {
        "object/tangible/deed/generator_deed/power_generator_wind_deed.iff",
        "object/tangible/deed/harvester_deed/ore_harvester_s1_deed.iff",
        "object/tangible/deed/harvester_deed/liquid_harvester_deed.iff"
    };

    public traderstarter()
    {
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setObjVar(self, "noTradeShared", true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, SID_REDEEM);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            grantKit(player, self);
        }
        return SCRIPT_CONTINUE;
    }

    private boolean grantKit(obj_id player, obj_id token) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player) || !isPlayer(player))
        {
            return false;
        }

        obj_id inventory = utils.getInventoryContainer(player);
        if (!isIdValid(inventory))
        {
            sendSystemMessageTestingOnly(player, "Unable to locate your inventory.");
            return false;
        }

        int freeSlots = getVolumeFree(inventory);
        if (freeSlots != -1 && freeSlots < KIT_TEMPLATES.length)
        {
            sendSystemMessageTestingOnly(player, "You need more inventory space to claim this kit.");
            return false;
        }

        obj_id[] createdItems = new obj_id[KIT_TEMPLATES.length];
        for (int i = 0; i < KIT_TEMPLATES.length; i++)
        {
            obj_id created = createObject(KIT_TEMPLATES[i], inventory, "");
            if (!isIdValid(created))
            {
                sendSystemMessageTestingOnly(player, "Failed to create the " + KIT_DISPLAY_NAME + ". Please clear more space and try again.");
                destroyCreated(createdItems);
                return false;
            }
            createdItems[i] = created;
        }

        sendSystemMessageTestingOnly(player, SUCCESS_MESSAGE);
        destroyObject(token);
        return true;
    }

    private void destroyCreated(obj_id[] createdItems) throws InterruptedException
    {
        if (createdItems == null)
        {
            return;
        }
        for (obj_id item : createdItems)
        {
            if (isIdValid(item))
            {
                destroyObject(item);
            }
        }
    }
}

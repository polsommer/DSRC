package script.swgplus_scripts;

import script.base_script;
import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.sui;
import script.library.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class townstarter extends base_script
{
    private static final string_id SID_REDEEM = new string_id("sui", "REDEEM_SET_TOKEN");
    private static final String TITLE = "Town Starter";
    private static final String PROMPT = "Select the civic starter kit you wish to claim.";

    private static final String[] KIT_DISPLAY_NAMES = {
        "Tatooine Civic Kit",
        "Generic Civic Kit",
        "Naboo Civic Kit",
        "Corellian Civic Kit"
    };

    private static final String[][] KIT_TEMPLATES = {
        {
            "object/tangible/deed/city_deed/cityhall_tatooine_deed.iff",
            "object/tangible/deed/city_deed/cloning_tatooine_deed.iff",
            "object/tangible/deed/city_deed/garden_tatooine_lrg_01_deed.iff",
            "object/tangible/deed/city_deed/garden_tatooine_lrg_02_deed.iff"
        },
        {
            // Corellian civic structures serve as the generic visual style in the live game data.
            "object/tangible/deed/city_deed/cityhall_corellia_deed.iff",
            "object/tangible/deed/city_deed/cloning_corellia_deed.iff",
            "object/tangible/deed/city_deed/garden_corellia_med_01_deed.iff",
            "object/tangible/deed/city_deed/garden_corellia_med_02_deed.iff"
        },
        {
            "object/tangible/deed/city_deed/cityhall_naboo_deed.iff",
            "object/tangible/deed/city_deed/cloning_naboo_deed.iff",
            "object/tangible/deed/city_deed/garden_naboo_lrg_01_deed.iff",
            "object/tangible/deed/city_deed/garden_naboo_lrg_02_deed.iff"
        },
        {
            "object/tangible/deed/city_deed/cityhall_corellia_deed.iff",
            "object/tangible/deed/city_deed/cloning_corellia_deed.iff",
            "object/tangible/deed/city_deed/garden_corellia_lrg_01_deed.iff",
            "object/tangible/deed/city_deed/garden_corellia_lrg_02_deed.iff"
        }
    };

    private static final String[] MENU_OPTIONS = buildMenuOptions();

    private static String[] buildMenuOptions()
    {
        return KIT_DISPLAY_NAMES.clone();
    }

    public townstarter()
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
            openSelection(self, player);
        }
        return SCRIPT_CONTINUE;
    }

    private void openSelection(obj_id self, obj_id player) throws InterruptedException
    {
        int pid = sui.listbox(self, player, PROMPT, sui.OK_CANCEL, TITLE, MENU_OPTIONS, "handleKitSelection", true, false);
        if (pid > -1)
        {
            utils.setScriptVar(player, "townstarter.pid", pid);
        }
    }

    public int handleKitSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        obj_id player = sui.getPlayerId(params);
        int button = sui.getIntButtonPressed(params);
        int index = sui.getListboxSelectedRow(params);

        cleanupSui(player);

        if (button == sui.BP_CANCEL || index < 0 || index >= KIT_DISPLAY_NAMES.length)
        {
            return SCRIPT_CONTINUE;
        }

        grantKit(player, KIT_DISPLAY_NAMES[index], KIT_TEMPLATES[index], self);
        return SCRIPT_CONTINUE;
    }

    private static final Method FORCE_CLOSE_METHOD = initForceCloseMethod();

    private static Method initForceCloseMethod()
    {
        try
        {
            return base_script.class.getMethod("forceCloseSUIPage", int.class);
        }
        catch (NoSuchMethodException | SecurityException err)
        {
            return null;
        }
    }

    private void cleanupSui(obj_id player) throws InterruptedException
    {
        if (player == null)
        {
            return;
        }
        if (utils.hasScriptVar(player, "townstarter.pid"))
        {
            int pid = utils.getIntScriptVar(player, "townstarter.pid");
            if (pid > -1)
            {
                closeSuiPage(player, pid);
            }
            utils.removeScriptVar(player, "townstarter.pid");
        }
    }

    private void closeSuiPage(obj_id player, int pid) throws InterruptedException
    {
        if (pid < 0)
        {
            return;
        }

        if (FORCE_CLOSE_METHOD != null)
        {
            try
            {
                FORCE_CLOSE_METHOD.invoke(this, pid);
                return;
            }
            catch (IllegalAccessException | InvocationTargetException err)
            {
                // Fall back to the scripted close behaviour below.
            }
        }

        sui.closeSUI(player, pid);
    }

    private boolean grantKit(obj_id player, String displayName, String[] templates, obj_id token) throws InterruptedException
    {
        if (templates == null || !isIdValid(player) || !exists(player) || !isPlayer(player))
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
        if (freeSlots != -1 && freeSlots < templates.length)
        {
            sendSystemMessageTestingOnly(player, "You need more inventory space to claim this kit.");
            return false;
        }

        obj_id[] createdItems = new obj_id[templates.length];
        for (int i = 0; i < templates.length; i++)
        {
            obj_id created = createObject(templates[i], inventory, "");
            if (!isIdValid(created))
            {
                sendSystemMessageTestingOnly(player, "Failed to create " + displayName + ". Please clear more space and try again.");
                destroyCreated(createdItems);
                return false;
            }
            createdItems[i] = created;
        }

        sendSystemMessageTestingOnly(player, "You receive the " + displayName + ".");
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

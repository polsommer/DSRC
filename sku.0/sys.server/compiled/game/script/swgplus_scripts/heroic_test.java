package script.swgplus_scripts;

import script.base_script;
import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.static_item;
import script.library.utils;
import script.library.sui;

import java.util.ArrayList;
import java.util.List;

import static script.library.holiday.closeOldWindow;

public class heroic_test extends base_script {

    private static final string_id REDEEM = new string_id("sui", "REDEEM_SET_TOKEN");
    private static final BioSet[] BIO_SETS = {
        new BioSet("Heroism set", "set_hero_01_01"),
        new BioSet("Dire Fate set", "set_bh_utility_b_01_01"),
        new BioSet("Enforcer's set", "set_bh_dps_01_01"),
        new BioSet("Flawless set", "set_bh_utility_a_01_01"),
        new BioSet("Frontman set", "set_commando_utility_a_01_01"),
        new BioSet("Grenadier set", "set_commando_dps_01_01"),
        new BioSet("Juggernaut set", "set_commando_utility_b_01_01"),
        new BioSet("Dark Fury set", "set_jedi_utility_a_01_01"),
        new BioSet("Guardian's set", "set_jedi_utility_b_01_01"),
        new BioSet("Lightsaber Duelist's set", "set_jedi_dps_01_01"),
        new BioSet("Blackbar's Doom set", "set_medic_utility_b_01_01"),
        new BioSet("First Responder's set", "set_medic_utility_a_01_01"),
        new BioSet("Striker's set", "set_medic_dps_01_01"),
        new BioSet("Dead Eye set", "set_officer_dps_01_01"),
        new BioSet("General's set", "set_officer_utility_b_01_01"),
        new BioSet("Hellstorm set", "set_officer_utility_a_01_01"),
        new BioSet("Gambler's set", "smuggler_utility_b_01_01"),
        new BioSet("Rogue set", "set_smuggler_utility_a_01_01"),
        new BioSet("Scoundrel's set", "set_smuggler_dps_01_01"),
        new BioSet("Assassin's set", "set_spy_dps_01_01"),
        new BioSet("The Ghost set", "set_spy_utility_a_01_01"),
        new BioSet("The Razor Cat set", "set_spy_utility_b_01_01"),
        new BioSet("Tragedy set", "set_ent_01_01"),
        new BioSet("Tinker's set", "set_trader_01_01")
    };

    private static final String[] MENU_OPTIONS = new String[BIO_SETS.length];

    static {
        for (int i = 0; i < BIO_SETS.length; i++) {
            MENU_OPTIONS[i] = BIO_SETS[i].displayName;
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException {
        menuInfo.addRootMenu(menu_info_types.ITEM_USE, REDEEM);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException {
        if (item == menu_info_types.ITEM_USE) {
            startSetSelection(player);
        }
        return SCRIPT_CONTINUE;
    }

    private void startSetSelection(obj_id player) throws InterruptedException {
        obj_id self = getSelf();
        String prompt = "Choose a Bio-Linked, No-Trade Set:";
        String title = "Set Redemption";
        int pid = sui.listbox(self, player, prompt, sui.OK_CANCEL, title, MENU_OPTIONS, "handleOptionSelect", true, false);
        setWindowPid(player, pid);
    }

    private void setWindowPid(obj_id player, int pid) throws InterruptedException {
        if (pid > -1) {
            utils.setScriptVar(player, "character_builder.pid", pid);
        }
    }

    public int OnInitialize(obj_id self) throws InterruptedException {
        setObjVar(self, "noTradeShared", true);
        return SCRIPT_CONTINUE;
    }

    public int handleOptionSelect(obj_id self, dictionary params) throws InterruptedException {
        if (params == null || params.isEmpty()) {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int button = sui.getIntButtonPressed(params);
        int index = sui.getListboxSelectedRow(params);
        if (button == sui.BP_CANCEL || index < 0 || index >= BIO_SETS.length) {
            cleanScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        grantBioSet(player, index, self);
        closeOldWindow(player);
        return SCRIPT_CONTINUE;
    }

    private void cleanScriptVars(obj_id player) throws InterruptedException {
        obj_id self = getSelf();
        utils.removeScriptVarTree(player, "character_builder");
        utils.removeScriptVarTree(self, "character_builder");
    }

    private void incrementCounter(obj_id player, String objvar) throws InterruptedException {
        int value = 0;
        if (hasObjVar(player, objvar)) {
            value = getIntObjVar(player, objvar);
        }
        setObjVar(player, objvar, value + 1);
    }

    private obj_id[] grantBioSet(obj_id player, int selection, obj_id destroyMe) throws InterruptedException {
        if (selection < 0 || selection >= BIO_SETS.length) {
            sendSystemMessage(player, "Unable to determine the requested equipment set.", null);
            return null;
        }

        obj_id inventory = utils.getInventoryContainer(player);
        if (!isIdValid(inventory)) {
            sendSystemMessage(player, "Unable to locate your inventory. Please ensure you have available space.", null);
            return null;
        }

        BioSet set = BIO_SETS[selection];
        List<obj_id> items = new ArrayList<>();
        for (String slot : BioSet.ITEM_SLOTS) {
            obj_id item = static_item.createNewItemFunction("item_" + slot + set.templateRoot, inventory);
            if (isIdValid(item)) {
                static_item.attachScript(item, "item.armor.biolink_item_non_faction");
                setObjVar(item, "noTrade", true);
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            sendSystemMessage(player, "The requested set could not be created. Please contact support.", null);
            return null;
        }

        obj_id[] created = items.toArray(new obj_id[0]);
        showLootBox(player, created);
        static_item.destroyObject(destroyMe);
        incrementCounter(player, "swgplus.heroic_token.redeemed");
        return created;
    }

    private static final class BioSet {
        private static final String[] ITEM_SLOTS = {
            "ring_",
            "band_",
            "necklace_",
            "bracelet_r_",
            "bracelet_l_"
        };

        private final String displayName;
        private final String templateRoot;

        private BioSet(String displayName, String templateRoot) {
            this.displayName = displayName;
            this.templateRoot = templateRoot;
        }
    }
}

package script.swgplus_scripts;

import script.base_script;
import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.skill;
import script.library.space_skill;
import script.library.static_item;
import script.library.sui;
import script.library.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static script.library.holiday.closeOldWindow;

public class pilot_test extends base_script {

    private static final string_id REDEEM = new string_id("sui", "redeem_pilot_token");
    private static final PilotPath[] PILOT_PATHS = {
        new PilotPath("Master Freelance Pilot", "neutral", Arrays.asList(
            "novice",
            "starships_01",
            "starships_02",
            "starships_03",
            "starships_04",
            "weapons_01",
            "weapons_02",
            "weapons_03",
            "weapons_04",
            "procedures_01",
            "procedures_02",
            "procedures_03",
            "procedures_04",
            "droid_01",
            "droid_02",
            "droid_03",
            "droid_04",
            "master"
        )),
        new PilotPath("Master Imperial Pilot", "imperial_navy", Arrays.asList(
            "novice",
            "starships_01",
            "starships_02",
            "starships_03",
            "starships_04",
            "weapons_01",
            "weapons_02",
            "weapons_03",
            "weapons_04",
            "procedures_01",
            "procedures_02",
            "procedures_03",
            "procedures_04",
            "droid_01",
            "droid_02",
            "droid_03",
            "droid_04",
            "master"
        )),
        new PilotPath("Master Rebel Pilot", "rebel_navy", Arrays.asList(
            "novice",
            "starships_01",
            "starships_02",
            "starships_03",
            "starships_04",
            "weapons_01",
            "weapons_02",
            "weapons_03",
            "weapons_04",
            "procedures_01",
            "procedures_02",
            "procedures_03",
            "procedures_04",
            "droid_01",
            "droid_02",
            "droid_03",
            "droid_04",
            "master"
        ))
    };

    private static final String[] MENU_OPTIONS = new String[PILOT_PATHS.length];

    static {
        for (int i = 0; i < PILOT_PATHS.length; i++) {
            MENU_OPTIONS[i] = PILOT_PATHS[i].displayName;
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
        String prompt = "Choose a Roadmap:";
        String title = "Master Pilot Skill Token";
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
        int btn = sui.getIntButtonPressed(params);
        int idx = sui.getListboxSelectedRow(params);
        if (btn == sui.BP_CANCEL || idx < 0 || idx >= PILOT_PATHS.length) {
            cleanScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        grantSkillPath(player, idx, self);
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

    private boolean revokeSpaceSkills(obj_id player) throws InterruptedException {
        if (hasSkill(player, "pilot_rebel_navy_novice") || hasSkill(player, "pilot_imperial_navy_novice") || hasSkill(player, "pilot_neutral_novice")) {
            String pilotFaction;
            if (hasSkill(player, "pilot_rebel_navy_novice")) {
                pilotFaction = "rebel_navy";
            } else if (hasSkill(player, "pilot_imperial_navy_novice")) {
                pilotFaction = "imperial_navy";
            } else {
                pilotFaction = "neutral";
            }
            for (String skillSuffix : space_skill.SKILL_NAMES) {
                skill.revokeSkill(player, "pilot_" + pilotFaction + skillSuffix);
            }
            sendSystemMessage(player, "Existing pilot training revoked to apply the new roadmap.", null);
            return true;
        }
        return !space_skill.hasSpaceSkills(player);
    }

    private void grantSkillPath(obj_id player, int index, obj_id destroyMe) throws InterruptedException {
        PilotPath path = PILOT_PATHS[index];
        if (!revokeSpaceSkills(player)) {
            sendSystemMessage(player, "Unable to revoke your previous pilot skills. Please contact support.", null);
            return;
        }
        for (String suffix : path.skillSuffixes) {
            skill.grantSkill(player, "pilot_" + path.faction + "_" + suffix);
        }
        sendSystemMessage(player, path.displayName + " roadmap applied successfully.", null);
        static_item.destroyObject(destroyMe);
        incrementCounter(player, "swgplus.pilot_token.redeemed");
    }

    private static final class PilotPath {
        private final String displayName;
        private final String faction;
        private final List<String> skillSuffixes;

        public PilotPath() {
            this("", "", Collections.emptyList());
        }

        private PilotPath(String displayName, String faction, List<String> skillSuffixes) {
            this.displayName = displayName;
            this.faction = faction;
            this.skillSuffixes = skillSuffixes;
        }
    }
}

package script.swgplus_scripts;

import script.base_script;
import script.dictionary;
import script.obj_id;
import script.library.utils;

public class macro_free_zone extends base_script {

    private static final String TRIGGER_PRIMARY = "60meters";
    private static final String TRIGGER_WARNING = "preentering";
    private static final float PRIMARY_RADIUS = 60.0f;
    private static final float WARNING_RADIUS = 80.0f;
    private static final float CHECK_INTERVAL = 60.0f;
    private static final float ESCALATION_INTERVAL = 300.0f;
    private static final int MAX_WARNINGS = 3;
    private static final String SCRIPT_VAR_ENTERED = "macro_zone.entered";
    private static final String SCRIPT_VAR_WARNINGS = "macro_zone.warnings";
    private static final String SCRIPT_VAR_ESCALATED = "macro_zone.escalated";

    private static final String MESSAGE_ENTER = "You have entered a macro-free zone. Automated monitoring is active.";
    private static final String MESSAGE_WARNING = "Macro usage is prohibited in this area. Continued violations will be escalated.";
    private static final String MESSAGE_EXIT = "You have left the macro-free zone.";

    public int OnInitialize(obj_id self) throws InterruptedException {
        ensureTriggerVolume(self, TRIGGER_PRIMARY, PRIMARY_RADIUS);
        ensureTriggerVolume(self, TRIGGER_WARNING, WARNING_RADIUS);
        scheduleMonitor(self, CHECK_INTERVAL);
        return SCRIPT_CONTINUE;
    }

    private void ensureTriggerVolume(obj_id self, String name, float radius) throws InterruptedException {
        if (!hasTriggerVolume(self, name)) {
            createTriggerVolume(name, radius, true);
        }
    }

    public int OnTriggerVolumeEntered(obj_id self, String volumeName, obj_id whoTriggeredMe) throws InterruptedException {
        if (!isPlayer(whoTriggeredMe)) {
            return SCRIPT_CONTINUE;
        }

        if (TRIGGER_PRIMARY.equals(volumeName)) {
            if (isGod(whoTriggeredMe) || hasObjVar(whoTriggeredMe, "dev")) {
                return SCRIPT_CONTINUE;
            }
            sendSystemMessage(whoTriggeredMe, MESSAGE_ENTER, null);
            utils.setScriptVar(whoTriggeredMe, SCRIPT_VAR_ENTERED, getGameTime());
            utils.removeScriptVar(whoTriggeredMe, SCRIPT_VAR_ESCALATED);
            sendConsoleCommand("/dumpp", whoTriggeredMe);
        } else if (TRIGGER_WARNING.equals(volumeName)) {
            sendSystemMessage(whoTriggeredMe, "You are about to enter a macro-free zone. Review the rules before proceeding.", null);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeExited(obj_id self, String volumeName, obj_id whoTriggeredMe) throws InterruptedException {
        if (!isPlayer(whoTriggeredMe)) {
            return SCRIPT_CONTINUE;
        }
        if (TRIGGER_PRIMARY.equals(volumeName)) {
            sendSystemMessage(whoTriggeredMe, MESSAGE_EXIT, null);
            utils.removeScriptVar(whoTriggeredMe, SCRIPT_VAR_ENTERED);
            utils.removeScriptVar(whoTriggeredMe, SCRIPT_VAR_WARNINGS);
            utils.removeScriptVar(whoTriggeredMe, SCRIPT_VAR_ESCALATED);
        }
        return SCRIPT_CONTINUE;
    }

    public int checkMacroActivity(obj_id self, dictionary params) throws InterruptedException {
        obj_id[] contents = getTriggerVolumeContents(self, TRIGGER_PRIMARY);
        if (contents != null) {
            for (obj_id player : contents) {
                if (!isPlayer(player) || utils.hasScriptVar(player, SCRIPT_VAR_ESCALATED)) {
                    continue;
                }
                if (isGod(player) || hasObjVar(player, "dev")) {
                    continue;
                }
                if (utils.hasScriptVar(player, SCRIPT_VAR_ENTERED)) {
                    sendConsoleCommand("/dumpp", player);
                    int warnings = utils.getIntScriptVar(player, SCRIPT_VAR_WARNINGS);
                    warnings++;
                    utils.setScriptVar(player, SCRIPT_VAR_WARNINGS, warnings);
                    if (warnings >= MAX_WARNINGS) {
                        escalate(player);
                    } else {
                        sendSystemMessage(player, MESSAGE_WARNING, null);
                    }
                }
            }
        }

        scheduleMonitor(self, CHECK_INTERVAL);
        return SCRIPT_CONTINUE;
    }

    private void scheduleMonitor(obj_id self, float delay) throws InterruptedException {
        messageTo(self, "checkMacroActivity", null, delay, false);
    }

    private void escalate(obj_id player) throws InterruptedException {
        utils.setScriptVar(player, SCRIPT_VAR_ESCALATED, getGameTime());
        CustomerServiceLog("macro_free_zone", "Escalating macro warning for player " + player);
        sendSystemMessage(player, "Macro monitoring flagged repeated activity. Staff has been notified.", null);
        messageTo(player, "handleMacroZoneEscalation", null, 0.0f, false);
    }

    public int resetMacroEject(obj_id self, dictionary params) throws InterruptedException {
        obj_id[] contents = getTriggerVolumeContents(self, TRIGGER_PRIMARY);
        if (contents != null) {
            for (obj_id player : contents) {
                if (isPlayer(player)) {
                    utils.removeScriptVar(player, SCRIPT_VAR_ENTERED);
                    utils.removeScriptVar(player, SCRIPT_VAR_WARNINGS);
                    utils.removeScriptVar(player, SCRIPT_VAR_ESCALATED);
                }
            }
        }
        scheduleMonitor(self, ESCALATION_INTERVAL);
        return SCRIPT_CONTINUE;
    }
}


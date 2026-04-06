package script.swgplus_scripts;

import script.base_script;
import script.dictionary;
import script.location;
import script.obj_id;
import script.library.utils;
import script.library.buff;

public class player_monitor extends base_script {

    private static final float MAX_SPEED_PERCENT = 3.5f;
    private static final float CHECK_INTERVAL = 5.0f;
    private static final int MAX_VIOLATIONS = 3;
    private static final String OBJVAR_LAST_LOCATION = "player.monitor.location";
    private static final String OBJVAR_VIOLATIONS = "player.monitor.violations";
    private static final String OBJVAR_MONITOR_ACTIVE = "player.monitor.active";

    private boolean debug = false;

    public int OnAttach(obj_id self) throws InterruptedException {
        logPlayerLocation(self);
        utils.setScriptVar(self, OBJVAR_MONITOR_ACTIVE, true);
        scheduleSpeedCheck(self, CHECK_INTERVAL);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException {
        if (debug) {
            debugSpeakMsg(self, "Speed percent is: " + getMovementPercent(self));
            debugSpeakMsg(self, "Speed is: " + getMovementSpeed(self));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnLogout(obj_id self) throws InterruptedException {
        clearPlayerData(self);
        return SCRIPT_CONTINUE;
    }

    public int OnMoveMoving(obj_id self) throws InterruptedException {
        if (debug) {
            debugSpeakMsg(self, "Current speed percent is: " + getMovementPercent(self));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnLocomotionChanged(obj_id self, int newLocomotion, int oldLocomotion) throws InterruptedException {
        checkSpeed(self);
        return SCRIPT_CONTINUE;
    }

    public int OnSpeaking(obj_id self, String text) throws InterruptedException {
        if (!isGod(self)) {
            return SCRIPT_CONTINUE;
        }
        if ("st".equals(text)) {
            debugSpeakMsg(self, "Speed percent is: " + getMovementPercent(self));
            debugSpeakMsg(self, "Speed is: " + getMovementSpeed(self));
        } else if ("st_debugtrue".equals(text)) {
            debug = true;
            debugSpeakMsg(self, "Debug mode enabled.");
        } else if ("st_debugfalse".equals(text)) {
            debug = false;
            debugSpeakMsg(self, "Debug mode disabled.");
        } else if ("st_dst".equals(text)) {
            float distance = calculateDistance(self);
            debugSpeakMsg(self, "Distance between last recorded point and current position is: " + distance);
            logPlayerLocation(self);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSpeedMonitor(obj_id self, dictionary params) throws InterruptedException {
        if (utils.hasScriptVar(self, OBJVAR_MONITOR_ACTIVE)) {
            checkSpeed(self);
            scheduleSpeedCheck(self, CHECK_INTERVAL);
        }
        return SCRIPT_CONTINUE;
    }

    private void scheduleSpeedCheck(obj_id self, float delay) throws InterruptedException {
        messageTo(self, "handleSpeedMonitor", null, delay, false);
    }

    private void checkSpeed(obj_id self) throws InterruptedException {
        float movementPercent = getMovementPercent(self);
        if (movementPercent <= MAX_SPEED_PERCENT || debug) {
            return;
        }
        int violations = utils.getIntScriptVar(self, OBJVAR_VIOLATIONS) + 1;
        utils.setScriptVar(self, OBJVAR_VIOLATIONS, violations);
        debugSpeakMsg(self, "WARNING: Movement percent " + movementPercent + " exceeds safe limit.");
        if (violations >= MAX_VIOLATIONS) {
            notifyStaff(self, movementPercent, violations);
            applyEmergencySlow(self);
        }
    }

    private void notifyStaff(obj_id self, float speed, int violations) throws InterruptedException {
        CustomerServiceLog("player_monitor", "Speed violation detected for player " + self + ", speed=" + speed + ", warnings=" + violations);
    }

    private void applyEmergencySlow(obj_id self) throws InterruptedException {
        buff.applyBuff(self, "item_snare_immediate", 10.0f);
        utils.removeScriptVar(self, OBJVAR_VIOLATIONS);
    }

    private float calculateDistance(obj_id self) throws InterruptedException {
        location lastLocation = utils.getLocationScriptVar(self, OBJVAR_LAST_LOCATION);
        if (lastLocation == null) {
            return 0.0f;
        }
        return getDistance(self, lastLocation);
    }

    private void logPlayerLocation(obj_id self) throws InterruptedException {
        location currentLocation = getLocation(self);
        utils.setScriptVar(self, OBJVAR_LAST_LOCATION, currentLocation);
    }

    private void clearPlayerData(obj_id self) throws InterruptedException {
        utils.removeScriptVar(self, OBJVAR_LAST_LOCATION);
        utils.removeScriptVar(self, OBJVAR_VIOLATIONS);
        utils.removeScriptVar(self, OBJVAR_MONITOR_ACTIVE);
    }
}


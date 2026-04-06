package script.swgplus_scripts;

import script.base_script;
import script.location;
import script.obj_id;
import script.library.group;

public class broadcasting_script extends base_script {

    private static final String COMMAND = "broadcast";
    private static final String OBJVAR_LAST_BROADCAST = "swgplus.broadcast.last";
    private static final float BROADCAST_COOLDOWN_SECONDS = 30.0f;
    private static final float AREA_BROADCAST_RANGE = 128.0f;
    private static final float PLANET_RADIUS = 8192.0f;
    private static final String USAGE_TEXT = "Usage: broadcast [galaxy|planet|area|group] <message>";

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException {
        if (!isIdValid(speaker) || !isPlayer(speaker)) {
            return SCRIPT_CONTINUE;
        }

        String trimmed = text == null ? "" : text.trim();
        String normalized = normalizeCommand(trimmed);
        if (!isBroadcastCommand(normalized)) {
            return SCRIPT_CONTINUE;
        }

        if (!canUseBroadcast(speaker)) {
            sendPlainMessage(speaker, "You do not have permission to use the broadcast channel right now.");
            return SCRIPT_CONTINUE;
        }

        String payload = normalized.length() > COMMAND.length() ? normalized.substring(COMMAND.length()).trim() : "";
        BroadcastRequest request = BroadcastRequest.parse(payload);
        if (request.message.isEmpty()) {
            sendPlainMessage(speaker, USAGE_TEXT);
            return SCRIPT_CONTINUE;
        }

        if (!passesCooldown(speaker)) {
            float remaining = getCooldownRemaining(speaker);
            sendPlainMessage(speaker, "Broadcast cooldown active. Please wait " + Math.round(remaining) + " more second(s).");
            return SCRIPT_CONTINUE;
        }

        executeBroadcast(speaker, request);
        setObjVar(speaker, OBJVAR_LAST_BROADCAST, getGameTime());
        logBroadcast(speaker, request);
        return SCRIPT_CONTINUE;
    }

    private String normalizeCommand(String text) {
        if (text.startsWith("/")) {
            return text.substring(1);
        }
        return text;
    }

    private boolean isBroadcastCommand(String text) {
        String lower = text.toLowerCase();
        return lower.startsWith(COMMAND + " ") || lower.equals(COMMAND);
    }

    private boolean canUseBroadcast(obj_id speaker) throws InterruptedException {
        if (isGod(speaker) || hasObjVar(speaker, "swgplus.eventTeam")) {
            return true;
        }
        return hasObjVar(speaker, "swgplus.broadcast.optIn");
    }

    private boolean passesCooldown(obj_id speaker) throws InterruptedException {
        if (!hasObjVar(speaker, OBJVAR_LAST_BROADCAST)) {
            return true;
        }
        float elapsed = getGameTime() - getFloatObjVar(speaker, OBJVAR_LAST_BROADCAST);
        return elapsed >= BROADCAST_COOLDOWN_SECONDS;
    }

    private float getCooldownRemaining(obj_id speaker) throws InterruptedException {
        float last = getFloatObjVar(speaker, OBJVAR_LAST_BROADCAST);
        float elapsed = getGameTime() - last;
        float remaining = BROADCAST_COOLDOWN_SECONDS - elapsed;
        return remaining < 0.0f ? 0.0f : remaining;
    }

    private void executeBroadcast(obj_id speaker, BroadcastRequest request) throws InterruptedException {
        switch (request.scope) {
            case GALAXY:
                sendSystemMessageGalaxyTestingOnly("[Broadcast] " + request.message);
                break;
            case PLANET:
                broadcastPlanetWide(speaker, request.message);
                break;
            case AREA:
                broadcastArea(speaker, request.message, AREA_BROADCAST_RANGE);
                break;
            case GROUP:
                broadcastGroup(speaker, request.message);
                break;
            default:
                sendPlainMessage(speaker, "Unknown broadcast scope. Use galaxy, planet, area, or group.");
        }
    }

    private void broadcastPlanetWide(obj_id speaker, String message) throws InterruptedException {
        location speakerLocation = getLocation(speaker);
        if (speakerLocation == null) {
            sendPlainMessage(speaker, "Unable to determine your current location.");
            return;
        }
        obj_id[] players = getAllPlayers(speakerLocation, PLANET_RADIUS);
        if (players == null || players.length == 0) {
            sendPlainMessage(speaker, "No players detected on this planet to receive the broadcast.");
            return;
        }
        for (obj_id recipient : players) {
            if (isIdValid(recipient) && isPlayer(recipient)) {
                location location = getLocation(recipient);
                if (isSameScene(speakerLocation, location)) {
                    sendPlainMessage(recipient, "[Planet Broadcast] " + message);
                }
            }
        }
    }

    private void broadcastArea(obj_id speaker, String message, float range) throws InterruptedException {
        obj_id[] players = getPlayerCreaturesInRange(speaker, range);
        if (players == null || players.length == 0) {
            sendPlainMessage(speaker, "No nearby players to receive your broadcast.");
            return;
        }
        for (obj_id recipient : players) {
            if (isIdValid(recipient) && isPlayer(recipient)) {
                sendPlainMessage(recipient, "[Area Broadcast] " + message);
            }
        }
    }

    private void broadcastGroup(obj_id speaker, String message) throws InterruptedException {
        if (!group.isGrouped(speaker)) {
            sendPlainMessage(speaker, "You are not in a group.");
            return;
        }
        obj_id groupObject = getGroupObject(speaker);
        if (!isIdValid(groupObject)) {
            sendPlainMessage(speaker, "Unable to locate your group information.");
            return;
        }
        obj_id[] members = getGroupMemberIds(groupObject);
        if (members == null || members.length == 0) {
            sendPlainMessage(speaker, "Your group has no members available to receive the broadcast.");
            return;
        }
        for (obj_id member : members) {
            if (isIdValid(member) && isPlayer(member)) {
                sendPlainMessage(member, "[Group Broadcast] " + message);
            }
        }
    }

    private void sendPlainMessage(obj_id target, String message) throws InterruptedException {
        sendSystemMessage(target, message, null);
    }

    private boolean isSameScene(location first, location second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.area == null) {
            return second.area == null;
        }
        return first.area.equals(second.area);
    }

    private void logBroadcast(obj_id speaker, BroadcastRequest request) throws InterruptedException {
        CustomerServiceLog("swgplus_broadcast", "Speaker: " + speaker + ", scope=" + request.scope + ", message=" + request.message);
    }

    private static final class BroadcastRequest {
        private final Scope scope;
        private final String message;

        private BroadcastRequest(Scope scope, String message) {
            this.scope = scope;
            this.message = message;
        }

        private static BroadcastRequest parse(String payload) {
            if (payload == null || payload.isEmpty()) {
                return new BroadcastRequest(Scope.GALAXY, "");
            }
            String trimmed = payload.trim();
            int firstSpace = trimmed.indexOf(' ');
            if (firstSpace == -1) {
                return new BroadcastRequest(resolveScope(trimmed), "");
            }
            String potentialScope = trimmed.substring(0, firstSpace);
            Scope scope = resolveScope(potentialScope);
            String message = trimmed.substring(firstSpace + 1).trim();
            if (scope == Scope.UNKNOWN) {
                scope = Scope.GALAXY;
                message = trimmed;
            }
            return new BroadcastRequest(scope, message);
        }

        private static Scope resolveScope(String value) {
            if (value == null) {
                return Scope.UNKNOWN;
            }
            String lower = value.toLowerCase();
            if (lower.equals("galaxy") || lower.equals("all")) {
                return Scope.GALAXY;
            }
            if (lower.equals("planet")) {
                return Scope.PLANET;
            }
            if (lower.equals("area") || lower.equals("local")) {
                return Scope.AREA;
            }
            if (lower.equals("group") || lower.equals("team")) {
                return Scope.GROUP;
            }
            return Scope.UNKNOWN;
        }
    }

    private enum Scope {
        GALAXY,
        PLANET,
        AREA,
        GROUP,
        UNKNOWN
    }
}

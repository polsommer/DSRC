package script.swgplus_scripts;

import script.base_script;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.ai_lib;
import script.library.static_item;
import script.library.utils;

public class addslot extends base_script {

    private static final string_id TOKEN_MENU = new string_id("sui", "redeem_slot_token");
    private static final float COOLDOWN_SECONDS = 5.0f;
    private static final String OBJVAR_LAST_REDEEM = "swgplus.slot_token.lastRedeem";
    private static final String OBJVAR_TOTAL_REDEEMS = "swgplus.slot_token.total";

    public int OnInitialize(obj_id self) throws InterruptedException {
        setObjVar(self, "noTradeShared", true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException {
        int menuId = menuInfo.addRootMenu(menu_info_types.ITEM_USE, TOKEN_MENU);
        menu_info_data data = menuInfo.getMenuItemById(menuId);
        if (data != null) {
            data.setServerNotify(true);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException {
        if (item != menu_info_types.ITEM_USE) {
            return SCRIPT_CONTINUE;
        }

        if (!isIdValid(player) || !isPlayer(player)) {
            return SCRIPT_CONTINUE;
        }

        if (isIncapacitated(player) || ai_lib.isInCombat(player)) {
            sendSystemMessage(player, "You must be conscious and out of combat to redeem the token.", null);
            return SCRIPT_CONTINUE;
        }

        if (!canRedeem(player)) {
            float timeRemaining = getTimeRemaining(player);
            sendSystemMessage(player, "The token is stabilising. Please wait " + Math.round(timeRemaining) + " more second(s).", null);
            return SCRIPT_CONTINUE;
        }

        addJediSlot(player);
        incrementCounter(player, OBJVAR_TOTAL_REDEEMS);
        setObjVar(player, OBJVAR_LAST_REDEEM, getGameTime());

        playMusic(player, "sound/music_amb_underwater_b.snd");
        sendSystemMessage(player, "An additional character slot has been unlocked for your account!", null);
        static_item.destroyObject(self);
        return SCRIPT_CONTINUE;
    }

    private void incrementCounter(obj_id player, String objvar) throws InterruptedException {
        int value = 0;
        if (hasObjVar(player, objvar)) {
            value = getIntObjVar(player, objvar);
        }
        setObjVar(player, objvar, value + 1);
    }

    private boolean canRedeem(obj_id player) throws InterruptedException {
        if (!hasObjVar(player, OBJVAR_LAST_REDEEM)) {
            return true;
        }
        float lastRedeem = getFloatObjVar(player, OBJVAR_LAST_REDEEM);
        float elapsed = getGameTime() - lastRedeem;
        return elapsed >= COOLDOWN_SECONDS;
    }

    private float getTimeRemaining(obj_id player) throws InterruptedException {
        float lastRedeem = getFloatObjVar(player, OBJVAR_LAST_REDEEM);
        float elapsed = getGameTime() - lastRedeem;
        float remaining = COOLDOWN_SECONDS - elapsed;
        return remaining < 0.0f ? 0.0f : remaining;
    }
}

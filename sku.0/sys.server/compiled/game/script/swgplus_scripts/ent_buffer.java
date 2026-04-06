package script.swgplus_scripts;

import script.base_script;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.buff;

public class ent_buffer extends base_script {

    private static final string_id MENU_LABEL = new string_id("sui", "claim_ent_buff");
    private static final float COOLDOWN_SECONDS = 300.0f;
    private static final String OBJVAR_LAST_USE = "swgplus.ent_buffer.lastUse";
    private static final String OBJVAR_TOTAL_USES = "swgplus.ent_buffer.uses";
    private static final String[] BUFFS = {
        "general_inspiration",
        "artisan_inspiration",
        "tailor_inspiration",
        "droidengineer_inspiration",
        "weaponsmith_inspiration",
        "shipwright_inspiration",
        "armorsmith_inspiration"
    };

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException {
        int menuId = menuInfo.addRootMenu(menu_info_types.ITEM_USE, MENU_LABEL);
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

        if (!canApplyBuffs(player)) {
            float wait = getCooldownRemaining(player);
            sendSystemMessage(player, "You must wait " + Math.round(wait) + " more second(s) before reclaiming entertainer buffs.", null);
            return SCRIPT_CONTINUE;
        }

        applyEntertainerBuffs(player);
        incrementCounter(player, OBJVAR_TOTAL_USES);
        setObjVar(player, OBJVAR_LAST_USE, getGameTime());
        sendSystemMessage(player, "A wave of inspiration washes over you.", null);
        return SCRIPT_CONTINUE;
    }

    private void incrementCounter(obj_id player, String objvar) throws InterruptedException {
        int value = 0;
        if (hasObjVar(player, objvar)) {
            value = getIntObjVar(player, objvar);
        }
        setObjVar(player, objvar, value + 1);
    }

    private void applyEntertainerBuffs(obj_id player) throws InterruptedException {
        for (String buffName : BUFFS) {
            if (!buff.hasBuff(player, buffName)) {
                buff.applyBuff(player, buffName, 7200);
            }
        }
    }

    private boolean canApplyBuffs(obj_id player) throws InterruptedException {
        if (!hasObjVar(player, OBJVAR_LAST_USE)) {
            return true;
        }
        float elapsed = getGameTime() - getFloatObjVar(player, OBJVAR_LAST_USE);
        return elapsed >= COOLDOWN_SECONDS;
    }

    private float getCooldownRemaining(obj_id player) throws InterruptedException {
        float elapsed = getGameTime() - getFloatObjVar(player, OBJVAR_LAST_USE);
        float remaining = COOLDOWN_SECONDS - elapsed;
        return remaining < 0.0f ? 0.0f : remaining;
    }
}

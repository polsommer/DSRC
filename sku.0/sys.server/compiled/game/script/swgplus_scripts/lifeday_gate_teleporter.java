package script.swgplus_scripts;

import script.*;
import script.library.ai_lib;
import script.library.utils;

public class lifeday_gate_teleporter extends script.base_script
{
    public lifeday_gate_teleporter()
    {
    }
    private static final String DEST_X = "lifeday.dest.x";
    private static final String DEST_Y = "lifeday.dest.y";
    private static final String DEST_Z = "lifeday.dest.z";
    private static final String NAME = "lifeday.teleporter_name";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        copyTeleportObjVarsFromParent(self);
        setInvulnerable(self, true);
        setCondition(self, CONDITION_CONVERSABLE);
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
        applyCustomName(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        copyTeleportObjVarsFromParent(self);
        setInvulnerable(self, true);
        setCondition(self, CONDITION_CONVERSABLE);
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
        applyCustomName(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, null);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, DEST_X) || !hasObjVar(self, DEST_Y) || !hasObjVar(self, DEST_Z))
        {
            copyTeleportObjVarsFromParent(self);
        }
        if (!hasObjVar(self, DEST_X) || !hasObjVar(self, DEST_Y) || !hasObjVar(self, DEST_Z))
        {
            location playerLoc = getLocation(player);
            String scene = getCurrentSceneName();
            location entryPoint = new location(-754f, 18f, 257f, scene);
            location exitPoint = new location(-747f, 19f, 272f, scene);
            float distanceToEntry = getDistance(playerLoc, entryPoint);
            float distanceToExit = getDistance(playerLoc, exitPoint);
            location destination = (distanceToEntry <= distanceToExit) ? exitPoint : entryPoint;
            utils.dismountRiderJetpackCheck(player);
            warpPlayer(player, scene, destination.x, destination.y, destination.z, null, destination.x, destination.y, destination.z);
            sendSystemMessageTestingOnly(player, "The guide ushers you to your destination.");
            return SCRIPT_CONTINUE;
        }
        float destX = getFloatObjVar(self, DEST_X);
        float destY = getFloatObjVar(self, DEST_Y);
        float destZ = getFloatObjVar(self, DEST_Z);
        String scene = getCurrentSceneName();
        utils.dismountRiderJetpackCheck(player);
        warpPlayer(player, scene, destX, destY, destZ, null, destX, destY, destZ);
        sendSystemMessageTestingOnly(player, "The guide ushers you to your destination.");
        return SCRIPT_CONTINUE;
    }

    private void copyTeleportObjVarsFromParent(obj_id self) throws InterruptedException
    {
        obj_id parent = getObjIdObjVar(self, "objParent");
        if (!isIdValid(parent))
        {
            return;
        }
        if (!hasObjVar(self, DEST_X) && hasObjVar(parent, DEST_X))
        {
            setObjVar(self, DEST_X, getFloatObjVar(parent, DEST_X));
        }
        if (!hasObjVar(self, DEST_Y) && hasObjVar(parent, DEST_Y))
        {
            setObjVar(self, DEST_Y, getFloatObjVar(parent, DEST_Y));
        }
        if (!hasObjVar(self, DEST_Z) && hasObjVar(parent, DEST_Z))
        {
            setObjVar(self, DEST_Z, getFloatObjVar(parent, DEST_Z));
        }
        if (!hasObjVar(self, NAME) && hasObjVar(parent, NAME))
        {
            setObjVar(self, NAME, getStringObjVar(parent, NAME));
        }
    }

    private void applyCustomName(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, NAME))
        {
            String desiredName = getStringObjVar(self, NAME);
            if (desiredName != null && desiredName.length() > 0)
            {
                setName(self, desiredName);
            }
        }
    }
}

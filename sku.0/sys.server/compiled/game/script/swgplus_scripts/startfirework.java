package script.swgplus_scripts;

import script.dictionary;
import script.library.create;
import script.library.firework;
import script.library.holiday;
import script.library.utils;
import script.obj_id;
import script.string_id;
import script.location;

public class startfirework extends script.base_script
{
    public startfirework()
    {
    }
    
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        String birthdaySetting = getConfigSetting("EventTeam", "birthdayEvent");
        
        if (birthdaySetting == null || birthdaySetting.equals("") || !birthdaySetting.equals("true"))
        {
            return SCRIPT_CONTINUE;
        }
        
        float rightNow = getGameTime();
        float nextShowTime = rightNow + 3600;
        setObjVar(self, "event.next_show_time", nextShowTime);
        messageTo(self, "fireworksTimerPing", null, 30, false);
        
        return SCRIPT_CONTINUE;
    }
    
    public int fireworksTimerReset(obj_id self, dictionary params) throws InterruptedException
    {
        float rightNow = getGameTime();
        float nextShowTime = rightNow + 3600;
        setObjVar(self, "event.next_show_time", nextShowTime);
        return SCRIPT_CONTINUE;
    }
    
    public int fireworksTimerPing(obj_id self, dictionary params) throws InterruptedException
    {
        float nextShowTime = getFloatObjVar(self, "event.next_show_time");
        float rightNow = getGameTime();
        
        if (rightNow > nextShowTime)
        {
            messageTo(self, "broadcastFireworkAnnouncement", null, 1, false);
        }
        else 
        {
            messageTo(self, "fireworksTimerPing", null, 3600, false);
        }
        
        return SCRIPT_CONTINUE;
    }
    
    public int broadcastFireworkAnnouncement(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] objPlayers = getPlayerCreaturesInRange(self, 512.0f);
        
        if (objPlayers != null && objPlayers.length > 0)
        {
            for (obj_id objPlayer : objPlayers)
            {
                sendSystemMessage(objPlayer, new string_id("event/birthday", "fireworks_broadcast"));
            }
        }
        
        messageTo(self, "startHugeFireworkDisplay", null, 10, false);
        return SCRIPT_CONTINUE;
    }
    
    public int startHugeFireworkDisplay(obj_id self, dictionary params) throws InterruptedException
    {
        for (int i = 0; i < 150; i++)
        {
            messageTo(self, "launchRandomFirework", null, i * 2, false);
        }
        
        messageTo(self, "fireworksTimerReset", null, 300, false);
        return SCRIPT_CONTINUE;
    }
    
    public int launchRandomFirework(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id effect = create.object(dataTableGetString(firework.TBL_FX, rand(1, dataTableGetNumRows(firework.TBL_FX)), "template"), utils.getRandomLocationInRing(getLocation(self), 0, 64));
        
        if (isIdValid(effect))
        {
            attachScript(effect, firework.SCRIPT_FIREWORK_CLEANUP);
        }
        
        return SCRIPT_CONTINUE;
    }
    
    public int OnHearSpeech(obj_id self, obj_id objSpeaker, String strText) throws InterruptedException
    {
        if (isPlayer(objSpeaker))
        {
            String lowerText = toLower(strText);
            String birthdaySetting = getConfigSetting("EventTeam", "birthdayEvent");
            
            if (lowerText.startsWith("startfireworks") ||
                lowerText.equals("yay!!!") || 
                lowerText.equals("yay") || 
                (birthdaySetting != null && birthdaySetting.equals("true") && lowerText.equals("happy birthday")))
            {
                messageTo(self, "broadcastFireworkAnnouncement", null, 1, false);
            }
        }
        
        return SCRIPT_CONTINUE;
    }

    // New method to launch random Rebel ship flyby
    public int launchRandomRebelShipFlyBy(obj_id self, dictionary params) throws InterruptedException
    {
        CustomerServiceLog("holidayEvent", "startfirework.launchRandomRebelShipFlyBy: messageHandler initialized.");
        int num = rand(1, 8);
        obj_id right = getObjIdObjVar(self, holiday.SPAWNER_PREFIX_OBJVAR + "right_most_waypoint");
        obj_id left = getObjIdObjVar(self, holiday.SPAWNER_PREFIX_OBJVAR + "left_most_waypoint");
        if (!isValidId(right) && !exists(right) && !isValidId(left) && !exists(left))
        {
            return SCRIPT_CONTINUE;
        }
        CustomerServiceLog("holidayEvent", "startfirework.launchRandomRebelShipFlyBy: random number received, firing something.");
        switch (num)
        {
            case 1:
                break;
            case 2:
                playRebelFlyBy(self, right, 2);
                break;
            case 3:
                playRebelFlyBy(self, left, 3);
                break;
            case 4:
                playRebelFlyBy(self, right, 4);
                break;
            case 5:
                playRebelFlyBy(self, right, 1);
                break;
            case 6:
                playRebelFlyBy(self, left, 2);
                break;
            case 7:
                playRebelFlyBy(self, right, 3);
                break;
            case 8:
                playRebelFlyBy(self, left, 4);
                break;
            default:
                playRebelFlyBy(self, left, 1);
                break;
        }
        return SCRIPT_CONTINUE;
    }
    
    public boolean playRebelFlyBy(obj_id self, obj_id playOnObject, int sequence) throws InterruptedException
    {
        CustomerServiceLog("holidayEvent", "startfirework.playRebelFlyBy: Function initialized.");
        if (!isValidId(self) || !exists(self))
        {
            return false;
        }
        if (!isValidId(playOnObject) || !exists(playOnObject))
        {
            return false;
        }
        CustomerServiceLog("holidayEvent", "startfirework.playRebelFlyBy" + sequence + ": playing.");
        location here = getLocation(playOnObject);
        playClientEffectLoc(getPlayerCreaturesInRange(here, 512.0f), getFlyBy(sequence), here, 1.0f);
        return true;
    }
    
    private String getFlyBy(int sequence)
    {
        switch(sequence)
        {
            case 1:
                return holiday.REBEL_FLYBY_PARTICLE_01;
            case 2:
                return holiday.REBEL_FLYBY_PARTICLE_02;
            case 3:
                return holiday.REBEL_FLYBY_PARTICLE_03;
            case 4:
                return holiday.REBEL_FLYBY_PARTICLE_04;
        }
        return holiday.REBEL_FLYBY_PARTICLE_01;
    }
}


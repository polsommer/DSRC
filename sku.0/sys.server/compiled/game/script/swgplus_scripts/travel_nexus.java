package script.swgplus_scripts;

import script.library.ai_lib;
import script.library.chat;
import script.library.create;
import script.library.pet_lib;
import script.*;

public class travel_nexus extends script.base_script
{
    public travel_nexus()
    {
    }
    public static final String travel_nexus_CONVO = "beta/travel_nexus";
    public int OnAttach(obj_id self) throws InterruptedException
    {
        setInvulnerable(self, true);
        setCondition(self, CONDITION_CONVERSABLE);
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
        setObjVar(self, "ai.diction", "townperson");
        setName(self, "Travel Agent");
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int mnu = mi.addRootMenu(menu_info_types.CONVERSE_START, null);
        menu_info_data mdata = mi.getMenuItemById(mnu);
        mdata.setServerNotify(false);
        return SCRIPT_CONTINUE;
    }
   public int OnStartNpcConversation(obj_id self, obj_id speaker) throws InterruptedException
    {
        chat.setGoodMood(self);
        setCondition(self, CONDITION_CONVERSABLE);
        setName(self, "Travel Agent");
        faceToBehavior(self, speaker);
        string_id greeting = new string_id(travel_nexus_CONVO, "start");
        string_id response[] = new string_id[2];
        response[0] = new string_id(travel_nexus_CONVO, "yes");
        response[1] = new string_id(travel_nexus_CONVO, "no");
        npcStartConversation(speaker, self, travel_nexus_CONVO, greeting, response);
        return SCRIPT_CONTINUE;
    }
    public int OnNpcConversationResponse(obj_id self, String convo, obj_id player, string_id response) throws InterruptedException
    {
        if (!convo.equals(travel_nexus_CONVO))
        {
            return SCRIPT_CONTINUE;
        }
        npcRemoveConversationResponse(player, response);
        if ((response.getAsciiId()).equals("yes"))
        {
            if (pet_lib.hasMaxPets(player, pet_lib.PET_TYPE_DROID))
            {
                string_id message = new string_id(travel_nexus_CONVO, "toomany");
                npcSpeak(player, message);
                npcEndConversation(player);
            }
            npcRemoveConversationResponse(player, new string_id(travel_nexus_CONVO, "no"));
            string_id message = new string_id(travel_nexus_CONVO, "choose");
            npcSpeak(player, message);
            string_id[] responses = new string_id[9];
            responses[0] = new string_id(travel_nexus_CONVO, "tatooine");
            responses[1] = new string_id(travel_nexus_CONVO, "corellia");
            responses[2] = new string_id(travel_nexus_CONVO, "dantooine");
            responses[3] = new string_id(travel_nexus_CONVO, "dathomir");
            responses[4] = new string_id(travel_nexus_CONVO, "endor");
            responses[5] = new string_id(travel_nexus_CONVO, "naboo");
            responses[6] = new string_id(travel_nexus_CONVO, "rori");
            responses[7] = new string_id(travel_nexus_CONVO, "talus");
            npcSetConversationResponses(player, responses);
            return SCRIPT_CONTINUE;
        }
        else if ((response.getAsciiId()).equals("no"))
        {
            npcRemoveConversationResponse(player, new string_id(travel_nexus_CONVO, "yes"));
            string_id message = new string_id(travel_nexus_CONVO, "goodbye");
            npcSpeak(player, message);
            npcEndConversation(player);
            return SCRIPT_CONTINUE;
        }
        String droidType = response.getAsciiId();
        String diction = "";
        if (droidType.equals("tatooine"))
        {
warpPlayer(player, "tatooine", 3528, 0, -4804, null, 0,0, 0, "", false);
        }
        if (droidType.equals("corellia"))
        {
warpPlayer(player, "corellia", -137, 28, -4723, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("dantooine"))
        {
warpPlayer(player, "dantooine", -617, 0, 2478, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("dathomir"))
        {
warpPlayer(player, "dathomir", -85, 0, -1600, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("endor"))
        {
warpPlayer(player, "endor", 3222, 0, -3467, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("naboo"))
        {
warpPlayer(player, "naboo", 1984, 0, 2154, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("rori"))
        {
warpPlayer(player, "rori", -5310, 0, -2221, null, 0, 0, 0, "", false);
        }
        if (droidType.equals("talus"))
        {
warpPlayer(player, "talus", 559, 0, -3028, null, 0, 0, 0, "", false);
        }
        string_id message = new string_id(travel_nexus_CONVO, "ok");
        npcSpeak(player, message);
        npcEndConversation(player);
        return SCRIPT_CONTINUE;
    }
}
    

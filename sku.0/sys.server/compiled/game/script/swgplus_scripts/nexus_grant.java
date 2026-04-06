package script.swgplus_scripts;
import script.*;
import script.library.instance;
import script.library.resource;
import script.library.sui;
import script.library.ai_lib;
import script.library.chat;
import script.library.create;
import script.library.pet_lib;
import script.library.groundquests;
import script.library.instance;
import script.library.utils;
import script.library.*;
import script.obj_id;
import script.prose_package;
import script.string_id;

public class nexus_grant extends script.base_script
{
    public nexus_grant()
    {
    }
    public static final String nexus_grant_CONVO = "beta/nexus_grant";
    public int OnAttach(obj_id self) throws InterruptedException
    {
        setInvulnerable(self, true);
        setCondition(self, CONDITION_CONVERSABLE);
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
        setObjVar(self, "ai.diction", "townperson");
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
        setName(self, "Heroic Unlock Terminal");
        setCondition(self, CONDITION_CONVERSABLE);
        faceToBehavior(self, speaker);
        string_id greeting = new string_id(nexus_grant_CONVO, "start");
        string_id response[] = new string_id[2];
        response[0] = new string_id(nexus_grant_CONVO, "yes");
        response[1] = new string_id(nexus_grant_CONVO, "no");
        npcStartConversation(speaker, self, nexus_grant_CONVO, greeting, response);
        return SCRIPT_CONTINUE;
    }
    public int OnNpcConversationResponse(obj_id self, String convo, obj_id player, string_id response) throws InterruptedException
    {
        if (!convo.equals(nexus_grant_CONVO))
        {
            return SCRIPT_CONTINUE;
        }
        npcRemoveConversationResponse(player, response);
        if ((response.getAsciiId()).equals("yes"))
        {
            npcRemoveConversationResponse(player, new string_id(nexus_grant_CONVO, "no"));
            string_id message = new string_id(nexus_grant_CONVO, "choose");
            npcSpeak(player, message);
            string_id[] responses = new string_id[8];
            responses[0] = new string_id(nexus_grant_CONVO, "grant");
            responses[1] = new string_id(nexus_grant_CONVO, "get_quest");
            responses[2] = new string_id(nexus_grant_CONVO, "complet_q");
            npcSetConversationResponses(player, responses);
            return SCRIPT_CONTINUE;
        }
    npcRemoveConversationResponse(player, response);
    if ((response.getAsciiId()).equals("yes"))
    {
        if (pet_lib.hasMaxPets(player, pet_lib.PET_TYPE_DROID))
        {
            string_id message = new string_id(nexus_grant_CONVO, "toomany");
            npcSpeak(player, message);
            npcEndConversation(player);
        }
        npcRemoveConversationResponse(player, new string_id(nexus_grant_CONVO, "no"));
        string_id message = new string_id(nexus_grant_CONVO, "choose");
        npcSpeak(player, message);
        string_id[] responses = new string_id[8];
        responses[0] = new string_id(nexus_grant_CONVO, "grant");
        responses[1] = new string_id(nexus_grant_CONVO, "get_quest");
        responses[2] = new string_id(nexus_grant_CONVO, "complet_q");
        npcSetConversationResponses(player, responses);
        return SCRIPT_CONTINUE;
    }
        else if ((response.getAsciiId()).equals("no"))
        {
            npcRemoveConversationResponse(player, new string_id(nexus_grant_CONVO, "yes"));
            string_id message = new string_id(nexus_grant_CONVO, "goodbye");
            npcSpeak(player, message);
            npcEndConversation(player);
            return SCRIPT_CONTINUE;
        }
        String heroictype = response.getAsciiId();
        String diction = "";
        if (heroictype.equals("grant"))
        {
            String[] instanceFlags = dataTableGetStringColumn(instance.INSTANCE_DATATABLE, "key_required");
            if (instanceFlags != null && instanceFlags.length > 0)
            {
                for (String flag : instanceFlags)
          {
                    if (flag != null && flag.length() > 0)
            {
                instance.flagPlayerForInstance(player, "heroic_tusken_army");
                setObjVar(player, "towship.metTravel", true);
                instance.flagPlayerForInstance(player, "heroic_axkva_min");
                instance.flagPlayerForInstance(player, flag);
            }
          }
                sendSystemMessageTestingOnly(player, "All Heroic Instances Flagged");
                sendSystemMessageTestingOnly(player, "NEW Nexus Grant v1 Access Granted!");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "No Instance Flags Found.");
            }
        }
if (heroictype.equals("get_quest"))
    {
        String[] instanceFlags = dataTableGetStringColumn(instance.INSTANCE_DATATABLE, "key_required");
        if (instanceFlags != null && instanceFlags.length > 0)
        {
            for (String flag : instanceFlags)
      {
                if (flag != null && flag.length() > 0)
        {
            groundquests.grantQuest(player, "axkva_min_intro");
            groundquests.grantQuest(player, "axkva_min_intro");
            groundquests.grantQuest(player, "axkva_min_intro");
            groundquests.grantQuest(player, "axkva_min_intro");
            groundquests.grantQuest(player, "axkva_min_intro");

        }
      }
            sendSystemMessageTestingOnly(player, "xxxxxxoirbxxxxx");
            sendSystemMessageTestingOnly(player, "xxxxxxoirbxxxxx");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "No Instance Flags Found.");
        }
    }
if (heroictype.equals("complet_q"))
{
    String[] instanceFlags = dataTableGetStringColumn(instance.INSTANCE_DATATABLE, "key_required");
    if (instanceFlags != null && instanceFlags.length > 0)
    {
        for (String flag : instanceFlags)
  {
            if (flag != null && flag.length() > 0)
    {
        groundquests.sendSignal(player, "axkva_min_intro");
        groundquests.sendSignal(player, "axkva_min_intro_01");
        groundquests.sendSignal(player, "axkva_min_intro_02");
        groundquests.sendSignal(player, "axkva_min_intro_03");
        groundquests.sendSignal(player, "axkva_min_intro_04");
        groundquests.sendSignal(player, "axkva_min_intro_05");
        groundquests.sendSignal(player, "axkva_min_intro_06");


    }
  }
        sendSystemMessageTestingOnly(player, "xxxxxx2oirb2xxxxx");
        sendSystemMessageTestingOnly(player, "xxxxxxoirbxxxxx");
    }
    else
    {
        sendSystemMessageTestingOnly(player, "No Instance Flags Found.");
    }
}
        string_id message = new string_id(nexus_grant_CONVO, "ok");
        npcSpeak(player, message);
        npcEndConversation(player);
        return SCRIPT_CONTINUE;
    }
}

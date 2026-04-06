package script.swgplus_scripts;

import script.library.instance;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.*;
import script.*;

public class daily_heroic_menu extends script.base_script
{
    public daily_heroic_menu()
    {
    }
    public static String c_stringFile = "conversation/daily_heroic_menu";

    public boolean daily_heroic_menu_condition__defaultCondition(obj_id player, obj_id npc) throws InterruptedException
    {
        return true;
    }

    public boolean daily_heroic_menu_condition_readyForInstance(obj_id player, obj_id npc) throws InterruptedException
    {
        return true;
    }

    public void daily_heroic_menu_action_sendToInstance(obj_id player, obj_id npc, String instanceName) throws InterruptedException
    {
        instance.requestInstanceMovement(player, instanceName);
    }

    public void daily_heroic_menu_action_sendQuestSignal(obj_id player, obj_id npc) throws InterruptedException
    {
 groundquests.isTaskActive(player, "feeder_tusken_01", "feeder_tusken_wait_02");
                groundquests.sendSignal(player, "feeder_tusken_signal_02");
    }

    public int daily_heroic_menu_handleBranch1(obj_id player, obj_id npc, string_id response) throws InterruptedException
    {
        if (response.equals("s_13"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                daily_heroic_menu_action_sendQuestSignal(player, npc);
                string_id message = new string_id(c_stringFile, "s_14");
                int numberOfResponses = 5;
                string_id[] responses = new string_id[numberOfResponses];
                 //responses[0] = new string_id(c_stringFile, "s_15");
                //responses[1] = new string_id(c_stringFile, "s_17");
                //responses[2] = new string_id(c_stringFile, "s_19");
                responses[3] = new string_id(c_stringFile, "s_21");
                //responses[4] = new string_id(c_stringFile, "s_23");

                utils.setScriptVar(player, "conversation.daily_heroic_menu.branchId", 2);
                npcSpeak(player, message);
                npcSetConversationResponses(player, responses);
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_DEFAULT;
    }

    public int daily_heroic_menu_handleBranch2(obj_id player, obj_id npc, string_id response) throws InterruptedException
    {
        if (response.equals("s_15"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                groundquests.isTaskActive(player, "feeder_tusken_01", "feeder_tusken_wait_02");
                groundquests.sendSignal(player, "feeder_tusken_signal_02");
                instance.flagPlayerForInstance(player, "daily_heroic_tusken_army");
                daily_heroic_menu_action_sendToInstance(player, npc, "daily_heroic_tusken_army");
                string_id message = new string_id(c_stringFile, "s_16");
                utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        else if (response.equals("s_17"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                daily_heroic_menu_action_sendToInstance(player, npc, "daily_heroic_axkva_min");
                string_id message = new string_id(c_stringFile, "s_18");
                utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        else if (response.equals("s_19"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                daily_heroic_menu_action_sendToInstance(player, npc, "daily_heroic_ig88");
                string_id message = new string_id(c_stringFile, "s_20");
                utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        else if (response.equals("s_21"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                daily_heroic_menu_action_sendToInstance(player, npc, "daily_heroic_star_destroyer");
                string_id message = new string_id(c_stringFile, "s_22");
                utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        else if (response.equals("s_23"))
        {
            if (daily_heroic_menu_condition__defaultCondition(player, npc))
            {
                daily_heroic_menu_action_sendToInstance(player, npc, "daily_heroic_exar_kun");
                string_id message = new string_id(c_stringFile, "s_24");
                utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_DEFAULT;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if ((!isTangible(self)) || (isPlayer(self)))
        {
            detachScript(self, "swgplus_scripts.daily_heroic_menu");
        }
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException
    {
        int menu = menuInfo.addRootMenu(menu_info_types.CONVERSE_START, null);
        menu_info_data menuInfoData = menuInfo.getMenuItemById(menu);
        menuInfoData.setServerNotify(false);
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        clearCondition(self, CONDITION_CONVERSABLE);
        detachScript(self, "swgplus_scripts.daily_heroic_menu");
        return SCRIPT_CONTINUE;
    }

    public boolean npcStartConversation(obj_id player, obj_id npc, String convoName, string_id greetingId, prose_package greetingProse, string_id[] responses) throws InterruptedException
    {
        Object[] objects = new Object[responses.length];
        System.arraycopy(responses, 0, objects, 0, responses.length);
        return npcStartConversation(player, npc, convoName, greetingId, greetingProse, objects);
    }

    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id npc = self;
        if (ai_lib.isInCombat(npc) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        if (daily_heroic_menu_condition_readyForInstance(player, npc))
        {
            string_id message = new string_id(c_stringFile, "s_4");
            int numberOfResponses = 1;
            boolean hasResponse = true;
            string_id[] responses = new string_id[numberOfResponses];
            responses[0] = new string_id(c_stringFile, "s_13");

            utils.setScriptVar(player, "conversation.daily_heroic_menu.branchId", 1);
            npcStartConversation(player, npc, "daily_heroic_menu", message, responses);
            return SCRIPT_CONTINUE;
        }
        if (daily_heroic_menu_condition__defaultCondition(player, npc))
        {
            string_id message = new string_id(c_stringFile, "s_12");
            chat.chat(npc, player, message);
            return SCRIPT_CONTINUE;
        }
        chat.chat(npc, "Error:  All conditions for OnStartNpcConversation were false.");
        return SCRIPT_CONTINUE;
    }

    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals("daily_heroic_menu"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id npc = self;
        int branchId = utils.getIntScriptVar(player, "conversation.daily_heroic_menu.branchId");
        if (branchId == 1 && daily_heroic_menu_handleBranch1(player, npc, response) == SCRIPT_CONTINUE)
        {
            return SCRIPT_CONTINUE;
        }
        if (branchId == 2 && daily_heroic_menu_handleBranch2(player, npc, response) == SCRIPT_CONTINUE)
        {
            return SCRIPT_CONTINUE;
        }
        chat.chat(npc, "Error:  Fell through all branches and responses for OnNpcConversationResponse.");
        utils.removeScriptVar(player, "conversation.daily_heroic_menu.branchId");
        return SCRIPT_CONTINUE;
    }
}


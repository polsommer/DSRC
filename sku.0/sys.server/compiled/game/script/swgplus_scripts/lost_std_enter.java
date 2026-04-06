package script.swgplus_scripts;
import script.library.instance;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.*;
import script.*;

public class lost_std_enter extends script.base_script
{
    public lost_std_enter()
    {
    }
    public static String c_stringFile = "conversation/lost_std_enter";
    public static final int MIN_HEROIC_GROUP_SIZE = 2;

    private boolean lost_std_enter_validateEntryRequirements(obj_id player, boolean sendFailureMessage) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
        {
            return false;
        }
        if (isDead(player) || isIncapacitated(player))
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "You must be conscious before entering the Lost Star Destroyer.", null);
            }
            return false;
        }
        if (ai_lib.isInCombat(player))
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "You cannot enter the Lost Star Destroyer while in combat.", null);
            }
            return false;
        }
        if (instance.isInInstanceArea(player))
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "You cannot queue for the Lost Star Destroyer while already in an instanced area.", null);
            }
            return false;
        }
        if (!group.isGrouped(player))
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "You need at least " + MIN_HEROIC_GROUP_SIZE + " players to begin this heroic encounter.", null);
            }
            return false;
        }
        obj_id groupObj = getGroupObject(player);
        if (!isIdValid(groupObj))
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "Your group could not be validated. Please regroup and try again.", null);
            }
            return false;
        }
        obj_id[] groupMembers = getGroupMemberIds(groupObj);
        int playerCount = 0;
        location playerLoc = getLocation(player);
        if (playerLoc == null)
        {
            return false;
        }
        if (groupMembers != null)
        {
            for (obj_id groupMember : groupMembers) {
                if (!isIdValid(groupMember) || !isPlayer(groupMember) || isDead(groupMember) || isIncapacitated(groupMember))
                {
                    continue;
                }
                if (instance.isInInstanceArea(groupMember))
                {
                    continue;
                }
                location groupMemberLoc = getLocation(groupMember);
                if (groupMemberLoc == null || groupMemberLoc.area == null || playerLoc.area == null)
                {
                    continue;
                }
                if (!groupMemberLoc.area.equals(playerLoc.area))
                {
                    continue;
                }
                if (getDistance(playerLoc, groupMemberLoc) > 128.0f)
                {
                    continue;
                }
                playerCount = playerCount + 1;
            }
        }
        if (playerCount < MIN_HEROIC_GROUP_SIZE)
        {
            if (sendFailureMessage)
            {
                sendSystemMessage(player, "You need at least " + MIN_HEROIC_GROUP_SIZE + " players in your group for this heroic.", null);
            }
            return false;
        }
        return true;
    }


    private obj_id[] lost_std_enter_getEligibleGroupMembers(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
        {
            return new obj_id[0];
        }
        location playerLoc = getLocation(player);
        if (playerLoc == null || playerLoc.area == null)
        {
            return new obj_id[0];
        }
        obj_id groupObj = getGroupObject(player);
        if (!isIdValid(groupObj))
        {
            return new obj_id[0];
        }
        obj_id[] groupMembers = getGroupMemberIds(groupObj);
        if (groupMembers == null || groupMembers.length == 0)
        {
            return new obj_id[0];
        }
        java.util.Vector eligibleMembers = new java.util.Vector();
        for (obj_id groupMember : groupMembers)
        {
            if (!isIdValid(groupMember) || !isPlayer(groupMember) || isDead(groupMember) || isIncapacitated(groupMember))
            {
                continue;
            }
            if (instance.isInInstanceArea(groupMember))
            {
                continue;
            }
            location groupMemberLoc = getLocation(groupMember);
            if (groupMemberLoc == null || groupMemberLoc.area == null)
            {
                continue;
            }
            if (!groupMemberLoc.area.equals(playerLoc.area))
            {
                continue;
            }
            if (getDistance(playerLoc, groupMemberLoc) > 128.0f)
            {
                continue;
            }
            eligibleMembers.add(groupMember);
        }
        if (eligibleMembers.isEmpty())
        {
            return new obj_id[0];
        }
        obj_id[] playersToMove = new obj_id[eligibleMembers.size()];
        eligibleMembers.toArray(playersToMove);
        return playersToMove;
    }

    public boolean lost_std_enter_condition__defaultCondition(obj_id player, obj_id npc) throws InterruptedException
    {
        return lost_std_enter_validateEntryRequirements(player, false);
    }
    public boolean lost_std_enter_condition_readyForInstance(obj_id player, obj_id npc) throws InterruptedException
    {
        return lost_std_enter_validateEntryRequirements(player, true);
    }
    public void lost_std_enter_action_sendToInstance(obj_id player, obj_id npc) throws InterruptedException
    {
        obj_id[] playersToMove = lost_std_enter_getEligibleGroupMembers(player);
        if (playersToMove == null || playersToMove.length == 0)
        {
            instance.requestInstanceMovement(player, "heroic_lost_star_destroyer");
            return;
        }
        for (obj_id groupMember : playersToMove)
        {
            instance.requestInstanceMovement(groupMember, "heroic_lost_star_destroyer");
        }
    }
    public void lost_std_enter_action_sendQuestSignal(obj_id player, obj_id npc) throws InterruptedException
    {
    }
    public int lost_std_enter_handleBranch1(obj_id player, obj_id npc, string_id response) throws InterruptedException
    {
        if (response.equals("s_13"))
        {
            if (lost_std_enter_condition__defaultCondition(player, npc))
            {
                lost_std_enter_action_sendQuestSignal(player, npc);
                string_id message = new string_id(c_stringFile, "s_14");
                int numberOfResponses = 0;
                boolean hasResponse = false;
                boolean hasResponse0 = false;
                if (lost_std_enter_condition__defaultCondition(player, npc))
                {
                    ++numberOfResponses;
                    hasResponse = true;
                    hasResponse0 = true;
                }
                if (hasResponse)
                {
                    int responseIndex = 0;
                    string_id responses[] = new string_id[numberOfResponses];
                    if (hasResponse0)
                    {
                        responses[responseIndex++] = new string_id(c_stringFile, "s_15");
                    }
                    utils.setScriptVar(player, "conversation.lost_std_enter.branchId", 2);
                    npcSpeak(player, message);
                    npcSetConversationResponses(player, responses);
                }
                else 
                {
                    ++numberOfResponses;
                    hasResponse = true;
                    hasResponse0 = true;
                }
                return SCRIPT_CONTINUE;
            }
            string_id message = new string_id(c_stringFile, "s_12");
            utils.removeScriptVar(player, "conversation.lost_std_enter.branchId");
            npcEndConversationWithMessage(player, message);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_DEFAULT;
    }
    public int lost_std_enter_handleBranch2(obj_id player, obj_id npc, string_id response) throws InterruptedException
    {
        if (response.equals("s_15"))
        {
            if (lost_std_enter_condition__defaultCondition(player, npc))
            {
                lost_std_enter_action_sendToInstance(player, npc);
                string_id message = new string_id(c_stringFile, "s_16");
                utils.removeScriptVar(player, "conversation.lost_std_enter.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
            string_id message = new string_id(c_stringFile, "s_12");
            utils.removeScriptVar(player, "conversation.lost_std_enter.branchId");
            npcEndConversationWithMessage(player, message);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_DEFAULT;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if ((!isTangible(self)) || (isPlayer(self)))
        {
            detachScript(self, "swgplus_scripts.lost_std_enter");
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
        detachScript(self, "swgplus_scripts.lost_std_enter");
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
        if (lost_std_enter_condition_readyForInstance(player, npc))
        {
            string_id message = new string_id(c_stringFile, "s_4");
            int numberOfResponses = 0;
            boolean hasResponse = false;
            boolean hasResponse0 = false;
            if (lost_std_enter_condition__defaultCondition(player, npc))
            {
                ++numberOfResponses;
                hasResponse = true;
                hasResponse0 = true;
            }
            if (hasResponse)
            {
                int responseIndex = 0;
                string_id responses[] = new string_id[numberOfResponses];
                if (hasResponse0)
                {
                    responses[responseIndex++] = new string_id(c_stringFile, "s_13");
                }
                utils.setScriptVar(player, "conversation.lost_std_enter.branchId", 1);
                npcStartConversation(player, npc, "lost_std_enter", message, responses);
            }
            else 
            {
                chat.chat(npc, player, message);
            }
            return SCRIPT_CONTINUE;
        }
        if (lost_std_enter_condition__defaultCondition(player, npc))
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
        if (!conversationId.equals("lost_std_enter"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id npc = self;
        int branchId = utils.getIntScriptVar(player, "conversation.lost_std_enter.branchId");
        if (branchId == 1 && lost_std_enter_handleBranch1(player, npc, response) == SCRIPT_CONTINUE)
        {
            return SCRIPT_CONTINUE;
        }
        if (branchId == 2 && lost_std_enter_handleBranch2(player, npc, response) == SCRIPT_CONTINUE)
        {
            return SCRIPT_CONTINUE;
        }
        chat.chat(npc, "Error:  Fell through all branches and responses for OnNpcConversationResponse.");
        utils.removeScriptVar(player, "conversation.lost_std_enter.branchId");
        return SCRIPT_CONTINUE;
    }
}

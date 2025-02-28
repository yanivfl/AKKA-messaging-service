import SharedMessages.Messages.*;
import Users.Constants;
import Users.UserInfo;
import Groups.GroupInfo;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.Broadcast;

import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//for mute action
import java.util.Timer;

public class Manager extends AbstractActor {

    private LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private ConcurrentMap<String, UserInfo> usersMap = new ConcurrentHashMap<>(); //user info
    private ConcurrentMap<String, GroupInfo> groupsMap = new ConcurrentHashMap<>(); // group info


    @Override
    public
    Receive createReceive() {
        return receiveBuilder()
                //validators from client
                .match(validateUserSendMessage.class, this::onValidateUserSendMessage)
                .match(validateGroupInvite.class, this::onValidateGroupInviteMessage)
                .match(validateGroupSendMessage.class, this::onValidateGroupSendMessage)

                //user reqs
                .match(ConnectionMessage.class, this::onConnectMessage)
                .match(DisconnectMessage.class, this::onDisconnectMessage)

                //group reqs
                .match(GroupCreateMessage.class,this::onGroupCreateMessage)
                .match(GroupLeaveMessage.class, this::onGroupLeaveMessage)
                .match(GroupInviteMessage.class, this::onGroupInviteMessage)
                .match(GroupRemoveMessage.class, this::onGroupRemoveMessage)
                .match(GroupMuteMessage.class, this::onGroupMuteMessage)
                .match(GroupUnMuteMessage.class, this::onGroupUnMuteMessage)
                .match(GroupCoAdminAddMessage.class, this::onGroupCoAdminAddMessage)
                .match(GroupCoAdminRemoveMessage.class, this::onGroupCoAdminRemoveMessage)
                .build();
    }

    //*************************** Validators from client *******************************//

    /**
     * recieves a validation request from client regarding target user
     * if validation passed -> manager sends client the target ActorRef
     * if validation failed -> sends client error message
     * @param msg
     */
    private void onValidateUserSendMessage(validateUserSendMessage msg) {
        logger.info("Got a IsUserExistMessage");
        if (!ValidateIsUserExist(msg.targetUserName, true)) return;

        ActorRef targetActor = usersMap.get(msg.targetUserName).getActor();
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
    }

    /**
     * recieves a validation request from client regarding group
     * if validation passed -> manager sends client the router ActorRef
     * if validation failed -> sends client error message
     * @param msg
     */
    private void onValidateGroupSendMessage(validateGroupSendMessage msg) {
        logger.info("got a group send text message!");
        if(!ValidateIsUserExist(msg.sourceName, true)) return;
        if(!ValidateIsGroupExist(msg.groupName, true)) return;
        if(!ValidateIsGroupContainsUser(groupsMap.get(msg.groupName),
                msg.sourceName, true)) return;
        if(ValidateIsGroupUserMuted(groupsMap.get(msg.groupName),
                msg.sourceName, false)) return;

        ActorRef broadcastRouter = groupsMap.get(msg.groupName).
                getGroupRouter().
                getBroadcastRouter((ActorCell) getContext(),
                usersMap.get(msg.sourceName).getActor());
        getSender().tell(new AddressMessage(broadcastRouter), ActorRef.noSender());
    }

    /**
     * recieves a validation request from client regarding target to invite to group
     * if validation passed -> manager sends client the target ActorRef
     * if validation failed -> sends client error message
     * @param msg
     */
    private void onValidateGroupInviteMessage(validateGroupInvite msg) {
        logger.info("Got a invite Group Message");
        String groupName = msg.groupName;
        String sourceUserName = msg.sourceUserName;
        String targetUserName = msg.targetUserName;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator
        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsGroupContainsUser(group, sourceUserName, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceUserName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (ValidateIsGroupContainsUser(group, targetUserName, false)) return;

        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
    }


    //*************************** user Requests from client *******************************//

    /**
     * on recieving connection message, manager validates request
     * if validation passed -> manager adds client and sends text message to client
     * if validation failed -> manager sends error message
     * @param connectMsg
     */
    private void onConnectMessage(ConnectionMessage connectMsg) {
        logger.info("Got a connection Message");
        if (ValidateIsUserExist(connectMsg.userName, false)) return;

        logger.info("Creating new user: " + connectMsg.userName);
        usersMap.put(connectMsg.userName, new UserInfo(new LinkedList<>(), connectMsg.client));
        logger.info("Debug- the users map:\n " + usersMap.toString());
        getSender().tell(new TextMessage(Constants.CONNECT_SUCC(connectMsg.userName)), ActorRef.noSender());

    }

    /**
     * on recieving disconnection message, manager validates request
     * if validation passed -> manager deletes client (removes him from all groups) and sends text message to client
     * if validation failed -> manager sends error message
     * if leaving group fails -> manager sends error message
     * @param disconnectMsg
     */
    private void onDisconnectMessage(DisconnectMessage disconnectMsg) {
        logger.info("Got a disconnection Message");
        if (!ValidateIsUserExist(disconnectMsg.userName, true)) return;

        List<String> usergroupNames = new LinkedList<>(usersMap.get(disconnectMsg.userName).getGroups());

        //return true if all user succescfully left all groups
        if(!usergroupNames.stream().
                allMatch(groupName -> onGroupLeaveMessage(
                        new GroupLeaveMessage(groupName, disconnectMsg.userName)))) { return; }

        usersMap.remove(disconnectMsg.userName);
        getSender().tell(new TextMessage(Constants.DISCONNECT_SUCC(disconnectMsg.userName)), ActorRef.noSender());

        logger.info("disconnecting user: " + disconnectMsg.userName);
        logger.info("Debug- the users map:\n " + usersMap.toString());
    }


    //*************************** Groups reqs from client *******************************//
    /**
     * on recieving group create message, manager validates request
     * if validation passed -> manager create group with client as admin, and sends text message to client
     * if validation failed -> manager sends error message
     * @param createMsg
     */
    private void onGroupCreateMessage(GroupCreateMessage createMsg) {
        logger.info("Got a Create Group Message");
        if(ValidateIsGroupExist(createMsg.groupName, false)) return;

        logger.info("Creating new group: " + createMsg.groupName);
        groupsMap.put(createMsg.groupName,  new GroupInfo(createMsg.groupName, createMsg.userName, getSender()));
        usersMap.get(createMsg.userName).getGroups().add(createMsg.groupName);
        getSender().tell(new TextMessage(Constants.GROUP_CREATE_SUCC(createMsg.groupName)), ActorRef.noSender());

        logger.info("Debug- the usersgroups map:\n " + groupsMap.toString());
        logger.info("admin path is::\n " +  getSender().path().toString());
    }


    /**
     * on recieving group leave message, manager validates request
     * if validation passed -> user leaves group, manager broadcasts messages according to
     * users type.
     *  if admin -> broadcast admin + coadmin + user/muted message
     *  if coadmin -> broadcast coadmin + user/muted message.
     *  if user/muted -> broadcast user/muted message.
     * if validation failed -> manager sends error message
     * @param leaveMsg
     */
    private boolean onGroupLeaveMessage(GroupLeaveMessage leaveMsg) {
        logger.info("Got a Leave Group Message");
        logger.info("user map: " + usersMap.toString());
        String groupName = leaveMsg.groupName;
        String userName = leaveMsg.userName;
        GroupInfo group = groupsMap.get(groupName);
        ActorRef sourceActor = usersMap.get(userName).getActor();

        if (!ValidateIsUserExist(userName, true)) return false;
        if (!ValidateIsGroupExist(groupName, true)) return false;
        if (!ValidateIsGroupContainsUser(group, userName, true)) return false;

        boolean deleteGroup = false;
       ActorRef broadcastRouter = group.getGroupRouter().
               getBroadcastRouter((ActorCell) getContext(), sourceActor);
        switch (group.getUserGroupMode(userName)){
            case ADMIN:
                deleteGroup = true;
                broadcastRouter.tell( new Broadcast(
                        new TextMessage(Constants.GROUP_ADMIN_LEAVE(groupName, userName))),
                        ActorRef.noSender());

            case CO_ADMIN:
                broadcastRouter.tell( new Broadcast( new TextMessage(
                        Constants.GROUP_COADMIN_LEAVE_SUCC(groupName,userName))),
                        ActorRef.noSender());

            case MUTED:
            case USER:
                broadcastRouter.tell( new Broadcast(
                        new TextMessage(Constants.GROUP_LEAVE_SUCC(groupName, userName))),
                        ActorRef.noSender());
                removeUserFromGroup(groupName, userName, group);
                break;
            case NONE:
                logger.info("DEBUG - Manager should not Reach this point!");
                return false;
        }
        if(deleteGroup){
            group.getAllUsers().
                    forEach((username) -> removeUserFromGroup(groupName, username, group));
            groupsMap.remove(groupName);
        }
        broadcastRouter.tell(PoisonPill.getInstance(), ActorRef.noSender());
        logger.info(userName + " left " + groupName);
        return true;
    }

    /**
     * on recieving group invite message, manager validates request
     * if validation passed -> manager adds targetUser to group
     * if validation failed -> manager sends error message
     * @param inviteMsg
     */
    private void onGroupInviteMessage(GroupInviteMessage inviteMsg) {
        logger.info("Got a invite Group Message");

        GroupInfo group = groupsMap.get(inviteMsg.groupName); //returns null if doesn't exist. we will leave function in validator
        addUserToGroup(inviteMsg.groupName, inviteMsg.targetUserName, group);
        getSender().tell(new isSuccMessage(true), ActorRef.noSender());

        logger.info("group after invite is: " + group.toString());
    }

    /**
     * on recieving group remove message, manager validates request
     * if validation passed -> manager removes targetUser from group
     * if validation failed -> manager sends error message
     * @param RemoveMsg
     */
    private void onGroupRemoveMessage(GroupRemoveMessage RemoveMsg) {
        logger.info("Got a remove Message");
        String groupName = RemoveMsg.groupName;
        String sourceUserName = RemoveMsg.sourceUserName;
        String targetUserName = RemoveMsg.targetUserName;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator

        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceUserName, true)) return;
        // extra pre-conditions
        if (!ValidateIsUserExist(sourceUserName, true)) return;
        if (!ValidateIsGroupContainsUser(group, targetUserName, true)) return;
        if (!ValidateIsGroupContainsUser(group, sourceUserName, true)) return;
        if (ValidateIsUserAdmin(group, targetUserName, false)) return;

        if(group.getUserGroupMode(targetUserName) == GroupInfo.groupMode.CO_ADMIN) //co-admin cant remove co-admin, only admin
            if (!ValidateIsUserAdmin(group, sourceUserName, true)) return;
        //pre-conditions checked!
        logger.info(targetUserName + " will be removed from the "+ groupName);

        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        removeUserFromGroup(groupName, targetUserName, group);
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());

        logger.info(group.toString());
    }

    /**
     * on recieving group mute message, manager validates request
     * if validation passed -> manager mutes targetUser in requested group
     * if validation failed -> manager sends error message
     * @param muteMsg
     */
    private void onGroupMuteMessage(GroupMuteMessage muteMsg) {
        logger.info("Got a mute Message");
        String groupName = muteMsg.groupName;
        String sourceUserName = muteMsg.sourceUserName;
        String targetUserName = muteMsg.targetUserName;
        int timeInMute = muteMsg.timeInSeconds;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator

        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceUserName, true)) return;
        // extra pre-conditions
        if (!ValidateIsUserExist(sourceUserName, true)) return;
        if (!ValidateIsGroupContainsUser(group, targetUserName, true)) return;
        if (ValidateIsUserAdmin(group, targetUserName, false)) return;

        // if targetUserName is co-admin pre-conditions
        if(group.getUserGroupMode(targetUserName) == GroupInfo.groupMode.CO_ADMIN)
            if (!ValidateIsUserAdmin(group, sourceUserName, true)) return;


        //pre-conditions checked!
        logger.info(targetUserName + " will be muted in the "+ groupName+ "for "+ timeInMute + " seconds");
        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        group.muteUser(targetUserName, targetActor);
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
        logger.info(group.toString());

        //Scheduling unMutedAutomatically() call in timeInMute second.
        Timer timer = new Timer();
        timer.schedule(new unMutedAutomatically(group,targetUserName,targetActor,timer), Constants.toSeconds(timeInMute));
    }

    /**
     * on recieving group unMute message, manager validates request
     * if validation passed -> manager un mutes targetUser in requested group
     * if validation failed -> manager sends error message
     * @param unMuteMsg
     */
    private void onGroupUnMuteMessage(GroupUnMuteMessage unMuteMsg) {
        logger.info("Got a unmute Message");
        String groupName = unMuteMsg.groupName;
        String sourceUserName = unMuteMsg.sourceUserName;
        String targetUserName = unMuteMsg.targetUserName;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator

        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceUserName, true)) return;
        if(!ValidateIsGroupUserMuted(group, targetUserName, true)) return;

        // pre-conditions checked!
        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        group.unMuteUser(targetUserName, targetActor);
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
    }

    /**
     * on recieving group CoAdminAddMsg message, manager validates request
     * if validation passed -> manager promotes targetUser to co-admin in requested group
     * if validation failed -> manager sends error message
     * @param CoAdminAddMsg
     */
    private void onGroupCoAdminAddMessage(GroupCoAdminAddMessage CoAdminAddMsg) {
        logger.info("Got a Coadmin add Message");
        String groupName = CoAdminAddMsg.groupName;
        String sourceUserName = CoAdminAddMsg.sourceUserName;
        String targetUserName = CoAdminAddMsg.targetUserName;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator

        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (!ValidateIsUserAdmin(group, sourceUserName, true)) return; // only admin can promote user to co-admin
        // extra pre-conditions
        if (!ValidateIsGroupContainsUser(group, targetUserName, true)) return;
        if (ValidateIsUserAdmin(group, targetUserName, false)) return; // admin can`t promote to co-admin
        if (ValidateIsUserCoAdmin(group, targetUserName, false)) return; //  co-admin can`t promote to co-admin, already co-admin


        //pre-conditions checked!
        logger.info(targetUserName + " will be be added to the "+ groupName +" co-admin list");
        String msg = "You have been promoted to co-admin in " + groupName + "!";
        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        ActorRef sourceActor = usersMap.get(sourceUserName).getActor();
        group.promoteToCoAdmin(targetUserName,targetActor);
        targetActor.tell(new TextMessage(msg), sourceActor);
        logger.info(group.toString());

    }

    /**
     * on recieving group CoadminRemoveMsg message, manager validates request
     * if validation passed -> manager de-motes targetUser to user in requested group
     * if validation failed -> manager sends error message
     * @param CoadminRemoveMsg
     */
    private void onGroupCoAdminRemoveMessage(GroupCoAdminRemoveMessage CoadminRemoveMsg) {
        logger.info("Got a Coadmin remove Message");
        String groupName = CoadminRemoveMsg.groupName;
        String sourceUserName = CoadminRemoveMsg.sourceUserName;
        String targetUserName = CoadminRemoveMsg.targetUserName;
        GroupInfo group = groupsMap.get(groupName); //returns null if doesn't exist. we will leave function in validator

        // check all pre-conditions
        if (!ValidateIsGroupExist(groupName, true)) return;
        if (!ValidateIsUserExist(targetUserName, true)) return;
        if (!ValidateIsUserAdmin(group, sourceUserName, true)) return; // only admin can demote co-admin to user
        // extra pre-conditions
        if (!ValidateIsGroupContainsUser(group, targetUserName, true)) return;
        if (ValidateIsUserAdmin(group, targetUserName, false)) return;
        if (!ValidateIsUserCoAdmin(group, targetUserName, true)) return; //  can`t demote if user isn't co-admin


        //pre-conditions checked!
        logger.info(targetUserName + " will be removed from the "+ groupName +" co-admin list");
        String msg = "You have been demoted to user in " + groupName + "!";
        ActorRef targetActor = usersMap.get(targetUserName).getActor();
        ActorRef sourceActor = usersMap.get(sourceUserName).getActor();
        group.demoteCoAdmin(targetUserName);
        targetActor.tell(new TextMessage(msg), sourceActor);
        logger.info(group.toString());
    }



    /** Auxiliary methods **/

    /**
     * adds userName to groupName
     * @param groupName
     * @param userName
     * @param group
     */
    private void addUserToGroup(String groupName, String userName, GroupInfo group) {
        group.getUsers().add(userName);
        group.getGroupRouter().addRoutee(usersMap.get(userName).getActor());
        usersMap.get(userName).getGroups().add(groupName);
        logger.info("added " + userName + " to group " + group.toString());
    }

    /**
     * removes userName from groupName
     * @param groupName
     * @param userName
     * @param group
     */
    private void removeUserFromGroup(String groupName, String userName, GroupInfo group) {
        logger.info("removing " + userName + " from " + groupName);
        group.removeUsername(userName);
        group.getGroupRouter().removeRoutee(usersMap.get(userName).getActor());
        usersMap.get(userName).getGroups().remove(groupName);
        logger.info("group after remove is: " + group.toString());
    }



//****************************Validators**********************************//

    /**
     * validates if user is muted with expected behavior
     * if not, sends error to user
     * @param group
     * @param userName
     * @param expectedMuted
     * @return true iff user is muted
     */
    private boolean ValidateIsGroupUserMuted(GroupInfo group,String userName, boolean expectedMuted){
        boolean isMuted = group.isMuted(userName);
        if(!isMuted && expectedMuted){
            logger.info(Constants.GROUP_UN_MUTE_ERROR(userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_UN_MUTE_ERROR(userName)), ActorRef.noSender());
        }
        if(isMuted && !expectedMuted){
            logger.info(Constants.GROUP_MUTED_ERROR);
            getSender().tell(new ErrorMessage(Constants.GROUP_MUTED_ERROR), ActorRef.noSender());
        }
        return isMuted;
    }

    /**
     * validates if user is admin with expected behavior
     * if not, sends error to user
     * @param group
     * @param userName
     * @param expectedMuted
     * @return true iff user is admin
     */
    private boolean ValidateIsUserAdmin(GroupInfo group,String userName, boolean expectedMuted) {
        boolean isAdmin = group.isAdmin(userName);
        if(!isAdmin && expectedMuted){
            logger.info(Constants.GROUP_NOT_HAVE_ADMIN_PREVILEDGES(group.getGroupName()));
            getSender().tell(new ErrorMessage(Constants.GROUP_NOT_HAVE_ADMIN_PREVILEDGES(group.getGroupName())), ActorRef.noSender());
        }
        if(isAdmin && !expectedMuted){
            logger.info(Constants.GROUP_ACTION_ON_ADMIN(group.getGroupName(),userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_ACTION_ON_ADMIN(group.getGroupName(),userName)), ActorRef.noSender());
        }
        return isAdmin;
    }

    /**
     * validates if user is co-admin with expected behavior
     * if not, sends error to user
     * @param group
     * @param userName
     * @param expectedMuted
     * @return true iff user is co admin
     */
    private boolean ValidateIsUserCoAdmin(GroupInfo group,String userName, boolean expectedMuted) {
        boolean isCoAdmin = group.isCoAdmin(userName);
        if(!isCoAdmin && expectedMuted){
            logger.info(Constants.GROUP_COADMIN_REMOVE_ERROR(group.getGroupName(),userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_COADMIN_REMOVE_ERROR(group.getGroupName(),userName)), ActorRef.noSender());
        }
        if(isCoAdmin && !expectedMuted){
            logger.info(Constants.GROUP_COADMIN_ADD_ERROR(group.getGroupName(),userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_COADMIN_ADD_ERROR(group.getGroupName(),userName)), ActorRef.noSender());
        }
        return isCoAdmin;
    }

    /**
     * validates if group contains user with expected beavior.
     * if not -> sends error
     * @param group
     * @param userName
     * @param expectedContained
     * @return true iff user is in group
     */
    private boolean ValidateIsGroupContainsUser(GroupInfo group, String userName, boolean expectedContained){
        boolean isContained = !group.getUserGroupMode(userName).equals(GroupInfo.groupMode.NONE);
        if(isContained && !expectedContained){
            logger.info(Constants.GROUP_TARGET_ALREADY_BELONGS(group.getGroupName(), userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_TARGET_ALREADY_BELONGS(group.getGroupName(), userName)), ActorRef.noSender());
        }
        if(!isContained && expectedContained){
            logger.info(Constants.GROUP_LEAVE_FAIL(group.getGroupName(), userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_LEAVE_FAIL(group.getGroupName(), userName)), ActorRef.noSender());
        }
        return isContained;
    }

    /**
     * validates if group exists with expected behavior
     * if not -> sends error
     * @param groupName
     * @param expectedExist
     * @return true iff group exists
     */
    private boolean ValidateIsGroupExist(String groupName, boolean expectedExist){
        boolean isExist = groupsMap.containsKey(groupName);
        if(isExist && !expectedExist){
            logger.info(Constants.GROUP_CREATE_FAIL(groupName));
            getSender().tell(new ErrorMessage(Constants.GROUP_CREATE_FAIL(groupName)), ActorRef.noSender());
        }
        if(!isExist && expectedExist){
            logger.info(Constants.NOT_EXIST(groupName));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(groupName)), ActorRef.noSender());
        }
        return isExist;
    }

    /**
     * validates if user has privilege with expected behavior
     * @param group
     * @param userName
     * @param expectedPrivilege
     * @return true iff user has privilege
     */
    private boolean ValidateUserHasPriviledges(GroupInfo group,String userName, boolean expectedPrivilege) {
        boolean isPrivilege = group.userHasPrivileges(userName);
        if(isPrivilege && !expectedPrivilege){
            logger.info(Constants.GROUP_ALREADY_HAVE_PREVILEDGES(group.getGroupName(), userName));
            getSender().tell(new ErrorMessage(Constants.GROUP_ALREADY_HAVE_PREVILEDGES(group.getGroupName(), userName)), ActorRef.noSender());
        }
        if(!isPrivilege && expectedPrivilege){
            logger.info(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName()));
            getSender().tell(new ErrorMessage(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName())), ActorRef.noSender());
        }
        return isPrivilege;
    }

    /**
     * validates if user exists with expected behavior
      * @param userName
     * @param expectedExist
     * @return true iff user exists
     */
    private boolean ValidateIsUserExist(String userName, boolean expectedExist) {
        boolean isExist = usersMap.containsKey(userName);
        if(isExist && !expectedExist){
            logger.info(Constants.CONNECT_FAIL(userName));
            getSender().tell(new ErrorMessage(Constants.CONNECT_FAIL(userName)), ActorRef.noSender());
        }
        if(!isExist && expectedExist){
            logger.info(Constants.NOT_EXIST(userName));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(userName)), ActorRef.noSender());
        }
        return isExist;
    }

}
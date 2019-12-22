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


public class Manager extends AbstractActor {

    private LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private ConcurrentMap<String, UserInfo> usersMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, GroupInfo> groupsMap = new ConcurrentHashMap<>();


    @Override
    public
    Receive createReceive() {
        return receiveBuilder()
                .match(validateUserSendMessage.class, this::onValidateUserSendMessage)
                .match(validateGroupInvite.class, this::onValidateGroupInvite)
                .match(validateGroupSendMessage.class, this::onValidateGroupSendMessage)

                .match(ConnectionMessage.class, this::onConnect)
                .match(DisconnectMessage.class, this::onDisconnect)


                .match(GroupCreateMessage.class,this::onGroupCreate)
                .match(GroupLeaveMessage.class, this::onGroupLeave)
                .match(GroupInviteMessage.class, this::onGroupInvite)
                .match(GroupRemoveMessage.class, this::onGroupRemoveMessage)

                .match(GroupCoadminAddMessage.class, this::onGroupCoadminAddMessage)
                .match(GroupCoadminRemoveMessage.class, this::onGroupCoadminRemoveMessage)
                .build();
    }

    private void onValidateUserSendMessage(validateUserSendMessage msg) {
        logger.info("Got a IsUserExistMessage");
        if (!ValidateIsUserExist(msg.targetusername, true)) return;

        ActorRef targetActor = usersMap.get(msg.targetusername).getActor();
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
    }

    private void onValidateGroupSendMessage(validateGroupSendMessage msg) {
        logger.info("got a group send text message!");
        if(!ValidateIsUserExist(msg.sourceName, true)) return;
        if(!ValidateIsGroupExist(msg.groupName, true)) return;
        if(!ValidateIsGroupContainsUser(groupsMap.get(msg.groupName),
                msg.sourceName, true)) return;

        ActorRef broadcastRouter = groupsMap.get(msg.groupName).
                getGroupRouter().
                getBroadcastRouter((ActorCell) getContext(),
                usersMap.get(msg.sourceName).getActor());
        getSender().tell(new AddressMessage(broadcastRouter), ActorRef.noSender());
    }

    private void onValidateGroupInvite(validateGroupInvite msg) {
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

    private void onConnect(ConnectionMessage connectMsg) {
        logger.info("Got a connection Message");
        if (ValidateIsUserExist(connectMsg.username, false)) return;

        logger.info("Creating new user: " + connectMsg.username);
        usersMap.put(connectMsg.username, new UserInfo(new LinkedList<>(), connectMsg.client));
        logger.info("Debug- the users map:\n " + usersMap.toString());
        getSender().tell(new TextMessage(Constants.CONNECT_SUCC(connectMsg.username)), ActorRef.noSender());

    }

    private void onDisconnect(DisconnectMessage disconnectMsg) {
        logger.info("Got a disconnection Message");
        if (!ValidateIsUserExist(disconnectMsg.username, true)) return;

        List<String> userGroupNames = new LinkedList<>(usersMap.get(disconnectMsg.username).getGroups());
        for( String groupName : userGroupNames ){
           if(!onGroupLeave(new GroupLeaveMessage(groupName, disconnectMsg.username))){ return; }
            logger.info(disconnectMsg.username + " left " + groupName);
        }
        usersMap.remove(disconnectMsg.username);
        getSender().tell(new TextMessage(Constants.DISCONNECT_SUCC(disconnectMsg.username)), ActorRef.noSender());

        logger.info("disconnecting user: " + disconnectMsg.username);
        logger.info("Debug- the users map:\n " + usersMap.toString());
    }

    private void onGroupCreate(GroupCreateMessage createMsg) {
        logger.info("Got a Create Group Message");
        if(ValidateIsGroupExist(createMsg.groupname, false)) return;

        logger.info("Creating new group: " + createMsg.groupname);
        groupsMap.put(createMsg.groupname,  new GroupInfo(createMsg.groupname, createMsg.username, getSender()));
        usersMap.get(createMsg.username).getGroups().add(createMsg.groupname);
        getSender().tell(new TextMessage(Constants.GROUP_CREATE_SUCC(createMsg.groupname)), ActorRef.noSender());

        logger.info("Debug- the usersgroups map:\n " + groupsMap.toString());
        logger.info("admin path is::\n " +  getSender().path().toString());
    }



    private boolean onGroupLeave(GroupLeaveMessage leaveMsg) {
        logger.info("Got a Leave Group Message");
        logger.info("user map: " + usersMap.toString());
        String groupname = leaveMsg.groupname;
        String username = leaveMsg.username;
        GroupInfo group = groupsMap.get(groupname);
        ActorRef sourceActor = usersMap.get(username).getActor();

        if (!ValidateIsUserExist(username, true)) return false;
        if (!ValidateIsGroupExist(groupname, true)) return false;
        if (!ValidateIsGroupContainsUser(group, username, true)) return false;

        boolean deleteGroup = false;
       ActorRef broadcastRouter = group.getGroupRouter().
               getBroadcastRouter((ActorCell) getContext(), sourceActor);
        switch (group.getUserGroupMode(username)){
            case ADMIN:
                deleteGroup = true;
                broadcastRouter.tell( new Broadcast(
                        new TextMessage(Constants.GROUP_ADMIN_LEAVE(groupname, username))),
                        ActorRef.noSender());

            case CO_ADMIN:
                broadcastRouter.tell( new Broadcast( new TextMessage(
                        Constants.GROUP_COADMIN_LEAVE_SUCC(groupname,username))),
                        ActorRef.noSender());

            case MUTED:
            case USER:
                broadcastRouter.tell( new Broadcast(
                        new TextMessage(Constants.GROUP_LEAVE_SUCC(groupname, username))),
                        ActorRef.noSender());
                removeUserFromGroup(groupname, username, group);
                break;
            case NONE:
                logger.info("DEBUG - Manager should not Reach this point!");
                return false;
        }
        if(deleteGroup){
            for(String userName : group.getAllUsers()){
                removeUserFromGroup(groupname, userName, group);
            }
            groupsMap.remove(groupname);
        }
        return true;
    }

    private void onGroupInvite(GroupInviteMessage inviteMsg) {
        logger.info("Got a invite Group Message");
        GroupInfo group = groupsMap.get(inviteMsg.groupName); //returns null if doesn't exist. we will leave function in validator
        addUserToGroup(inviteMsg.groupName, inviteMsg.targetUserName, group);
        getSender().tell(new isSuccMessage(true), ActorRef.noSender());
        logger.info("group after invite is: " + group.toString());
    }

    private void onGroupRemoveMessage(GroupRemoveMessage RemoveMsg) {
        logger.info("Got a remove Message");
        String groupname = RemoveMsg.groupname;
        String sourceusername = RemoveMsg.sourceusername;
        String targetusername = RemoveMsg.targetusername;
        GroupInfo group = groupsMap.get(groupname); //returns null if doesn't exist. we will leave function in validator
        // check all pre-conditions
        if (!ValidateIsGroupExist(groupname, true)) return;
        if (!ValidateIsUserExist(targetusername, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceusername, true)) return;
        // extra pre-conditions
        if (!ValidateIsUserExist(sourceusername, true)) return;
        if (!ValidateIsGroupContainsUser(group, targetusername, true)) return;
        if (!ValidateIsGroupContainsUser(group, sourceusername, true)) return;
        if (ValidateIsUserAdmin(group, targetusername, false)) return;

        //pre-conditions checked!
        logger.info(targetusername + " will be removed from the "+ groupname);
        ActorRef targetActor = usersMap.get(targetusername).getActor();
        removeUserFromGroup(groupname, targetusername, group);
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
        logger.info(group.toString());
    }

    private void onGroupCoadminAddMessage(GroupCoadminAddMessage CoadminAddMsg) {
        logger.info("Got a Coadmin add Message");
        String groupname = CoadminAddMsg.groupname;
        String sourceusername = CoadminAddMsg.sourceusername;
        String targetusername = CoadminAddMsg.targetusername;
        GroupInfo group = groupsMap.get(groupname); //returns null if doesn't exist. we will leave function in validator
        // check all pre-conditions
        if (!ValidateIsGroupExist(groupname, true)) return;
        if (!ValidateIsUserExist(targetusername, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceusername, true)) return;
        // extra pre-conditions
        if (!ValidateIsGroupContainsUser(group, targetusername, true)) return;
        if (ValidateUserHasPriviledges(group, targetusername, false)) return;

        //pre-conditions checked!
        logger.info(targetusername + " will be be added to the "+ groupname +" co-admin list");
        String msg = "You have been promoted to co-admin in " + groupname + "!";
        ActorRef targetActor = usersMap.get(targetusername).getActor();
        ActorRef sourceActor = usersMap.get(sourceusername).getActor();
        group.promoteToCoadmin(targetusername);
        targetActor.tell(new TextMessage(msg), sourceActor);
        logger.info(group.toString());

    }

    private void onGroupCoadminRemoveMessage(GroupCoadminRemoveMessage CoadminRemoveMsg) {
        logger.info("Got a Coadmin remove Message");
        String groupname = CoadminRemoveMsg.groupname;
        String sourceusername = CoadminRemoveMsg.sourceusername;
        String targetusername = CoadminRemoveMsg.targetusername;
        GroupInfo group = groupsMap.get(groupname); //returns null if doesn't exist. we will leave function in validator
        // check all pre-conditions
        if (!ValidateIsGroupExist(groupname, true)) return;
        if (!ValidateIsUserExist(targetusername, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceusername, true)) return;
        // extra pre-conditions
        if (!ValidateIsGroupContainsUser(group, targetusername, true)) return;
        if (ValidateIsUserAdmin(group, targetusername, false)) return;

        //pre-conditions checked!
        logger.info(targetusername + " will be removed from the "+ groupname +" co-admin list");
        String msg = "You have been demoted to user in " + groupname + "!";
        ActorRef targetActor = usersMap.get(targetusername).getActor();
        ActorRef sourceActor = usersMap.get(sourceusername).getActor();
        group.demoteCoadmin(targetusername);
        targetActor.tell(new TextMessage(msg), sourceActor);
        logger.info(group.toString());


    }



    /** Auxiliary methods **/

    private void addUserToGroup(String groupname, String username, GroupInfo group) {
        group.getUsers().add(username);
        group.getGroupRouter().addRoutee(usersMap.get(username).getActor());
        usersMap.get(username).getGroups().add(groupname);
        logger.info("added " + username + " to group " + group.toString());
    }

    private void removeUserFromGroup(String groupname, String username, GroupInfo group) {
        logger.info("removing " + username + " from " + groupname);
        group.removeUsername(username);
        group.getGroupRouter().removeRoutee(usersMap.get(username).getActor());
        usersMap.get(username).getGroups().remove(groupname);
        logger.info("group after remove is: " + group.toString());
    }



//****************************Validators**********************************

    private boolean ValidateIsUserAdmin(GroupInfo group,String username, boolean expectedPrivilege) {
        boolean isAdmin = group.isAdmin(username);
        if(!isAdmin && expectedPrivilege){
            logger.info("No Worries Bro");
        }
        if(isAdmin && !expectedPrivilege){
            logger.info(Constants.GROUP_ACTION_ON_ADMIN(group.getGroupName(),username));
            getSender().tell(new ErrorMessage(Constants.GROUP_ACTION_ON_ADMIN(group.getGroupName(),username)), ActorRef.noSender());
        }

        return isAdmin;
    }

    private boolean ValidateIsGroupContainsUser(GroupInfo group, String username, boolean expectedContained){
        boolean isContained = !group.getUserGroupMode(username).equals(GroupInfo.groupMode.NONE);
        if(isContained && !expectedContained){
            logger.info(Constants.GROUP_TARGET_ALREADY_BELONGS(group.getGroupName(), username));
            getSender().tell(new ErrorMessage(Constants.GROUP_TARGET_ALREADY_BELONGS(group.getGroupName(), username)), ActorRef.noSender());
        }
        if(!isContained && expectedContained){
            logger.info(Constants.GROUP_LEAVE_FAIL(group.getGroupName(), username));
            getSender().tell(new ErrorMessage(Constants.GROUP_LEAVE_FAIL(group.getGroupName(), username)), ActorRef.noSender());
        }
        return isContained;
    }

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

    private boolean ValidateUserHasPriviledges(GroupInfo group,String username, boolean expectedPrivilege) {
        boolean isPrivilege = group.userHasPriviledges(username);
        if(isPrivilege && !expectedPrivilege){
            logger.info(Constants.GROUP_ALREADY_HAVE_PREVILEDGES(group.getGroupName(), username));
            getSender().tell(new ErrorMessage(Constants.GROUP_ALREADY_HAVE_PREVILEDGES(group.getGroupName(), username)), ActorRef.noSender());
        }

        if(!isPrivilege && expectedPrivilege){
            logger.info(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName()));
            getSender().tell(new ErrorMessage(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName())), ActorRef.noSender());
        }
        return isPrivilege;
    }

    private boolean ValidateIsUserExist(String username, boolean expectedExist) {
        boolean isExist = usersMap.containsKey(username);
        logger.info("isExist: " + isExist+ ", username: "+ username+ ", expected: " + expectedExist);
        if(isExist && !expectedExist){
            logger.info(Constants.CONNECT_FAIL(username));
            getSender().tell(new ErrorMessage(Constants.CONNECT_FAIL(username)), ActorRef.noSender());
        }
        if(!isExist && expectedExist){
            logger.info(Constants.NOT_EXIST(username));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(username)), ActorRef.noSender());
        }
        return isExist;
    }


}
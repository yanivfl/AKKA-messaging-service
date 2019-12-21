import Groups.GroupRouter;
import SharedMessages.Messages.*;
import Users.Constants;
import Users.UserInfo;
import Groups.GroupInfo;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.time.Duration;
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
                .match(ConnectionMessage.class, this::onConnect)
                .match(DisconnectMessage.class, this::onDisconnect)
                .match(isUserExistMessage.class, this::onIsUserExist)
                .match(GroupCreateMessage.class,this::onGroupCreate)
                .match(GroupLeaveMessage.class, this::onGroupLeave)
                .match(GroupInviteMessage.class, this::onGroupInvite)
                .match(GroupSendTextMessage.class, this::onGroupSendTextMessage)
                .match(GroupSendFileMessage.class, this::onGroupSendFileMessage)
                .build();
    }

    private void onGroupSendTextMessage(GroupSendTextMessage groupTextMessage){
        logger.info("got a group send text message!");
        if(!ValidateIsGroupExist(groupTextMessage.groupname, true)) return;
        if(!ValidateIsGroupContainsUser(groupsMap.get(groupTextMessage.groupname),
                groupTextMessage.sourcename, true)) return;

        groupsMap.get(groupTextMessage.groupname).
                getGroupRouter().
                broadcastMessage((ActorCell) getContext(), new TextMessage(groupTextMessage.message));
        }

    private void onGroupSendFileMessage(GroupSendFileMessage groupFileMessage){
        logger.info("got a group send file message!");
        if(!ValidateIsGroupExist(groupFileMessage.groupname, true)) return;
        if(!ValidateIsGroupContainsUser(groupsMap.get(groupFileMessage.groupname),
                groupFileMessage.sourcename, true)) return;

        groupsMap.get(groupFileMessage.groupname).
                getGroupRouter().
                broadcastFile((ActorCell) getContext(),
                        new AllBytesFileMessage(groupFileMessage.sourcename, groupFileMessage.fileName,
                                groupFileMessage.groupname, groupFileMessage.buffer));
    }

    private void onConnect(ConnectionMessage connectMsg) {
        logger.info("Got a connection Message");
        if (ValidateIsUserExist(connectMsg.username, false)) return;

        logger.info("Creating new user: " + connectMsg.username);
        usersMap.put(connectMsg.username, new UserInfo(connectMsg.username, new LinkedList<>(), connectMsg.client));
        logger.info("Debug- the users map:\n " + usersMap.toString());
        getSender().tell(new TextMessage(Constants.CONNECT_SUCC(connectMsg.username)), ActorRef.noSender());

    }

    private void onDisconnect(DisconnectMessage disconnectMsg) {
        logger.info("Got a disconnection Message");
        if (!ValidateIsUserExist(disconnectMsg.username, true)) return;

        List<String> userGroupNames = new LinkedList<>(usersMap.get(disconnectMsg.username).getGroups());
        for( String groupName : userGroupNames ){
           if(!onGroupLeave(new GroupLeaveMessage(groupName, disconnectMsg.username))){ return; }
        }
        usersMap.remove(disconnectMsg.username);
        getSender().tell(new TextMessage(Constants.DISCONNECT_SUCC(disconnectMsg.username)), ActorRef.noSender());

        logger.info("disconnecting user: " + disconnectMsg.username);
        logger.info("Debug- the users map:\n " + usersMap.toString());
    }

    private void onIsUserExist(isUserExistMessage msg) {
        logger.info("Got a IsUserExistMessage");
        if (!ValidateIsUserExist(msg.targetusername, true)) return;

        ActorRef targetActor = usersMap.get(msg.targetusername).getActor();
        getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
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
        String groupname = leaveMsg.groupname;
        String username = leaveMsg.username;

        if (!ValidateIsGroupExist(groupname, true)) return false;

        GroupInfo group = groupsMap.get(groupname);
        if (!ValidateIsGroupContainsUser(group, username, true)) return false;

        boolean deleteGroup = false;
        GroupRouter groupRouter = group.getGroupRouter();
        switch (group.getUserGroupMode(username)){
            case ADMIN:
                deleteGroup = true;
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(Constants.GROUP_ADMIN_LEAVE(groupname, username)));

            case CO_ADMIN:
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(Constants.GROUP_COADMIN_LEAVE_SUCC(groupname,username)));

            case MUTED:
            case USER:
                removeUserFromGroup(groupname, username, group);
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(Constants.GROUP_LEAVE_SUCC(groupname, username)));
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

    private void removeUserFromGroup(String groupname, String username, GroupInfo group) {
        logger.info("removing " + username + " from " + groupname);
        group.removeUsername(username, group.getUserGroupMode(username));
        group.getGroupRouter().removeRoutee(usersMap.get(username).getActor());
        usersMap.get(username).getGroups().remove(groupname);
        logger.info("group after remove is: " + group.toString());
    }

    private void onGroupInvite(GroupInviteMessage inviteMsg) {
        logger.info("Got a invite Group Message");
        String groupname = inviteMsg.groupname;
        String sourceusername = inviteMsg.sourceusername;
        String targetusername = inviteMsg.targetusername;
        GroupInfo group = groupsMap.get(groupname); //returns null if doesn't exist. we will leave function in validator
        // check all pre-conditions
        if (!ValidateIsGroupExist(groupname, true)) return;
        if (!ValidateIsGroupContainsUser(group, sourceusername, true)) return;
        if (!ValidateUserHasPriviledges(group, sourceusername, true)) return;
        if (!ValidateIsUserExist(targetusername, true)) return;
        if (ValidateIsGroupContainsUser(group, targetusername, false)) return;

        //pre-conditions checked!
        logger.info("sends invite request to " + targetusername);
        String msg = "You have been invited to " + groupname + ", Accept?";
        ActorRef targetActor = usersMap.get(targetusername).getActor();
        final Timeout timeout = Timeout.create(Duration.ofSeconds(60)); //give user 1 minute to answer
        Future<Object> rt = Patterns.ask(targetActor, new GroupInviteRequestReply(groupname, sourceusername, msg), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result.getClass() == isAcceptInvite.class) {
                if(((isAcceptInvite)result).isAccept){
                    logger.info("adding " + targetusername + " to " + groupname);
                    addUserToGroup(targetusername, group, targetActor);
                    targetActor.tell(new TextMessage("Welcome to " + groupname + "!"), ActorRef.noSender());
                    logger.info("group after invite is: " + group.toString());
                }
                else {
                    logger.info(targetusername + " declined group invitation. after invite is: " + group.toString());
                }
            }
        } catch (Exception e) {
            logger.info("took to much time for user to answer");
            getSender().tell(new ErrorMessage(targetusername + " did not answer on time."), ActorRef.noSender());
        }
    }

    private void addUserToGroup(String targetusername, GroupInfo group, ActorRef targetActor) {
        group.getUsers().add(targetusername);
        group.getGroupRouter().addRoutee(targetActor);
        logger.info("added " + targetusername + " to group " + group.toString() + " with path " + targetActor.path().toString() );
    }


    /** Auxiliary methods **/



//****************************Validators**********************************
    private boolean ValidateIsGroupContainsUser(GroupInfo group, String username, boolean expectedContained){
        boolean isContained = !group.getUserGroupMode(username).equals(GroupInfo.groupMode.NONE);
        if(isContained && !expectedContained){
            logger.info(Constants.GROUP_TARGET_ALREADY_BELONGS(username, group.getGroupName()));
            getSender().tell(new ErrorMessage(Constants.GROUP_TARGET_ALREADY_BELONGS(username, group.getGroupName())), ActorRef.noSender());
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
            logger.info("No Worries Bro");
        }
        if(!isPrivilege && expectedPrivilege){
            logger.info(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName()));
            getSender().tell(new ErrorMessage(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName())), ActorRef.noSender());
        }
        return isPrivilege;
    }

    private boolean ValidateIsUserExist(String username, boolean expectedExist) {
        boolean isExist = usersMap.containsKey(username);
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
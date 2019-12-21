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
                .build();
    }

    private void onConnect(ConnectionMessage connectMsg) { //TODO use validator
        logger.info("Got a connection Message");
        if (!usersMap.containsKey(connectMsg.username)) {
            logger.info("Creating new user: " + connectMsg.username);
            usersMap.put(connectMsg.username,  new UserInfo(connectMsg.username,new LinkedList<String>(), connectMsg.client));
            logger.info("Debug- the users map:\n " + usersMap.toString());
            getSender().tell(new TextMessage(Constants.CONNECT_SUCC(connectMsg.username)), ActorRef.noSender());
        } else {
            logger.info(Constants.CONNECT_FAIL(connectMsg.username));
            getSender().tell(new ErrorMessage(Constants.CONNECT_FAIL(connectMsg.username)), ActorRef.noSender());
        }
    }
    //TODO use validator
    private void onDisconnect(DisconnectMessage disconnectMsg) { //TODO: gracefully leaves all groups
        logger.info("Got a disconnection Message");
        if (usersMap.containsKey(disconnectMsg.username)){
            usersMap.remove(disconnectMsg.username);
            logger.info("disconnecting user: " + disconnectMsg.username);
            logger.info("Debug- the users map:\n " + usersMap.toString());
            getSender().tell(new TextMessage(Constants.DISCONNECT_SUCC(disconnectMsg.username)), ActorRef.noSender());
        }
        else {
            logger.info(Constants.DISCONNECT_FAIL(disconnectMsg.username));
            getSender().tell(new ErrorMessage(Constants.DISCONNECT_FAIL(disconnectMsg.username)), ActorRef.noSender());
        }
    }

    private void onIsUserExist(isUserExistMessage msg) { //TODO use validator
        logger.info("Got a IsUserExistMessage");
        if(usersMap.containsKey(msg.targetusername)) {
            ActorRef targetActor = usersMap.get(msg.targetusername).getActor();
            getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
        }
        else {
            logger.info(Constants.NOT_EXIST(msg.targetusername));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(msg.targetusername)), ActorRef.noSender());
        }
    }

    private void onGroupCreate(GroupCreateMessage createMsg) { //TODO use validators
        logger.info("Got a Create Group Message");
        if (!groupsMap.containsKey(createMsg.groupname)) {
            logger.info("Creating new group: " + createMsg.groupname);
            groupsMap.put(createMsg.groupname,  new GroupInfo(createMsg.groupname, createMsg.username, getSender()));
            usersMap.get(createMsg.username).getGroups().add(createMsg.groupname);
            logger.info("Debug- the usersgroups map:\n " + groupsMap.toString());
            getSender().tell(new TextMessage(Constants.GROUP_CREATE_SUCC(createMsg.groupname)), ActorRef.noSender());
            logger.info("admin path is::\n " +  getSender().path().toString());
        } else {
            logger.info(Constants.GROUP_CREATE_FAIL(createMsg.groupname));
            getSender().tell(new ErrorMessage(Constants.GROUP_CREATE_FAIL(createMsg.groupname)), ActorRef.noSender());
        }
    }



    private void onGroupLeave(GroupLeaveMessage leaveMsg) {
        logger.info("Got a Leave Group Message");
        String groupname = leaveMsg.groupname;
        String username = leaveMsg.username;

        if (!ValidateIsGroupExist(groupname, true)) return;

        GroupInfo group = groupsMap.get(groupname);
        if (!ValidateIsGroupContainsUser(group, username, true)) return;

        boolean deleteGroup = false;
        GroupRouter groupRouter = group.getGroupRouter();
        switch (group.getUserGroupMode(username)){
            case ADMIN:
                deleteGroup = true;
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(username +" admin has closed " + groupname +"!"));

            case CO_ADMIN:
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(username +" is removed from co-admin list in " + groupname));

            case MUTED:
            case USER:
                removeUserFromGroup(groupname, username, group);
                groupRouter.broadcastMessage((ActorCell) getContext(), new TextMessage(username +" has left " + groupname + "!"));
                break;
            case NONE:
                logger.info("DEBUG - Manager should not Reach this point!");
                return;
        }
        if(deleteGroup){
            for(String userName : group.getAllUsers()){ //TODO remove total users outside the scope. will throw exception
                removeUserFromGroup(groupname, userName, group);
            }
            groupsMap.remove(groupname);
        }
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

        /**
         * if actor doesn't exist, return null
         * @param actorRef
         * @return
         **/
    private UserInfo getUserInfo(ActorRef actorRef){

        for (ConcurrentMap.Entry<String, UserInfo> entry : usersMap.entrySet()) {
            if (entry.getValue().getActor().equals(actorRef))
                return entry.getValue();
        }
        return null;
    }


    private String getUserName(UserInfo userInfo) { //checked! works
        return usersMap
            .entrySet()
            .stream()
            .filter(entry -> userInfo.equals(entry.getValue()))
            .map(ConcurrentMap.Entry::getKey)
            .findFirst()
            .get();
    }




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
            logger.info(Constants.NOT_EXIST(username));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(username)), ActorRef.noSender());
        }
        if(!isExist && expectedExist){
            logger.info(Constants.CONNECT_FAIL(username));
            getSender().tell(new ErrorMessage(Constants.CONNECT_FAIL(username)), ActorRef.noSender());
        }
        return isExist;
    }


}
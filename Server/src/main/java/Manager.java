import SharedMessages.Messages.*;
import Users.Constants;
import Users.UserInfo;
import Groups.GroupInfo;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
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
                .match(isUserExistMessage.class, this::isUserExist)
                .match(GroupCreateMessage.class,this::onGroupCreate)
                .match(GroupLeaveMessage.class, this::onGroupLeave)
                .build();
    }

    private void onConnect(ConnectionMessage connectMsg) {
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

    private void isUserExist(isUserExistMessage msg) {
        logger.info("Got a IsUserExitsMessage");
        if(usersMap.containsKey(msg.targetusername)) {
            ActorRef targetActor = usersMap.get(msg.targetusername).getActor();
            getSender().tell(new AddressMessage(targetActor), ActorRef.noSender());
        }
        else {
            logger.info(Constants.NOT_EXIST(msg.targetusername));
            getSender().tell(new ErrorMessage(Constants.NOT_EXIST(msg.targetusername)), ActorRef.noSender());
        }
    }

    private void onGroupCreate(GroupCreateMessage createMsg) {
        logger.info("Got a Create Group Message");
        if (!groupsMap.containsKey(createMsg.groupname)) {
            logger.info("Creating new group: " + createMsg.groupname);
            groupsMap.put(createMsg.groupname,  new GroupInfo(createMsg.groupname, createMsg.username));
            usersMap.get(createMsg.username).getGroups().add(createMsg.groupname);
            logger.info("Debug- the usersgroups map:\n " + groupsMap.toString());
            getSender().tell(new TextMessage(Constants.GROUP_CREATE_SUCC(createMsg.groupname)), ActorRef.noSender());
        } else {
            logger.info(Constants.GROUP_CREATE_FAIL(createMsg.groupname));
            getSender().tell(new ErrorMessage(Constants.GROUP_CREATE_FAIL(createMsg.groupname)), ActorRef.noSender());
        }
    }

    private void onGroupLeave(GroupLeaveMessage leaveMsg) {
        logger.info("Got a Leave Group Message");
        String groupname = leaveMsg.groupname;
        String username = leaveMsg.username;

        if (!isGroupExist(groupname)) return;
        GroupInfo group = groupsMap.get(groupname);
        if (isGroupContainsUser(group, username)) return;

    }

    private void onGroupInvite(GroupInviteMessage inviteMsg) {
        logger.info("Got a invite Group Message");
        String groupname = inviteMsg.groupname;
        String sourceusername = inviteMsg.sourceusername;
        String targetusername = inviteMsg.targetusername;

        // check all pre-conditions
        if (!isGroupExist(groupname)) return;
        GroupInfo group = groupsMap.get(groupname);
        if (!userHasPriviledges(group, sourceusername)) return;
        if (!isUserExist(targetusername)) return;
        if (isGroupContainsUser(group, sourceusername)) return;

        //pre-conditions checked!
        logger.info("sends invite request to " + targetusername);
        String msg = "You have been invited to " + groupname + ", Accept?";
        final Timeout timeout = Timeout.create(Duration.ofSeconds(1));
        Future<Object> rt = Patterns.ask(getSender(), new GroupInviteRequestReply(groupname, sourceusername, msg), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result.toString() == "yes")
                logger.info("adding " + targetusername + " to " + groupname);
                group.getUsers().add(targetusername);
                getSender().tell(new TextMessage("Welcome to" + groupname + "!"), ActorRef.noSender());

        } catch (Exception e) {
            logger.info(Constants.SERVER_IS_OFFLINE_DISCONN);
            getSender().tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }


    /** Auxiliary methods **/

    private boolean isGroupExist(String groupname){
        if (groupsMap.containsKey(groupname)) { return true; }

        logger.info(Constants.NOT_EXIST(groupname));
        getSender().tell(new ErrorMessage(Constants.NOT_EXIST(groupname)), ActorRef.noSender());
        return false;
    }

    private boolean userHasPriviledges(GroupInfo group,String username) {
        if (group.userHasPriviledges(username)) { return true; }

        logger.info(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName()));
        getSender().tell(new ErrorMessage(Constants.GROUP_NOT_HAVE_PREVILEDGES(group.getGroupName())), ActorRef.noSender());
        return false;
    }

    private boolean isUserExist(String username) {
        if(usersMap.containsKey(username)) { return true; }

        logger.info(Constants.NOT_EXIST(username));
        getSender().tell(new ErrorMessage(Constants.NOT_EXIST(username)), ActorRef.noSender());
        return false;
    }

    private boolean isGroupContainsUser(GroupInfo group, String username) {
        if(!group.contains(username)) { return false; }

        logger.info(Constants.GROUP_TARGET_ALREADY_BELONGS(username, group.getGroupName()));
        getSender().tell(new ErrorMessage(Constants.GROUP_TARGET_ALREADY_BELONGS(username, group.getGroupName())), ActorRef.noSender());
        return true;
    }

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
    //        return  usersMap
////                .entrySet()
////                .stream()
////                .filter(entry -> actorRef.equals(entry.getValue().getActor()))
////                .map(ConcurrentMap.Entry::getValue)
////                .findFirst()
////                .get();

    private String getUserName(UserInfo userInfo) { //checked! works
        return usersMap
            .entrySet()
            .stream()
            .filter(entry -> userInfo.equals(entry.getValue()))
            .map(ConcurrentMap.Entry::getKey)
            .findFirst()
            .get();
    }
}
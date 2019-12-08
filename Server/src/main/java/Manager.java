import SharedMessages.Messages.*;
import Users.Constants;
import Users.UserInfo;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.dsl.Creators;
import akka.event.Logging;
import akka.event.LoggingAdapter;


import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class Manager extends AbstractActor {

    private LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);
    private ConcurrentMap<String, UserInfo> usersMap = new ConcurrentHashMap<>();


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectionMessage.class, this::onConnect)
                .match(DisconnectMessage.class, this::onDisconnect)
                .build();
    }

    private void onConnect(ConnectionMessage connectMsg) {
        logger.info("got a connection Message");
        if (usersMap.get(connectMsg.username)== null) { //returns null if new user
            logger.info("Creating new user");
            usersMap.put(connectMsg.username, new UserInfo(false, new LinkedList<String>(), getSender()));
            getSender().tell(new TextMessage(Constants.CONNECT_SUCC(connectMsg.username)), ActorRef.noSender());
        } else {
            logger.info("User Already exists");
            getSender().tell(new ErrorMessage(Constants.CONNECT_FAIL(connectMsg.username)), ActorRef.noSender());
        }
    }

    private void onDisconnect(DisconnectMessage disconnectMsg) {
        logger.info("got a disconnection Message");
        UserInfo userInfo = getUserInfo(getSender());
        if(userInfo!=null){
            //TODO erase client from usersmap and groups and return success message
        }

    }

    /***
     * if actor doesn't exist, return null
     * @param actorRef
     * @return
     */
    private UserInfo getUserInfo(ActorRef actorRef){
        return  (UserInfo) usersMap.entrySet().stream()
                .filter(map -> actorRef.equals(map.getValue().getActor())) //TODO is this how to check if actors are equal
                .map(map -> map.getValue())
                .collect(Collectors.toList()).get(0);
    }


}
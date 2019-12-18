import SharedMessages.Messages.*;
import Users.Constants;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;


public class ClientActor extends AbstractActor {
    private String clientUserName = "";
    private final ActorRef client = this.self();
    public final ActorSelection manager = getContext().actorSelection(Constants.PATH_TO_MANAGER);
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TextMessage.class, this::onTextMessage)
                .match(ErrorMessage.class, this::onErrorMessage)
                .match(GroupInviteRequestReply.class, this::onInviteRequest)
                .build();
    }

    private void onTextMessage(TextMessage textMsg) {
        System.out.println(textMsg.text);
    }

    private void onErrorMessage(ErrorMessage errorMsg) {
        logger.info(errorMsg.error); //TODO: delete
        System.out.println(errorMsg.error);
    }

    private void onInviteRequest(GroupInviteRequestReply reqMsg) {
        logger.info(reqMsg.text);
    }
}


//    @Override
//    public void preStart() throws Exception { // this is the first step once the Actor is added to the system.
////        System.out.println(conf.getString("akka.actor.debug.lifecycle"));
////        String path = conf.getString("akka.remote.netty.tcp.prefix") +
////                Constants.SERVER + "@" +
////                conf.getString("akka.remote.netty.tcp.hostname") + ":" +
////                3553 + "/user/" + Constants.MANAGER;
////        logger.info("Path is: " + path);
//}
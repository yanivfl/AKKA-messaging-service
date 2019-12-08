import SharedMessages.Messages.*;
import Users.Constants;
import Users.UserInfo;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.LinkedList;

public class ClientActor extends AbstractActor {
    private final Config conf = ConfigFactory.load();
    private final ActorSelection manager =getContext().actorSelection(Constants.PATH_TO_MANAGER);
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TextMessage.class, this::onTextMessage)
                .match(ErrorMessage.class, this:: onErrorMessage)
                .build();
    }


    private void onTextMessage(TextMessage textMsg) {
        logger.info("got a text Message from Manager");
        System.out.println(textMsg);
    }

    private void onErrorMessage(ErrorMessage errorMsg) {
        logger.info("got an error from Manager");
        System.out.println(errorMsg);
    }



    @Override
    public void preStart() { // this is the first step once the Actor is added to the system.
        System.out.println(conf.getString("akka.actor.debug.lifecycle"));
        String path = conf.getString("akka.remote.netty.tcp.prefix") +
                Constants.SERVER+ "@" +
                conf.getString("akka.remote.netty.tcp.hostname") + ":" +
                3553 +"/user/" + Constants.MANAGER;
        logger.info("Path is: " + path);
        manager.tell(new ConnectionMessage("yaniv"), self());
    }
}
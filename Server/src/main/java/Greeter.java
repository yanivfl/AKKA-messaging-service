import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import SharedMessages.Message;

public class Greeter extends AbstractActor {

    private LoggingAdapter logger = Logging.getLogger(getContext().getSystem(), this);


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(Message.GREET, m -> {
                    System.out.println("Hello World!");
                    logger.info("Hello World!!!!");
                    sender().tell(Message.DONE, self());
                })
                .matchEquals(Message.DONE, m -> {
                    System.out.println("Client Disconnected!");
                    logger.info("Client Disconnected!");
                })
                .build();
    }
}
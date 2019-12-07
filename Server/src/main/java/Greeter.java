import akka.actor.AbstractActor;
import scala.Serializable;


enum Message implements Serializable {
    GREET, DONE;
}

public class Greeter extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(Message.GREET, m -> {
                    System.out.println("Hello World!");
                    sender().tell(Message.DONE, self());
                })
                .matchEquals(Message.DONE, m -> {
                    System.out.println("Client Disconnected!");
                })
                .build();
    }
}
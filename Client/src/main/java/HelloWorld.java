import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.Serializable;

enum Message implements Serializable {
    GREET, DONE;
}

public class HelloWorld extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(Message.DONE, m -> {
                    // when the greeter is done, stop this actor and with it the application
                    sender().tell(Message.DONE, self());
                    getContext().stop(self());
                })
                .build();
    }
    @Override
    public void preStart() { // this is the first step once the Actor is added to the system.
        System.out.println("in pre start!!!");
        ActorSelection greeter =getContext().actorSelection("akka.tcp://AkkaRemoteServer@127.0.0.1:2552/user/CalculatorActor");
        greeter.tell(Message.GREET, self());
    }
}

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

class Client {
    public static void main(String[] args) {
        // Creating environment
        ActorSystem system = ActorSystem.create("AkkaRemoteClient", ConfigFactory.load());

        // Client actor
        ActorRef client = system.actorOf(Props.create(HelloWorld.class));

    }
}

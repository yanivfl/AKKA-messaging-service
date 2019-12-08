
import Users.Constants;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

class ClientMain {
    public static void main(String[] args) {
        // Creating environment
        ActorSystem system = ActorSystem.create(Constants.CLIENT, ConfigFactory.load());

        // Client actor
        ActorRef client = system.actorOf(Props.create(ClientActor.class));

    }
}

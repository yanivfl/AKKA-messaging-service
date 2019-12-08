import Users.Constants;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainServer {

    public static void main(String[] args) {
        // Creating environment
        ActorSystem system = ActorSystem.create(Constants.SERVER, ConfigFactory.load());

        // Create an actor
        system.actorOf(Props.create(Manager.class), Constants.MANAGER);

//        System.out.println("ENTER to terminate");
//        try(BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
//            in.readLine();
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        } finally {
//            system.terminate();
//        }


    }

}
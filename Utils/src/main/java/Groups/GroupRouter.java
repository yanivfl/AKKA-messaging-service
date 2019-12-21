package Groups;

import SharedMessages.Messages;
import akka.actor.ActorCell;
import akka.actor.ActorRef;
import akka.routing.*;
import com.fasterxml.uuid.Generators;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class GroupRouter {

    private List<String> paths = new LinkedList<>();


    public GroupRouter(){
//        router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    public void addRoutee(ActorRef routee){
        paths.add(routee.path().toString());
    }

    public void removeRoutee(ActorRef routee){
        paths.remove(routee.path().toString());
    }

    public void broadcastMessage(ActorCell context, Messages.TextMessage msg ){
        UUID uuid = Generators.timeBasedGenerator().generate();
        ActorRef broadcastRouter = context.actorOf(new ConsistentHashingGroup(paths).props(), "BroadcastingRouter"+ uuid);
        broadcastRouter.tell(new Broadcast(msg), ActorRef.noSender());
    }

    public void broadcastFile(ActorCell context, Messages.AllBytesFileMessage msg ){
        UUID uuid = Generators.timeBasedGenerator().generate();
        ActorRef broadcastRouter = context.actorOf(new ConsistentHashingGroup(paths).props(), "BroadcastingRouter"+ uuid);
        broadcastRouter.tell(new Broadcast(msg), ActorRef.noSender());
    }


    @Override
    public String toString() {
        return "GroupRouter{" +
                "paths=" + paths +
                '}';
    }
}

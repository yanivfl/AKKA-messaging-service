package Groups;

import SharedMessages.Messages;
import akka.actor.ActorCell;
import akka.actor.ActorRef;
import akka.routing.*;

import java.util.LinkedList;
import java.util.List;

public class GroupRouter {

    private List<String> paths = new LinkedList<>();
    private long uniqueID = 0;


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
        ActorRef broadcastRouter = context.actorOf(new ConsistentHashingGroup(paths).props(), "BroadcastingRouter"+ uniqueID++);
        broadcastRouter.tell(new Broadcast(msg), ActorRef.noSender());
    }


    @Override
    public String toString() {
        return "GroupRouter{" +
                "paths=" + paths +
                '}';
    }
}

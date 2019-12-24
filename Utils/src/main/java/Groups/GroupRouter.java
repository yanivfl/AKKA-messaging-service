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

    public void addRoutee(ActorRef routee){
        paths.add(routee.path().toString());
    }

    public void removeRoutee(ActorRef routee){
        paths.remove(routee.path().toString());
    }

    /**
     * removes source user from router so he won't get broadcasted message.
     * creates unique id for clientref
     * create broadcast router from paths in group
     * add back user
     * @param context
     * @param sourceRoutee
     * @return broadcast router as clientRef
     */
    public ActorRef getBroadcastRouter(ActorCell context, ActorRef sourceRoutee ){
        removeRoutee(sourceRoutee);
        UUID uuid = Generators.timeBasedGenerator().generate();
        ActorRef broadcastRouter = context.actorOf(new ConsistentHashingGroup(paths).props(), "BroadcastingRouter"+ uuid);
        addRoutee(sourceRoutee);
        return broadcastRouter;
    }




    @Override
    public String toString() {
        return "GroupRouter{" +
                "paths=" + paths +
                '}';
    }
}

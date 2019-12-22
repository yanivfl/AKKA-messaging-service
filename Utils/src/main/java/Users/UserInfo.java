package Users;

import akka.actor.ActorRef;

import java.util.List;

public class UserInfo{
    private List<String> groups;
    private ActorRef actor;

    public UserInfo(List<String> groups, ActorRef actor) {
        this.groups = groups;
        this.actor = actor;
    }

    public List<String> getGroups() {
        return groups;
    }

    public ActorRef getActor() {
        return actor;
    }

    public void setActor(ActorRef actor) {
        this.actor = actor;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                ", groups=" + groups +
                ", actor=" + actor +
                '}';
    }
}
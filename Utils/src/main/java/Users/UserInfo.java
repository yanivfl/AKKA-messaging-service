package Users;

import akka.actor.ActorRef;

import java.util.List;

public class UserInfo{
    private boolean isAdmin;
    private List<String> groups;
    ActorRef actor;

    public UserInfo(boolean isAdmin, List<String> groups, ActorRef actor) {
        this.isAdmin = isAdmin;
        this.groups = groups;
        this.actor = actor;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
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
                "isAdmin=" + isAdmin +
                ", groups=" + groups +
                ", actor=" + actor +
                '}';
    }
}
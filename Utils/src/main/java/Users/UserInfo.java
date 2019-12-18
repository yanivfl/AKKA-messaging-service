package Users;

import akka.actor.ActorRef;

import java.util.List;

public class UserInfo{
    private String userName;
    private boolean isAdmin;
    private List<String> groups;
    private ActorRef actor;

    public UserInfo(String userName, List<String> groups, ActorRef actor) {
        this.userName = userName;
        this.groups = groups;
        this.actor = actor;
    }

    public String getUserName() {
        return userName;
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
package Groups;


import akka.actor.ActorRef;

import java.util.LinkedList;
import java.util.List;



public class GroupInfo {
    private String groupName;
    private String admin;
    private List<String> coAdmins = new LinkedList<>();
    private List<String> mutedusers= new LinkedList<>();
    private List<String> users= new LinkedList<>();
    private List<ActorRef> router = new LinkedList<>();
    private List<String> totalUsers = new LinkedList<>();

    public GroupInfo(String groupName, String admin, ActorRef adminActor) {
        this.groupName = groupName;
        this.admin = admin;
        this.router.add(adminActor);
        this.totalUsers.add(admin);
    }

    public enum groupMode {
        ADMIN, CO_ADMIN, MUTED, USER, NONE;
    }

    public String getGroupName() { return groupName; }

    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAdmin() { return admin; }

    public List<String> getCoAdmins() { return coAdmins; }

    public List<String> getMuteds() { return mutedusers; }

    public List<String> getUsers() { return users; }

    public List<String> getTotalUsers() { return totalUsers; }

    public List<ActorRef> getRouter() { return router; }
    public boolean addRoutee(ActorRef routee) { return router.add(routee); }
    public boolean removeRoutee(ActorRef routee) { return router.remove(routee); }

    public boolean userHasPriviledges(String username){
        return admin.equals(username) || coAdmins.contains(username);
    }

    public groupMode getUserGroupMode(String username){
        return  admin.equals(username)? groupMode.ADMIN:
                coAdmins.contains(username)? groupMode.CO_ADMIN:
                mutedusers.contains(username)? groupMode.MUTED:
                users.contains(username)? groupMode.USER:
                groupMode.NONE;
    }

    private boolean removeAdmin(String username){
        if(admin.equals(username)){
            this.admin = "";
            return true;
        }
        return false;
    }

    public boolean removeUsername(String username, groupMode type){
        return  type.equals(groupMode.CO_ADMIN)? coAdmins.remove(username) :
                type.equals(groupMode.MUTED)? mutedusers.remove(username) :
                type.equals(groupMode.USER)? users.remove(username) :
                type.equals(groupMode.ADMIN) && removeAdmin(username);

    }

    @Override
    public String toString() {
        return "GroupInfo{" +
                "groupName='" + groupName + '\'' +
                ", admin='" + admin + '\'' +
                ", coAdmins=" + coAdmins +
                ", mutedusers=" + mutedusers +
                ", users=" + users +
                ", router=" + router +
                ", totalUsers=" + totalUsers +
                '}';
    }
}

//    List<String> paths = Arrays.asList("/user/workers/w1", "/user/workers/w2", "/user/workers/w3");
//    ActorRef router4 = getContext().actorOf(new RoundRobinGroup(paths).props(), "router4");


package Groups;


import akka.routing.Router;

import java.util.List;

public class GroupInfo {
    private String groupName;
    private String admin;
    private List<String> coAdmins;
    private List<String> muteds;
    private List<String> users;
    private List<String> userPaths;


    public GroupInfo(String groupName, String admin) {
        this.groupName = groupName;
        this.admin = admin;
    }

    public String getGroupName() { return groupName; }

    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAdmin() { return admin; }

    public List<String> getCoAdmins() { return coAdmins; }

    public List<String> getMuteds() { return muteds; }

    public List<String> getUsers() { return users; }

    public List<String> getUserPaths() { return userPaths; }

    public boolean userHasPriviledges(String username){
        if (admin == username || coAdmins.contains(username))
            return true;
        return false;
    }

    public boolean contains(String username){
        if (admin == username || coAdmins.contains(username) || muteds.contains(username) || users.contains(username))
            return true;
        return false;
    }

    @Override
    public String toString() {
        return "GroupInfo{" +
                "groupName='" + groupName + '\'' +
                ", admin='" + admin + '\'' +
                ", coAdmins=" + coAdmins +
                ", muteds=" + muteds +
                ", users=" + users +
                '}';
    }
}

//    List<String> paths = Arrays.asList("/user/workers/w1", "/user/workers/w2", "/user/workers/w3");
//    ActorRef router4 = getContext().actorOf(new RoundRobinGroup(paths).props(), "router4");


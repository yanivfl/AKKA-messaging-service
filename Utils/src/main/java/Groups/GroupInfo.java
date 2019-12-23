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
    private GroupRouter groupRouter;

    public GroupInfo(String groupName, String admin, ActorRef adminActor) {
        this.groupName = groupName;
        this.admin = admin;
        this.groupRouter = new GroupRouter();
        groupRouter.addRoutee(adminActor);
    }

    public enum groupMode {
        ADMIN, CO_ADMIN, MUTED, USER, NONE;
    }

    public String getGroupName() { return groupName; }

    public boolean isAdmin(String username) { return admin.equals(username); }

    public boolean isMuted(String username) { return mutedusers.contains(username); }

    public List<String> getMuteds() { return mutedusers; }

    public List<String> getUsers() { return users; }

    public GroupRouter getGroupRouter() { return groupRouter; }

    public List<String> getAllUsers() {
        List<String> allUsers = new LinkedList<>();
        if(!admin.equals(""))
            allUsers.add(admin);
        allUsers.addAll(coAdmins);
        allUsers.addAll(mutedusers);
        allUsers.addAll(users);
        return allUsers;
    }

    public boolean userHasPrivileges(String username){
        return admin.equals(username) || coAdmins.contains(username);
    }

    public groupMode getUserGroupMode(String username){
        return  admin.equals(username)? groupMode.ADMIN:
                coAdmins.contains(username)? groupMode.CO_ADMIN:
                mutedusers.contains(username)? groupMode.MUTED:
                users.contains(username)? groupMode.USER:
                groupMode.NONE;
    }

    public void muteUser(String username){
        if (coAdmins.contains(username)) {coAdmins.remove(username);}
        if (users.contains(username)) {users.remove(username);}
        mutedusers.add(username);
    }

    public void unMuteUser(String username){
        mutedusers.remove(username);
        users.add(username);
    }

    public void promoteToCoAdmin(String username){
        if (mutedusers.contains(username)) {mutedusers.remove(username);}
        if (users.contains(username)) {users.remove(username);}
        coAdmins.add(username);
    }
    public void demoteCoAdmin(String username){
        users.add(username);
        coAdmins.remove(username);
    }

    private boolean removeAdmin(String username){
        if(admin.equals(username)){
            this.admin = "";
            return true;
        }
        return false;
    }

    public boolean removeUsername(String username){
        groupMode type = getUserGroupMode(username);
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
                ", groupRouter=" + groupRouter +
                '}';
    }
}


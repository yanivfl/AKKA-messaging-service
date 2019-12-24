package Groups;

import akka.actor.ActorRef;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    public List<String> getUsers() { return users; }

    public GroupRouter getGroupRouter() { return groupRouter; }


    private List<String> concatList(List<String> list1, List<String> list2){
        return Stream.concat(list1.stream(), list2.stream())
                .collect(Collectors.toList());
    }

    public List<String> getAllUsers() {
        List<String> adminList = admin.equals("")?
                Collections.emptyList() :
                Collections.singletonList(admin);
        return concatList(
                concatList(adminList, coAdmins),
                concatList(mutedusers,users)
        );
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

    public void muteUser(String username, ActorRef targetActor){
        getGroupRouter().removeRoutee(targetActor); //for not getting broadcast
        removeUsername(username);
        mutedusers.add(username);
    }
    public void unMuteUser(String username, ActorRef targetActor){
        mutedusers.remove(username);
        getGroupRouter().addRoutee(targetActor); // for getting broadcast
        users.add(username);
    }
    public void promoteToCoAdmin(String username, ActorRef user){
        if(mutedusers.contains(username))
            unMuteUser(username, user);

        removeUsername(username);
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


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
        ADMIN, CO_ADMIN, MUTED, USER, NONE
    }

    public String getGroupName() { return groupName; }

    public boolean isAdmin(String username) { return admin.equals(username); }

    public boolean isMuted(String username) { return mutedusers.contains(username); }

    public List<String> getUsers() { return users; }

    public GroupRouter getGroupRouter() { return groupRouter; }

    /**
     * concats 2 lists
     * @param list1
     * @param list2
     * @return new list
     */
    private List<String> concatList(List<String> list1, List<String> list2){
        return Stream.concat(list1.stream(), list2.stream())
                .collect(Collectors.toList());
    }

    /**
     * gets all the users in this group
     * @return list<String>, users in group
     */
    public List<String> getAllUsers() {
        List<String> adminList = admin.equals("")?
                Collections.emptyList() :
                Collections.singletonList(admin);
        return concatList(
                concatList(adminList, coAdmins),
                concatList(mutedusers,users)
        );
    }

    /**
     *
     * @param username
     * @return true iff user has privilege
     */
    public boolean userHasPrivileges(String username){
        return admin.equals(username) || coAdmins.contains(username);
    }

    /**
     * get user mode
     * @param username
     * @return user mode
     */
    public groupMode getUserGroupMode(String username){
        return  admin.equals(username)? groupMode.ADMIN:
                coAdmins.contains(username)? groupMode.CO_ADMIN:
                mutedusers.contains(username)? groupMode.MUTED:
                users.contains(username)? groupMode.USER:
                groupMode.NONE;
    }

    /**
     * mute user
     * @param username
     * @param targetActor
     */
    public void muteUser(String username, ActorRef targetActor){
        getGroupRouter().removeRoutee(targetActor); //for not getting broadcast
        removeUsername(username);
        mutedusers.add(username);
    }
    /**
     * un mute user
     * @param username
     * @param targetActor
     */
    public void unMuteUser(String username, ActorRef targetActor){
        mutedusers.remove(username);
        getGroupRouter().addRoutee(targetActor); // for getting broadcast
        users.add(username);
    }

    /**
     * promote user/muted user to coadmin
     * @param username
     * @param user
     */
    public void promoteToCoAdmin(String username, ActorRef user){
        if(mutedusers.contains(username))
            unMuteUser(username, user);

        removeUsername(username);
        coAdmins.add(username);
    }

    /**
     * move user to user
     * @param username
     */
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

    /**
     * remove user from list he is contained in
     * @param username
     * @return
     */
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


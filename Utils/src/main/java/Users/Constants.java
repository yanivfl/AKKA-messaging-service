package Users;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants{
    public static final int BUFFER_SIZE = 4096; // 4KB=BUFFER_SIZE
    public static final String ACTION_USER = "user";
    public static final String CHAT_DOWNLOADS_DIR = "Chat_Downloads";
    public static final String MANAGER = "Manager";
    public static final String SERVER= "WhatsupServer";
    public static final String CLIENT= "Client";
    public static final String CLIENT_SYSTEM = "Client-System";
    public static final String PATH_TO_MANAGER = "akka.tcp://"+ SERVER + "@127.0.0.1:3553/user/"+MANAGER;


    public static final String SERVER_RESPONSE =  "user log in to server";
    public static final String SERVER_IS_OFFLINE_CONN =  "server is offline!";
    public static final String SERVER_IS_OFFLINE_DISCONN =  "server is offline! try again later!";
    public static String CONNECT_FAIL(String username){return username + " is in use!";}
    public static String CONNECT_SUCC(String username){return username + " has connected successfully!";}
    public static String DISCONNECT_FAIL(String username){return username + " already disconnected!";}
    public static String DISCONNECT_SUCC(String username){return username + " has been disconnected successfully!";}
    public static String NOT_EXIST(String notexits){return notexits + " does not exist!";}




    public static String GROUP_CREATE_SUCC(String groupname){return groupname + " created successfully!";}
    public static String GROUP_CREATE_FAIL(String groupname){return groupname + " already exists!";}
    public static String GROUP_LEAVE_FAIL(String groupname, String sourceusername){return sourceusername + " is not in " + groupname + "!";}
    public static String GROUP_COADMIN_LEAVE_SUCC(String groupname, String sourceusername){return sourceusername +" is removed from co-admin list in " + groupname;}
    public static String GROUP_LEAVE_SUCC(String groupname, String sourceusername){return sourceusername + " has left " + groupname + "!";}
    public static String GROUP_ADMIN_LEAVE(String groupname, String sourceusername){return sourceusername + " admin has closed " + groupname + "!";}
    public static String GROUP_MUTE(String groupname, String time){return "You are muted for" + time + " in " + groupname + "!";}
    public static String GROUP_NOT_BELONG(String groupname){return "You are not part of " + groupname + "!";}
    public static String GROUP_NOT_HAVE_PREVILEDGES(String groupname){return "You are neither an admin nor a co-admin of " + groupname + "!";}
    public static String GROUP_ALREADY_HAVE_PREVILEDGES(String groupname, String username){return username+ " already have an admin or a co-admin privileges in " + groupname + "!";}
    public static String GROUP_TARGET_ALREADY_BELONGS(String targetusername, String groupname){return targetusername+ " is already in " + groupname + "!";}
    public static String GROUP_RESPOND_TO_SOURCE(String username, String answer){return username+ " responded to group invite with the following answer: " + answer;}
    public static String GROUP_INVITE_PROMPT(String groupName){ return "You have been invited to " + groupName + ", Accept?";}
    public static String GROUP_WELCOME_PROMPT(String groupName){ return "Welcome to " + groupName + "!";}
    public static String GROUP_REMOVE_PROMPT(String groupName, String sourceusername){ return "You have been removed from "+ groupName + " by " + sourceusername+ "!";}



    public static String GROUP_ACTION_ON_ADMIN(String groupname, String sourceusername){return " action denied because " + sourceusername+ " is admin in " + groupname;}
    public static String PRINTING(String action, String sourcename, String message){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        return "["+ formatter.format(date) +"] [" + action + "] [" + sourcename + "] " + message;
    }

}


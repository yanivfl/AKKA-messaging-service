package Users;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants{
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";

    public static final int MINUTE = 60;
    public static final String ACTION_USER = "user";
    public static final String CHAT_DOWNLOADS_DIR = "Chat_Downloads";
    public static final String MANAGER = "Manager";
    public static final String SERVER= "WhatsupServer";
    public static final String CLIENT= "Client";
    public static final String CLIENT_SYSTEM = "Client-System";
    public static final String PATH_TO_MANAGER = "akka.tcp://"+ SERVER + "@127.0.0.1:3553/user/"+MANAGER;


    public static final String SERVER_IS_OFFLINE_CONN =  "server is offline!";
    public static final String SERVER_IS_OFFLINE_DISCONN =  "server is offline! try again later!";
    public static String CONNECT_FAIL(String username){return username + " is in use!";}
    public static String CONNECT_SUCC(String username){return username + " has connected successfully!";}
    public static String ALREADY_CONNECT=" you are connected!";
    public static String DISCONNECT_SUCC(String username){return username + " has been disconnected successfully!";}
    public static String NOT_EXIST(String notExits){return notExits + " does not exist!";}



    public static String GROUP_CREATE_SUCC(String groupName){return groupName + " created successfully!";}
    public static String GROUP_CREATE_FAIL(String groupName){return groupName + " already exists!";}
    public static String GROUP_LEAVE_FAIL(String groupName, String sourceUserName){return sourceUserName + " is not in " + groupName + "!";}
    public static String GROUP_COADMIN_LEAVE_SUCC(String groupName, String sourceUserName){return sourceUserName +" is removed from co-admin list in " + groupName;}
    public static String GROUP_COADMIN_ADD_ERROR(String groupName, String userName){return userName +" is already co-admin in " + groupName;}
    public static String GROUP_COADMIN_REMOVE_ERROR(String groupName, String userName){return userName +" is not co-admin in " + groupName;}
    public static String GROUP_LEAVE_SUCC(String groupName, String sourceUserName){return sourceUserName + " has left " + groupName + "!";}
    public static String GROUP_ADMIN_LEAVE(String groupName, String sourceUserName){return sourceUserName + " admin has closed " + groupName + "!";}
    public static String GROUP_MUTE(String groupName, String time,String sourceName){return "You have been muted for " + time + " seconds in " + groupName + " by "+ sourceName +"!";}
    public static String GROUP_MUTED_ERROR = "action failed, you are muted!";
    public static String GROUP_UN_MUTE_ERROR(String targetUserName){return targetUserName+" is not muted!";}
    public static String GROUP_UN_MUTE_AUTO = "You have been unmuted! Muting time is up!";
    public static String GROUP_UN_MUTE(String groupName, String sourceName){return  "You have been unmuted in " +groupName+" by " +sourceName+"!";}
    public static String GROUP_NOT_BELONG(String groupName){return "You are not part of " + groupName + "!";}
    public static String GROUP_NOT_HAVE_PREVILEDGES(String groupName){return "You are neither an admin nor a co-admin of " + groupName + "!";}
    public static String GROUP_NOT_HAVE_ADMIN_PREVILEDGES(String groupName){return "You are not an admin of " + groupName + "!";}
    public static String GROUP_ALREADY_HAVE_PREVILEDGES(String groupName, String username){return username+ " already have an admin or a co-admin privileges in " + groupName + "!";}
    public static String GROUP_TARGET_ALREADY_BELONGS(String groupName, String targetUserName){return targetUserName+ " is already in " + groupName + "!";}
    public static String GROUP_RESPOND_TO_SOURCE(String username, String answer){return username+ " responded to group invite with the following answer: " + answer;}
    public static String GROUP_INVITE_PROMPT(String groupName){ return "You have been invited to " + groupName + ", Accept?";}
    public static String GROUP_INVITE_TIMEOUT(String groupName){ return "You didn't answer on time to group:" + groupName + " Invitation has been canceled";}
    public static String GROUP_WELCOME_PROMPT(String groupName){ return "Welcome to " + groupName + "!";}
    public static String GROUP_REMOVE_PROMPT(String groupName, String sourceUserName){ return "You have been removed from "+ groupName + " by " + sourceUserName+ "!";}
    public static String GROUP_ACTION_ON_ADMIN(String groupName, String sourceUserName){return " action denied because " + sourceUserName+ " is admin in " + groupName;}

    public static String ERROR_PRINTING(String text){
        return ANSI_RED + text + ANSI_RESET;
    }

    public static String PRINTING(String action, String sourceName, String message){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        return "["+ formatter.format(date) +"] [" + action + "] [" + sourceName + "] " + message;
    }

    public static int toSeconds(int seconds){return seconds * 1000;}

}


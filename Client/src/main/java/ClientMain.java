import SharedMessages.Messages;
import SharedMessages.Messages.*;
import Users.Constants;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.routing.Broadcast;
import com.typesafe.config.ConfigFactory;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


    class ClientMain {
        private static final ActorSystem system = ActorSystem.create(Constants.CLIENT_SYSTEM, ConfigFactory.load()); // Creating environment
        private static final ActorSelection manager = system.actorSelection(Constants.PATH_TO_MANAGER);
        private static ActorRef clientRef;
        private static final Scanner scanner = new Scanner(System.in);   // Creating a Scanner object
        private static boolean connect = false;
        private static String clientUserName = "";
        private static final Timeout timeout = Timeout.create(Duration.ofSeconds(2));
        private static AtomicBoolean isInviteAnswer = new AtomicBoolean(false); //client ansew from keyboard
        private static AtomicBoolean expectingInviteAnswer = new AtomicBoolean(false); //expecting yes or no from keyboard
        private static final Object waitingObject = new Object();
        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_RED = "\u001B[31m";


        public static void main(String[] args) {
            while(true){
                try{
                    disconnectedActions(); // verify that a connect command happen before any other command
                    connectedActions(); // infinity loop for user input,after connect command until disconnect command
                } catch (Exception e){
                    e.printStackTrace();
                    break;
                }
            }
            scanner.close();
            system.terminate();
        }

        /**
         * handles actions while user is connected
         */
        private static void connectedActions() {
            String userInput;
            String[] command;
            while (connect) {
                userInput = scanner.nextLine();
                command = userInput.split("\\s+");

                if (expectingInviteAnswer.get()) {
                    getInviteAnswer(command);
                    continue;
                }

                switch (command[0]) {
                    case "/user":
                        userCommandSwitch(command);
                        break;
                    case "/group":
                        groupCommandSwitch(command);
                        break;
                    default:
                        System.out.println("The Chat feature you requested does not exist, please try again");
                }
            }
        }

        /**
         * handles action while user is disconnected
         */
        private static void disconnectedActions() {
            String userInput;
            String[] command;
            while (!connect) {
                userInput = scanner.nextLine();
                command = userInput.split("\\s+");
                if (command.length != 3 || !command[0].equals("/user") || !command[1].equals("connect"))
                    System.out.println("Before any chat activity, you must connect");
                else
                    onConnect(command[2]);
            }
        }

        /**
         * get answer from user regarding group invitationץ
         * sets AtomicBoolean according to answer.
         * wakes up actor to handle answer
         * @param command
         */
        private static void getInviteAnswer(String[] command) {
            if (command.length != 1) {
                System.out.println("Answer must be \"yes\" or \"no\" (more than 1 word)");
                return;
            }
            String  answer = command[0].toLowerCase();
            switch (answer) {
                case "yes":
                case "no":
                    isInviteAnswer.set(
                            answer.equals("yes")
                    );
                    expectingInviteAnswer.set(false);
                    try {
                        synchronized (waitingObject) {
                            waitingObject.notifyAll();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                default:
                    System.out.println("Answer must be \"yes\" or \"no\" ");
            }
        }

        /**
         * handles /user ...commands
         * @param command
         */

        private static void userCommandSwitch(String[] command) {

            switch (command[1]) { // /user disconnect
                case "disconnect":
                    if (command.length == 2) {
                        onDisconnect();
                        return;
                    }
                case "text":
                    if (command.length > 3) { // /user text targer msg
                        onChatTextualMessage(command[2], extractMsg(command, 3));
                        return;
                    }
                case "file":
                    if (command.length == 4) { // /user text target filePath
                        onChatBinaryMessage(command[2], command[3]);
                        return;
                    }
            }
            System.out.println("The User feature you requested does not exist, please try again");
        }

        /**
         * handles /group ....commands
         * @param command
         */

        private static void groupCommandSwitch(String[] command) {
            switch (command[1]) {
                case "create":
                    if (command.length == 3) {
                        onGroupCreate(command[2]);
                        return;
                    }
                case "leave":
                    if (command.length == 3) {
                        onGroupLeave(command[2]);
                        return;
                    }
                case "user":
                    groupUserCommandSwitch(command);
                    return;
                case "coadmin":
                    coAdminCommandSwitch(command);
                    return;
                case "send":
                    groupSendCommandSwitch(command);
                    return;
            }

            System.out.println("The Group feature you requested does not exist, please try again");
        }

        /**
         * handles /group send ...commands
         * @param command
         */
        private static void groupSendCommandSwitch(String[] command) {
            switch (command[2]) {
                case "text":
                    if (command.length >= 5) {
                        onGroupSendText(command[3], extractMsg(command, 4));
                        return;
                    }
                case "file":
                    if (command.length == 5) {
                        onGroupSendFile(command[3], command[4]);
                        return;
                    }
            }
            System.out.println("The Group send feature you requested does not exist, please try again");
        }


        /**
         * handles /group user ...commands
         * @param command
         */
        private static void groupUserCommandSwitch(String[] command) {
            switch (command[2]) {
                case "invite":
                    if (command.length == 5) {
                        onGroupInvite(command[3], command[4]);
                        return;
                    }
                case "remove":
                    if (command.length == 5) {
                        onGroupRemove(command[3], command[4]);
                        return;
                    }
                case "mute":
                    if (command.length == 6) {
                        onGroupMute(command[3], command[4], command[5]);
                        return;
                    }
                case "unmute":
                    if (command.length == 5) {
                        onGroupUnMute(command[3], command[4]);
                        return;
                    }
            }
            System.out.println("The Group user feature you requested does not exist, please try again");
        }

        /**
         * handles /group coadmin ...commands
         * @param command
         */
        private static void coAdminCommandSwitch(String[] command) {
            switch (command[2]) {
                case "add":
                    if (command.length == 5) {
                        onCoAdminAdd(command[3], command[4]);
                        break;
                    }
                case "remove":
                    if (command.length == 5) {
                        onCoAdminRemove(command[3], command[4]);
                        break;
                    }
                default:
                    System.out.println("The Group Co-admin feature you requested does not exist, please try again");
            }
        }

        /**
         * creates string, starting at indexMessage untill end of command array
         * @param command
         * @param indexMessage
         * @return String
         */
        private static String extractMsg(String[] command, int indexMessage) {
            String[] temp = Arrays.copyOfRange(command, indexMessage, command.length);
            String msg = Arrays.toString(temp);
            msg = msg.substring(1, msg.length() - 1).replace(",", "");
            return msg;
        }



        /***************************** SERVER/USER REQUESTS ***********************/

        /**
         * ask server is username doesn't exist.
         * if true -> connects user and prints text message
         * if false -> prints error message
         * @param username
         */
        private static void onConnect(String username) {
            /**
             * Connecting a user to the server - clientActor send Ask message to manager to connect
             **/
            clientRef = system.actorOf(ClientActor.props(isInviteAnswer, expectingInviteAnswer, waitingObject), Constants.CLIENT + "-" + username);   // Creating Client actor
            ConnectionMessage connMsg = new ConnectionMessage(username, clientRef);
            Future<Object> rt = Patterns.ask(manager, connMsg, timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == TextMessage.class) {
                    clientUserName = connMsg.userName;
                    connect = true;
                    System.out.println(((TextMessage) result).text);
                } else
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
        }

        /**
         * ask server is username exists.
         * if true -> disconnects user and prints text message
         * if false -> prints error message
         * kills with poison pill clientRef
         */
        private static void onDisconnect() {
            /**
             * Disconnecting a user from the server - clientActor send Ask message to manager to disconnect
             **/
            Future<Object> rt = Patterns.ask(manager, new DisconnectMessage(clientUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == TextMessage.class) {
                    connect = false;
                    clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                    System.out.println(((TextMessage) result).text);
                } else
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_DISCONN);
            }
        }


        /**
         * validate with server
         * if true -> user sends message to target
         * if false -> prints error message
         * @param targetName
         * @param msg
         */
        private static void onChatTextualMessage(String targetName, String msg) {
            ActorRef targetActor = ServerRequest(new validateUserSendMessage(targetName));
            if (targetActor != null)
                targetActor.tell(new TextMessage(Constants.PRINTING(Constants.ACTION_USER, clientUserName, msg)), clientRef);
        }

        /**
         * validate with server and that file exists
         * if true -> user sends file to target
         * if false -> prints error message
         * @param targetName
         * @param filePath
         */
        private static void onChatBinaryMessage(String targetName, String filePath) {
            if (!isFileExists(filePath)) return;
            ActorRef targetActor = ServerRequest(new validateUserSendMessage(targetName)); //prints error message inside
            if (targetActor == null) {  return; }


            String fileName = Paths.get(filePath).getFileName().toString();
            try {
                byte[] buffer = Files.readAllBytes(Paths.get(filePath));
                targetActor.tell(new AllBytesFileMessage(clientUserName, fileName, Constants.ACTION_USER, buffer), clientRef);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * sends server group create request
         * @param groupName
         */
        private static void onGroupCreate(String groupName) {
            manager.tell(new GroupCreateMessage(groupName, clientUserName), clientRef);
        }

        /**
         * sends server group leave request
         * @param groupName
         */
        private static void onGroupLeave(String groupName) {
            manager.tell(new GroupLeaveMessage(groupName, clientUserName), clientRef);
        }

        /**
         * validates with server
         * invites target to group with ask request
         * sends server request to add target to group iff targets answer was positive
         * @param groupName
         * @param targetUserName
         */
        private static void onGroupInvite(String groupName, String targetUserName) {
            ActorRef targetActor = ServerRequest(new validateGroupInvite(groupName, clientUserName, targetUserName));
            if (targetActor == null) { return;  }

            final Timeout userTimeout = Timeout.create(Duration.ofSeconds(Constants.MINUTE)); //give user 1 minute to answer
            Future<Object> rtInvite = Patterns.ask(targetActor, new GroupInviteRequestReplyMessage(groupName, Constants.GROUP_INVITE_PROMPT(groupName)), userTimeout);
            try {
                Object resultInvite = Await.result(rtInvite, userTimeout.duration());
                if (resultInvite.getClass() == isAcceptInvite.class) {
                    String answer = ((isAcceptInvite) resultInvite).isAccept ? "yes" : "no";
                    if (((isAcceptInvite) resultInvite).isAccept) {
                        Future<Object> rt = Patterns.ask(manager, new GroupInviteMessage(groupName, clientUserName, targetUserName), timeout);
                        try {
                            Object result = Await.result(rt, timeout.duration());
                            if (result.getClass() == isSuccMessage.class) {
                                if (((isSuccMessage) result).isSucc) {
                                    targetActor.tell(new TextMessage(Constants.GROUP_WELCOME_PROMPT(groupName)), clientRef);
                                }
                            } else{
                                System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
                        }
                    }
                    System.out.println(Constants.GROUP_RESPOND_TO_SOURCE(targetUserName, answer));
                } else{
                    System.out.println(ANSI_RED + ((ErrorMessage) resultInvite).error + ANSI_RESET);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.GROUP_INVITE_DIDNT_ANSWER(targetUserName));
            }
        }

        /**
         * validate with server.
         * broadcast text message to group
         * @param groupName
         * @param msg
         */
        private static void onGroupSendText(String groupName, String msg) {
            ActorRef broadcastRouter = ServerRequest(new validateGroupSendMessage(groupName, clientUserName));
            if (broadcastRouter == null) { return; }
            broadcastRouter.tell(new Broadcast(new TextMessage(
                    Constants.PRINTING(groupName, clientUserName, msg))), clientRef);
            broadcastRouter.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }

        /**
         * validate with server and if file exists
         * broadcast file message to group
         * @param groupName
         * @param filePath
         */
        private static void onGroupSendFile(String groupName, String filePath) {
            if (!isFileExists(filePath)) return;
            ActorRef broadcastRouter = ServerRequest(new validateGroupSendMessage(groupName, clientUserName));
            if (broadcastRouter == null) { return; }

            String fileName = Paths.get(filePath).getFileName().toString();
            try {
                byte[] buffer = Files.readAllBytes(Paths.get(filePath));
                broadcastRouter.tell(new Broadcast(new AllBytesFileMessage(
                        clientUserName, fileName, groupName, buffer)), clientRef);
                broadcastRouter.tell(PoisonPill.getInstance(), ActorRef.noSender());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * sends remove request to server.
         * server removes target.
         * user sends remove message to target.
         * @param groupName
         * @param targetUserName
         */
        private static void onGroupRemove(String groupName, String targetUserName){
            Future<Object> rt = Patterns.ask(manager, new GroupRemoveMessage(groupName, clientUserName, targetUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class){
                    ActorRef targetActor = ((AddressMessage) result).targetActor;
                    targetActor.tell(new TextMessage(Constants.PRINTING( groupName, clientUserName,
                            Constants.GROUP_REMOVE_PROMPT(groupName, clientUserName)
                    )), clientRef);
                } else{
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
        }

        /**
         * sends mute request to server.
         * server mutes target.
         * user sends mute message to target.
         * @param groupName
         * @param targetUserName
         * @param timeInSeconds
         */
        private static void onGroupMute(String groupName, String targetUserName, String timeInSeconds){
            Future<Object> rt = Patterns.ask(manager, new GroupMuteMessage(groupName, clientUserName, targetUserName, timeInSeconds), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class){
                    ActorRef targetActor = ((AddressMessage) result).targetActor;
                    targetActor.tell(new TextMessage(Constants.PRINTING( groupName, clientUserName,
                            Constants.GROUP_MUTE(groupName, timeInSeconds,clientUserName)
                    )), clientRef);
                } else{
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
        }

        /**
         * sends unmute request to server.
         * server unmutes target.
         * user sends unmute message to target.
         * @param groupName
         * @param targetUserName
         */
        private static void onGroupUnMute(String groupName, String targetUserName){
            Future<Object> rt = Patterns.ask(manager, new GroupUnMuteMessage(groupName, clientUserName, targetUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class){
                    ActorRef targetActor = ((AddressMessage) result).targetActor;
                    targetActor.tell(new TextMessage(Constants.PRINTING( groupName, clientUserName,
                            Constants.GROUP_UN_MUTE(groupName,clientUserName)
                    )), clientRef);
                } else{
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
        }

        /**
         * sends coadmin add request to server
         * @param groupName
         * @param targetUserName
         */
        private static void onCoAdminAdd(String groupName, String targetUserName){
            manager.tell(new GroupCoAdminAddMessage(groupName, clientUserName, targetUserName), clientRef);
        }

        /**
         * sends remove coadmin request to server
         * @param groupName
         * @param targetUserName
         */
        private static void onCoAdminRemove(String groupName, String targetUserName){
            manager.tell(new GroupCoAdminRemoveMessage(groupName, clientUserName, targetUserName), clientRef);
        }

        /**
         * general server request.
         * @param message
         * @return ActorRef - can be broadcast actor or target actor
         */
        private static ActorRef ServerRequest (Messages message){
            Future<Object> rt = Patterns.ask(manager, message, timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class)
                    return ((AddressMessage) result).targetActor;
                else
                    System.out.println(ANSI_RED + ((ErrorMessage) result).error + ANSI_RESET);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
            return null;
        }

        /**
         * checks if file exists
         * @param filePath
         * @return true iff file exists
         */
        private static boolean isFileExists(String filePath) {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                System.out.println(Constants.NOT_EXIST(filePath));
                return false;
            }
            return true;
        }

    }




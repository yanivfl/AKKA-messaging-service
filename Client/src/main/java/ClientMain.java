import Groups.GroupInfo;
import SharedMessages.Messages.*;
import Users.Constants;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.routing.Broadcast;
import com.typesafe.config.ConfigFactory;
import akka.util.Timeout;
import scala.collection.immutable.Stream;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


//final Config config = ConfigFactory.load().withValue("akka.log-dead-letters", ConfigValueFactory.fromAnyRef("off"));

    class ClientMain {
        private static final ActorSystem system = ActorSystem.create(Constants.CLIENT_SYSTEM, ConfigFactory.load()); // Creating environment
        private static final ActorSelection manager = system.actorSelection(Constants.PATH_TO_MANAGER);
        private static ActorRef clientRef;
        private static final Scanner scanner = new Scanner(System.in);   // Creating a Scanner object
        private static boolean connect;
        private static String clientUserName = "";
        private static final Timeout timeout = Timeout.create(Duration.ofSeconds(2));
        private static AtomicBoolean isInviteAnswer = new AtomicBoolean(false);
        private static AtomicBoolean expectingInviteAnswer = new AtomicBoolean(false);
        private static final Object waitingObject = new Object();


        public static void main(String[] args) {

            String userInput = "";
            String[] command = null;

            // verify that a connect command happen before any other command
            while (!connect) {
                userInput = scanner.nextLine();
                command = userInput.split("\\s+");
                if (command.length != 3 || !command[0].equals("/user") || !command[1].equals("connect"))
                    System.out.println("Before any chat activity, you must connect");
                else
                    onConnect(command[2]);
            }

            // infinity loop for user input,after connect command until disconnect command
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

            // When received a disconnect command, the connection with the server is closed
            scanner.close();
            system.terminate();
        }

        /**
         * Auxiliary methods
         **/

        private static void getInviteAnswer(String[] command) {
            if (command.length != 1) {
                System.out.println("Answer must be \"yes\" or \"no\" (more than 1 word)");
                return;
            }
            switch (command[0]) {
                case "yes":
                case "no":
                    isInviteAnswer.set(
                            command[0].equals("yes")
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

        private static void userCommandSwitch(String[] command) {

            switch (command[1]) { // /user disconnect
                case "disconnect":
                    if (command.length == 2) {
                        onDisconnect();
                        return;
                    }
                case "text":
                    if (command.length > 3) { // /user text targer msg
                        onChatTextualMessage(command[2], extaractMsg(command, 3));
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
                    coadminCommandSwitch(command);
                    return;
                case "send":
                    groupSendCommandSwitch(command);
                    return;
            }

            System.out.println("The Group feature you requested does not exist, please try again");
        }

        private static void groupSendCommandSwitch(String[] command) {
            switch (command[2]) {
                case "text":
                    if (command.length >= 5) {
                        groupSendText(command[3], extaractMsg(command, 4));
                        return;
                    }
                case "file":
                    if (command.length == 5) {
                        groupSendFile(command[3], command[4]);
                        return;
                    }
            }
            System.out.println("The Group send feature you requested does not exist, please try again");
        }

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
                    System.out.println("mute not implement");
                    break;
                case "unmute":
                    System.out.println("unmute not implement");
                    break;
            }
            System.out.println("The Group user feature you requested does not exist, please try again");
        }

        private static void coadminCommandSwitch(String[] command) {
            switch (command[2]) {
                case "add":
                    if (command.length == 5) {
                        onCoadminAdd(command[3], command[4]);
                        break;
                    }
                case "remove":
                    if (command.length == 5) {
                        onCoadminRemove(command[3], command[4]);
                        break;
                    }
                default:
                    System.out.println("The Group Co-admin feature you requested does not exist, please try again");
            }
        }

        private static String extaractMsg(String[] command, int indexMessage) {
            String[] temp = Arrays.copyOfRange(command, indexMessage, command.length);
            String msg = Arrays.toString(temp);
            msg = msg.substring(1, msg.length() - 1).replace(",", "");
            return msg;
        }

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
                    clientUserName = connMsg.username;
                    connect = true;
                    System.out.println(((TextMessage) result).text);
                } else
                    System.out.println(((ErrorMessage) result).error);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
                clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
        }

        private static void onDisconnect() {
            /**
             * Disconnecting a user from the server - clientActor send Ask message to manager to disconnect
             **/
            Future<Object> rt = Patterns.ask(manager, new DisconnectMessage(clientUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == TextMessage.class) {
                    connect = false;
                    System.out.println(((TextMessage) result).text);
                } else
                    System.out.println(((ErrorMessage) result).error);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_DISCONN);
                clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
        }

        private static void onChatTextualMessage(String targetname, String msg) {
            ActorRef targetactor = validateGetTargetActor(targetname);
            if (targetactor != null)
                targetactor.tell(new TextMessage(Constants.PRINTING(Constants.ACTION_USER, clientUserName, msg)), clientRef);
        }

        private static void onChatBinaryMessage(String targetName, String filePath) {
            ActorRef targetactor = validateGetTargetActor(targetName); //prints error message inside
            if (targetactor == null) {
                return;
            }
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                System.out.println(Constants.NOT_EXIST(filePath));
                return;
            }

            String fileName = Paths.get(filePath).getFileName().toString();
            try {
                byte[] buffer = Files.readAllBytes(Paths.get(filePath));
                targetactor.tell(new AllBytesFileMessage(clientUserName, fileName, Constants.ACTION_USER, buffer), clientRef);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private static void onGroupCreate(String groupname) {
            manager.tell(new GroupCreateMessage(groupname, clientUserName), clientRef);
        }

        private static void onGroupLeave(String groupname) {
            manager.tell(new GroupLeaveMessage(groupname, clientUserName), clientRef);
        }

        private static void onGroupInvite(String groupName, String targetUserName) {
            ActorRef targetActor = validateGroupInvite(groupName, targetUserName);
            if (targetActor == null) {
                return;
            }

            final Timeout userTimeout = Timeout.create(Duration.ofSeconds(60)); //give user 1 minute to answer
            Future<Object> rt = Patterns.ask(targetActor, new GroupInviteRequestReply(groupName, Constants.GROUP_INVITE_PROMPT(groupName)), userTimeout);
            try {
                Object result = Await.result(rt, userTimeout.duration());
                if (result.getClass() == isAcceptInvite.class) {
                    String answer = ((isAcceptInvite) result).isAccept ? "yes" : "no";
                    if (((isAcceptInvite) result).isAccept) {
                        rt = Patterns.ask(manager, new GroupInviteMessage(groupName, clientUserName, targetUserName), timeout);
                        try {
                            result = Await.result(rt, timeout.duration());
                            if (result.getClass() == isSuccMessage.class) {
                                if (((isSuccMessage) result).isSucc) {
                                    targetActor.tell(new TextMessage(Constants.GROUP_WELCOME_PROMPT(groupName)), clientRef);
                                }
                            } else{
                                System.out.println(((ErrorMessage) result).error);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
                            clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
                        }
                    }
                    System.out.println(Constants.GROUP_RESPOND_TO_SOURCE(clientUserName, answer));
                } else{
                    System.out.println(((ErrorMessage) result).error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(targetUserName + " did not answer on time.");
            }
        }

        private static void groupSendText(String groupName, String msg) {
            ActorRef broadcastRouter = validateGetRouterActor(groupName);
            if (broadcastRouter == null) {
                return;
            }
            broadcastRouter.tell(new Broadcast(new TextMessage(
                    Constants.PRINTING(groupName, clientUserName, msg))), clientRef);
        }

        private static void groupSendFile(String groupName, String filePath) {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory()) {
                System.out.println(Constants.NOT_EXIST(filePath));
                return;
            }
            ActorRef broadcastRouter = validateGetRouterActor(groupName);
            if (broadcastRouter == null) { return; }
            String fileName = Paths.get(filePath).getFileName().toString();
            try {
                byte[] buffer = Files.readAllBytes(Paths.get(filePath));
                broadcastRouter.tell(new Broadcast(new AllBytesFileMessage(
                        clientUserName, fileName, groupName, buffer)), clientRef);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static void onGroupRemove (String groupName, String targetUserName){
            ActorRef targetActor = null;
            Future<Object> rt = Patterns.ask(manager, new GroupRemoveMessage(groupName, clientUserName, targetUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class){
                    targetActor = ((AddressMessage) result).targetactor;
                    if( targetActor != null) {
                        targetActor.tell(new TextMessage(Constants.PRINTING( groupName, clientUserName,
                                Constants.GROUP_REMOVE_PROMPT(groupName, clientUserName)
                        )), clientRef);
                    }
                } else{
                    System.out.println(((ErrorMessage) result).error);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
        }

        private static void onGroupMute(String groupName, String targetUserName, String timeInSeconds){
            ActorRef targetActor = null;
            Future<Object> rt = Patterns.ask(manager, new GroupMuteMessage(groupName, clientUserName, targetUserName, timeInSeconds), timeout);


        }

        private static void onGroupUnMute(String groupName, String targetUserName{
            ActorRef targetActor = null;
            Future<Object> rt = Patterns.ask(manager, new GroupUnMuteMessage(groupName, clientUserName, targetUserName), timeout);


        }

        private static void onCoadminAdd (String groupName, String targetUserName){
            manager.tell(new GroupCoadminAddMessage(groupName, clientUserName, targetUserName), clientRef);
        }

        private static void onCoadminRemove (String groupName, String targetUserName){
            manager.tell(new GroupCoadminRemoveMessage(groupName, clientUserName, targetUserName), clientRef);
        }

        private static ActorRef validateGroupInvite (String groupName, String targetUserName){
            Future<Object> rt = Patterns.ask(manager, new validateGroupInvite(groupName, clientUserName, targetUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class)
                    return ((AddressMessage) result).targetactor;
                else
                    System.out.println(((ErrorMessage) result).error);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
            return null;
        }


        private static ActorRef validateGetTargetActor (String targetname){
            Future<Object> rt = Patterns.ask(manager, new validateUserSendMessage(targetname), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class)
                    return ((AddressMessage) result).targetactor;
                else
                    System.out.println(((ErrorMessage) result).error);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
            return null;
        }


        private static ActorRef validateGetRouterActor (String groupName){
            Future<Object> rt = Patterns.ask(manager, new validateGroupSendMessage(groupName, clientUserName), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class)
                    return ((AddressMessage) result).targetactor;
                else
                    System.out.println(((ErrorMessage) result).error);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
            }
            return null;
        }
    }




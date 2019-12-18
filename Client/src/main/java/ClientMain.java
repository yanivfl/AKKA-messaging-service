import SharedMessages.Messages.*;
import Users.Constants;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import com.typesafe.config.ConfigFactory;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Scanner;


//final Config config = ConfigFactory.load().withValue("akka.log-dead-letters", ConfigValueFactory.fromAnyRef("off"));

    class ClientMain {
        private static final ActorSystem system = ActorSystem.create(Constants.CLIENT_SYSTEM, ConfigFactory.load()); // Creating environment
        private static final ActorSelection manager = system.actorSelection(Constants.PATH_TO_MANAGER);
        private static ActorRef clientRef;
        private static final Scanner scanner = new Scanner(System.in);   // Creating a Scanner object
        private static boolean connect;
        private static String clientUserName = "";
        private static final Timeout timeout = Timeout.create(Duration.ofSeconds(1));



        public static void main(String[] args) {

            String userInput = "";
            String[] command = null;

            // verify that a connect command happen before any other command
            while (!connect) {
                userInput = scanner.nextLine();
                command = userInput.split("\\s+");
                if ( command.length != 3 || !command[0].equals("/user") || !command[1].equals("connect"))
                    System.out.println("Before any chat activity, you must connect");
                else
                    onConnect(command[2]);
            }

            // infinity loop for user input,after connect command until disconnect command
            while (connect) {
                userInput = scanner.nextLine();
                command = userInput.split("\\s+");

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
            system.terminate();
        }

        /**
         * Auxiliary methods
         **/

        public static void userCommandSwitch(String[] command) {

            switch (command[1]) { // /user disconnect
                case "disconnect":
                    if(command.length ==2){
                        onDisconnect();
                        return;
                    }
                case "text":
                    if (command.length > 3) { // /user text targer msg
                        onChatTextualMessage(command[2], extaractMsg(command));
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

        public static void groupCommandSwitch(String[] command) {
        switch (command[1]) {
            case "create":
                onGroupCreate(command[2]);
                break;
            case "leave":
                onGroupLeave(command[2]);
                break;
            case "invite":
                onGroupInvite(command[2], command[3]);
                break;
            case "send":
                System.out.println("send not implement");
                break;
            case "remove":
                System.out.println("remove not implement");
                break;
            case "mute":
                System.out.println("mute not implement");
                break;
            case "unmute":
                System.out.println("unmute not implement");
                break;
            case "coadmin":
                coadminCommandSwitch(command);
                break;
            default:
                System.out.println("The Group feature you requested does not exist, please try again");
        }
        }

        public static void coadminCommandSwitch(String[] command) {
            switch (command[3]) {
                case "add":
                    System.out.println("coadmin add not implement");
                    break;
                case "remove":
                    System.out.println("coadmin remove not implement");
                    break;
                default:
                    System.out.println("The Group Co-admin feature you requested does not exist, please try again");
            }
        }

        public static String extaractMsg(String[] command) {
            String[] temp = Arrays.copyOfRange(command, 3, command.length);
            String msg = Arrays.toString(temp);
            msg = msg.substring(1, msg.length() - 1).replace(",", "");
            return msg;
        }

        private static void onConnect(String username) {
            /**
             * Connecting a user to the server - clientActor send Ask message to manager to connect
             **/
            clientRef = system.actorOf(Props.create(ClientActor.class), Constants.CLIENT +"-"+ username);   // Creating Client actor
            ConnectionMessage connMsg = new ConnectionMessage(username, clientRef);
            Future<Object> rt = Patterns.ask(manager, connMsg, timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == TextMessage.class) {
                    clientUserName = connMsg.username;
                    connect = true;
                    System.out.println(((TextMessage)result).text);
                } else
                    System.out.println(((ErrorMessage)result).error);

            } catch (Exception e) {
                System.out.println(Constants.SERVER_IS_OFFLINE_CONN);
                clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
        }

        private static void onDisconnect() {
            /**
             * Disconnecting a user from the server - clientActor send Ask message to manager to disconnect
             **/
            Future<Object> rt = Patterns.ask(manager, new DisconnectMessage(clientUserName) , timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == TextMessage.class) {
                    connect = false;
                    System.out.println(((TextMessage) result).text);
                }
                else
                    System.out.println(((ErrorMessage)result).error);

            } catch (Exception e) {
                System.out.println(Constants.SERVER_IS_OFFLINE_DISCONN);
                clientRef.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }
        }

        private static void onChatTextualMessage(String targetname, String msg) {
            ActorRef targetactor = getTargetActorRef(targetname);
            if (targetactor!=null)
                targetactor.tell(new TextMessage(Constants.PRINTING("user", clientUserName, msg)), clientRef);
        }

        private static void onChatBinaryMessage(String targetName, String filePath)  {
            ActorRef targetactor = getTargetActorRef(targetName); //prints error message inside
            if (targetactor==null) { return;}
            File file = new File(filePath);
            if(!file.exists() || file.isDirectory()) {
                System.out.println(Constants.NOT_EXIST(filePath));
                return;
            }
            try{
                InputStream inputStream =null;
                try {
                    byte[] buffer = new byte[Constants.BUFFER_SIZE];
                    inputStream = new BufferedInputStream(new FileInputStream(filePath));
                    String fileName =Paths.get(filePath).getFileName().toString();
                    int bytesRead;
                    int iteration =0;
                    while ((bytesRead = inputStream.read(buffer)) >= 0) {
                        targetactor.tell(new FileMessage(clientUserName, fileName, buffer, bytesRead, iteration,  false), clientRef);
                        iteration++;
                    }
                    targetactor.tell(new FileMessage(clientUserName ,fileName, buffer, bytesRead, iteration, true), clientRef);
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    inputStream.close();
                }
            } catch (IOException e){
                e.printStackTrace();
            }

        }


        private static void onGroupCreate(String groupname) {
            manager.tell(new GroupCreateMessage(groupname, clientUserName), clientRef);
        }

        private static void onGroupLeave(String groupname) {
            manager.tell(new GroupLeaveMessage(groupname, clientUserName), clientRef);
        }

        private static void onGroupInvite(String groupname, String targetusername) {
            manager.tell(new GroupInviteMessage(groupname, clientUserName, targetusername), clientRef);
        }

        private static ActorRef getTargetActorRef(String targetname) {
            Future<Object> rt = Patterns.ask(manager, new isUserExistMessage(targetname), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result.getClass() == AddressMessage.class)
                    return  ((AddressMessage) result).targetactor;
                else
                    System.out.println(((ErrorMessage)result).error);
            } catch (Exception e) {
                System.out.println("server offline");
            }
            return null;
        }
    }


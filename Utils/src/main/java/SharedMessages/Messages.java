package SharedMessages;

import akka.actor.ActorRef;

import java.io.OutputStream;
import java.io.Serializable;

public class Messages implements Serializable {


    public static class DisconnectMessage implements Serializable {
        public final String username;

        public DisconnectMessage(String clientUserName) {
            this.username = clientUserName;
        }
    }

    public static class ConnectionMessage implements Serializable {
        public final String username;
        public final ActorRef client;

        public ConnectionMessage(String username, ActorRef client) {
            this.username = username;
            this.client = client;
        }
    }

    public static class ErrorMessage implements Serializable {
        public final String error;

        public ErrorMessage(String error) {
            this.error = error;
        }
    }

    public static class TextMessage implements Serializable {
        public final String text;

        public TextMessage(String text) {
            this.text = text;
        }
    }

    public static class isUserExistMessage implements Serializable {
        public final String targetusername;

        public isUserExistMessage(String targetusername) {
            this.targetusername = targetusername;
        }
    }

    public static class AddressMessage implements Serializable {
        public final ActorRef targetactor;

        public AddressMessage(ActorRef targetactor) {
            this.targetactor = targetactor;
        }
    }


    public static class FileMessage implements Serializable {
        public final String userName;
        public final String fileName;
        public final byte[] buffer;
        public final int bytesRead;
        public final boolean isDone;
        public final OutputStream outputStream;
        public FileMessage(String userName, String fileName, byte[] buffer, int bytesRead, OutputStream outputStream, boolean isDone) {
            this.userName = userName;
            this.fileName = fileName;
            this.buffer = buffer;
            this.bytesRead = bytesRead;
            this.isDone = isDone;
            this.outputStream = outputStream;
        }
    }

    public static class AllBytesFileMessage implements Serializable {
        public final String userName;
        public final String fileName;
        public final String action;
        public final byte[] buffer;

        public AllBytesFileMessage(String userName, String fileName, String action, byte[] buffer) {
            this.userName = userName;
            this.fileName = fileName;
            this.buffer = buffer;
            this.action = action; //user or groupname
        }
    }

    public static class GroupCreateMessage implements Serializable {
        public final String username;
        public final String groupname;

        public GroupCreateMessage(String groupname, String username) {
            this.groupname = groupname;
            this.username = username;
        }
    }

    public static class GroupLeaveMessage implements Serializable {
        public final String groupname;
        public final String username;

        public GroupLeaveMessage(String groupname, String username) {
            this.groupname = groupname;
            this.username = username;
        }
    }

    public static class GroupSendTextMessage implements Serializable {
        public final String groupname;
        public final String sourcename;
        public final String message;

        public GroupSendTextMessage(String groupname, String sourcename, String message) {
            this.groupname = groupname;
            this.sourcename = sourcename;
            this.message = message;

        }
    }

    public static class GroupSendFileMessage implements Serializable {
        public final String groupname;
        public final String sourcename;
        public final String fileName;
        public final byte[] buffer;

        public GroupSendFileMessage(String groupname, String sourcename, String fileName, byte[] buffer) {
            this.groupname = groupname;
            this.sourcename = sourcename;
            this.fileName = fileName;
            this.buffer = buffer;

        }
    }

    public static class GroupInviteMessage implements Serializable {
        public final String groupname;
        public final String sourceusername;
        public final String targetusername;

        public GroupInviteMessage(String groupname, String sourceusername, String targetusername) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.targetusername = targetusername;
        }
    }

    public static class GroupInviteRequestReply implements Serializable {
        public final String groupname;
        public final String sourceusername;
        public final String text;
        public final ActorRef sourceActor;

        public GroupInviteRequestReply(String groupname, String sourceusername, String text, ActorRef sourceActor) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.text = text;
            this.sourceActor = sourceActor;
        }
    }

    public static class GroupRemoveMessage implements Serializable {
        public final String groupname;
        public final String sourceusername;
        public final String targetusername;

        public GroupRemoveMessage(String groupname, String sourceusername, String targetusername) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.targetusername = targetusername;
        }
    }

    public static class GroupCoadminAddMessage implements Serializable {
        public final String groupname;
        public final String sourceusername;
        public final String targetusername;

        public GroupCoadminAddMessage(String groupname, String sourceusername, String targetusername) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.targetusername = targetusername;
        }
    }

    public static class GroupCoadminRemoveMessage implements Serializable {
        public final String groupname;
        public final String sourceusername;
        public final String targetusername;

        public GroupCoadminRemoveMessage(String groupname, String sourceusername, String targetusername) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.targetusername = targetusername;
        }
    }

    public static class isAcceptInvite implements Serializable {
        public final boolean isAccept;


        public isAcceptInvite(boolean isAccept) {
            this.isAccept = isAccept;
        }
    }


    public static class printServerMessage implements Serializable {
        public static void printServerMessage(Object message) {
            if (message.getClass() == TextMessage.class)
                System.out.println(((TextMessage) message).text);

            if (message.getClass() == ErrorMessage.class)
                System.out.println(((ErrorMessage) message).error);

            if (message.getClass() == TextMessage.class)
                System.out.println(((TextMessage) message).text);

            else
                System.out.println("Unknown message received to print");
        }
    }
}



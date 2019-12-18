package SharedMessages;

import akka.actor.ActorRef;

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
        public final int iteration;
        public FileMessage(String userName,String fileName, byte[] buffer, int bytesRead, int iteration, boolean isDone) {
            this.userName = userName;
            this.fileName = fileName;
            this.buffer = buffer;
            this.bytesRead = bytesRead;
            this.isDone = isDone;
            this.iteration = iteration;
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
        public final String message;

        public GroupSendTextMessage(String groupname, String message) {
            this.groupname = groupname;
            this.message = message;
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

        public GroupInviteRequestReply(String groupname, String sourceusername, String text) {
            this.groupname = groupname;
            this.sourceusername = sourceusername;
            this.text = text;
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



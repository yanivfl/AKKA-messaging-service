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

    public static class validateUserSendMessage implements Serializable {
        public final String targetusername;

        public validateUserSendMessage(String targetusername) {
            this.targetusername = targetusername;
        }
    }

    public static class validateGroupSendMessage implements Serializable {
        public final String groupName;
        public final String sourceName;


        public validateGroupSendMessage(String groupName, String sourceName) {
            this.groupName = groupName;
            this.sourceName = sourceName;
        }
    }

    public static class validateGroupInvite implements Serializable {
        public final String targetUserName;
        public final String groupName;
        public final String sourceUserName;

        public validateGroupInvite(String groupName, String sourceUserName, String targetUserName) {
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.groupName = groupName;
        }
    }

    public static class AddressMessage implements Serializable {
        public final ActorRef targetactor;

        public AddressMessage(ActorRef targetactor) {
            this.targetactor = targetactor;
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


    public static class GroupInviteMessage implements Serializable {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupInviteMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupInviteRequestReply implements Serializable {
        public final String groupname;
        public final String text;


        public GroupInviteRequestReply(String groupname, String text) {
            this.groupname = groupname;
            this.text = text;

        }
    }

    public static class isAcceptInvite implements Serializable {
        public final boolean isAccept;


        public isAcceptInvite(boolean isAccept) {
            this.isAccept = isAccept;
        }
    }

    public static class isSuccMessage implements Serializable {
        public final boolean isSucc;

        public isSuccMessage(boolean isSucc) {
            this.isSucc = isSucc;
        }
    }
}



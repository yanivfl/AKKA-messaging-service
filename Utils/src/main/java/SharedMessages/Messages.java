package SharedMessages;

import akka.actor.ActorRef;
import java.io.Serializable;

public class Messages implements Serializable {


    public static class DisconnectMessage extends Messages {
        public final String userName;

        public DisconnectMessage(String clientUserName) {
            this.userName = clientUserName;
        }
    }

    public static class ConnectionMessage extends Messages {
        public final String userName;
        public final ActorRef client;

        public ConnectionMessage(String userName, ActorRef client) {
            this.userName = userName;
            this.client = client;
        }
    }

    public static class ErrorMessage extends Messages {
        public final String error;

        public ErrorMessage(String error) {
            this.error = error;
        }
    }

    public static class TextMessage extends Messages {
        public final String text;

        public TextMessage(String text) {
            this.text = text;
        }
    }

    public static class validateUserSendMessage extends Messages {
        public final String targetUserName;

        public validateUserSendMessage(String targetUserName) {
            this.targetUserName = targetUserName;
        }
    }

    public static class validateGroupSendMessage extends Messages {
        public final String groupName;
        public final String sourceName;


        public validateGroupSendMessage(String groupName, String sourceName) {
            this.groupName = groupName;
            this.sourceName = sourceName;
        }
    }

    public static class validateGroupInvite extends Messages {
        public final String targetUserName;
        public final String groupName;
        public final String sourceUserName;

        public validateGroupInvite(String groupName, String sourceUserName, String targetUserName) {
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.groupName = groupName;
        }
    }

    public static class AddressMessage extends Messages {
        public final ActorRef targetActor;

        public AddressMessage(ActorRef targetActor) {
            this.targetActor = targetActor;
        }
    }


    public static class AllBytesFileMessage extends Messages {
        public final String userName;
        public final String fileName;
        public final String action;
        public final byte[] buffer;

        public AllBytesFileMessage(String userName, String fileName, String action, byte[] buffer) {
            this.userName = userName;
            this.fileName = fileName;
            this.buffer = buffer;
            this.action = action; //user or groupName
        }
    }

    public static class GroupCreateMessage extends Messages {
        public final String userName;
        public final String groupName;

        public GroupCreateMessage(String groupName, String userName) {
            this.groupName = groupName;
            this.userName = userName;
        }
    }

    public static class GroupLeaveMessage extends Messages {
        public final String groupName;
        public final String userName;

        public GroupLeaveMessage(String groupName, String userName) {
            this.groupName = groupName;
            this.userName = userName;
        }
    }


    public static class GroupInviteMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupInviteMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupInviteRequestReplyMessage extends Messages {
        public final String groupName;
        public final String text;


        public GroupInviteRequestReplyMessage(String groupName, String text) {
            this.groupName = groupName;
            this.text = text;

        }
    }

    public static class GroupRemoveMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupRemoveMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupMuteMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;
        public final int timeInSeconds;

        public GroupMuteMessage(String groupName, String sourceUserName, String targetUserName, String timeInSeconds) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.timeInSeconds = Integer.parseInt(timeInSeconds);
        }
    }

    public static class GroupUnMuteMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupUnMuteMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupCoAdminAddMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupCoAdminAddMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupCoAdminRemoveMessage extends Messages {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupCoAdminRemoveMessage(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }

    public static class isAcceptInvite extends Messages {
        public final boolean isAccept;


        public isAcceptInvite(boolean isAccept) {
            this.isAccept = isAccept;
        }
    }

    public static class isSuccMessage extends Messages {
        public final boolean isSucc;

        public isSuccMessage(boolean isSucc) {
            this.isSucc = isSucc;
        }
    }
}



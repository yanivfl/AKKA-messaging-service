package SharedMessages;

import java.io.Serializable;

public class Messages implements Serializable {

    public enum Message implements Serializable {
        GREET, DONE;
    }

    public class DisconnectMessage implements Serializable {
        public  DisconnectMessage(){ }
    }

    public static class ConnectionMessage implements Serializable {
        public final String username;
        public ConnectionMessage(String username){
            this.username = username;
        }
    }

    public static class ErrorMessage implements Serializable {
        public final String error;
        public ErrorMessage(String error){
            this.error = error;
        }
    }

    public static class TextMessage implements Serializable {
        public final String text;
        public TextMessage(String text){
            this.text = text;
        }
    }
}


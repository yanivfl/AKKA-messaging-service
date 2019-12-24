import SharedMessages.Messages.*;
import Users.Constants;
import Users.SharedFucntions;
import akka.actor.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClientActor extends AbstractActor {
    private final ActorRef myActorRef = this.self();
    private AtomicBoolean isInviteAnswer;
    private AtomicBoolean expectingInviteAnswer;
    private final Object waitingObject;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";

    public ClientActor(AtomicBoolean isInviteAnswer, AtomicBoolean expectingInviteAnswer, Object waitingObject) {
        this.isInviteAnswer = isInviteAnswer;
        this.waitingObject = waitingObject;
        this.expectingInviteAnswer = expectingInviteAnswer;
    }

    static Props props(AtomicBoolean isInviteAnswer, AtomicBoolean expectingInviteAnswer, Object waitingObject) {
        return Props.create(ClientActor.class, () -> new ClientActor(isInviteAnswer, expectingInviteAnswer, waitingObject));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TextMessage.class, this::onTextMessage)
                .match(AllBytesFileMessage.class, this::onAllBytesFileMessage)
                .match(ErrorMessage.class, this::onErrorMessage)
                .match(GroupInviteRequestReplyMessage.class, this::onGroupInviteRequestReplyMessage)
                .build();
    }

    /**
     * on recieving a file message.
     * user saves the file, prints recieved message
     * @param abfileMsg
     */
    private void onAllBytesFileMessage(AllBytesFileMessage abfileMsg) {
        String outputFile = SharedFucntions.getTargetFilePath(abfileMsg.userName,abfileMsg.fileName);
        try (FileOutputStream stream = new FileOutputStream(outputFile)) {
            stream.write(abfileMsg.buffer);
            System.out.println(Constants.PRINTING(abfileMsg.action, abfileMsg.userName, "File received: " + outputFile));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * on recieving text message, prints it
     * @param textMsg
     */
    private void onTextMessage(TextMessage textMsg) {
        System.out.println(textMsg.text);
    }

    /**
     * on recieving error message, prints it with red color
     * @param errorMsg
     */
    private void onErrorMessage(ErrorMessage errorMsg) {
        System.out.println(ANSI_RED+ errorMsg.error+ ANSI_RESET);
    }

    /**
     * prints message, and goes to sleep.
     * thread will wake up once user answers yes or no in keyboard.
     * sends answer to source client who invited.
     * @param reqMsg
     */
    private void onGroupInviteRequestReplyMessage(GroupInviteRequestReplyMessage reqMsg) {
        System.out.println(reqMsg.text);
        expectingInviteAnswer.set(true);
        try{
            synchronized (waitingObject){
                waitingObject.wait();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            getSender().tell(new isAcceptInvite(isInviteAnswer.get()), myActorRef);
        }
    }
}

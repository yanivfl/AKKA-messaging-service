import SharedMessages.Messages.*;
import Users.Constants;

import Users.SharedFucntions;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;


public class ClientActor extends AbstractActor {
    private String clientUserName = "";
    private final ActorRef myActorRef = this.self();
    public final ActorSelection manager = getContext().actorSelection(Constants.PATH_TO_MANAGER);
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private AtomicBoolean isInviteAnswer;
    private AtomicBoolean expectingInviteAnswer;
    private Object waitingObject;

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
                .match(AllBytesFileMessage.class, this::AllBytesFileMessage)
                .match(ErrorMessage.class, this::onErrorMessage)
                .match(GroupInviteRequestReply.class, this::onGroupInviteRequestReply)
                .build();
    }


    private void AllBytesFileMessage(AllBytesFileMessage abfileMsg) {
        String outputFile = SharedFucntions.getTargetFilePath(abfileMsg.userName,abfileMsg.fileName);
        try (FileOutputStream stream = new FileOutputStream(outputFile)) {
            stream.write(abfileMsg.buffer);
            System.out.println(Constants.PRINTING(abfileMsg.action, abfileMsg.userName, "File received: " + outputFile));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onTextMessage(TextMessage textMsg) {
        System.out.println(textMsg.text);
    }

    private void onErrorMessage(ErrorMessage errorMsg) {
        System.out.println(errorMsg.error);
    }

    private void onGroupInviteRequestReply(GroupInviteRequestReply reqMsg) {
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
            String answer = isInviteAnswer.get()? "yes" : "no";
            reqMsg.sourceActor.tell(new TextMessage(
                    Constants.GROUP_RESPOND_TO_SOURCE(clientUserName,answer )), myActorRef);
        }
    }
}

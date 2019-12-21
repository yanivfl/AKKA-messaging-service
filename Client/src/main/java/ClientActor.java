import SharedMessages.Messages.*;
import Users.Constants;

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
                .match(FileMessage.class, this::onFileMessage)
                .match(ErrorMessage.class, this::onErrorMessage)
                .match(GroupInviteRequestReply.class, this::onGroupInviteRequestReply)
                .build();
    }

    /***
     * get targetfilepath, create directories if needed
     * @param fileMsg
     * @return target path name
     */
    private String getTargetFilePath(FileMessage fileMsg){
        String outputpath =  Paths.get( Constants.CHAT_DOWNLOADS_DIR, fileMsg.userName).toString();
        new File(outputpath).mkdirs();
        return Paths.get( outputpath, fileMsg.fileName).toString();
    }

    private byte[] getFixedBuffer(byte[] buffer, int bytesRead){
        byte[] output = new byte[bytesRead];
        for (int i = 0; i < bytesRead ; i++) {
            output[i] = buffer[i];
        }
        return output;
    }

    private void onFileMessage(FileMessage fileMsg) {
        String outputFile =getTargetFilePath(fileMsg);
        if(fileMsg.isDone){
            //[<time>][user][<source>] File received: <targetfilepath>
            System.out.println(Constants.PRINTING("user", fileMsg.userName, "File received: " + outputFile));
            return;
        }
        try {
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile, true));
                outputStream.write(getFixedBuffer(fileMsg.buffer, fileMsg.bytesRead));

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                outputStream.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private void onTextMessage(TextMessage textMsg) {
        System.out.println(textMsg.text);
    }

    private void onErrorMessage(ErrorMessage errorMsg) {
        logger.info(errorMsg.error); //TODO: delete
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
        }
    }
}

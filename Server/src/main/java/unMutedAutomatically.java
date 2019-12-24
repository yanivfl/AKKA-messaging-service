import Groups.GroupInfo;
import SharedMessages.Messages;
import Users.Constants;
import akka.actor.ActorRef;

import java.util.Timer;
import java.util.TimerTask;

class unMutedAutomatically extends TimerTask {

    private final Timer timer;
    private GroupInfo group;
    private ActorRef targetActor;
    private String targetUserName;

    unMutedAutomatically(GroupInfo group, String targetUserName, ActorRef targetActor, Timer timer) {
        this.group = group;
        this.targetUserName = targetUserName;
        this.targetActor = targetActor;
        this.timer = timer;
    }

    @Override
    /**
     * create timer for muted user. once timer ends muted user goes back to user
     */
    public void run() {
        System.out.println("Terminated the Timer Thread!");
        if (group.getUserGroupMode(targetUserName) == GroupInfo.groupMode.MUTED) {
            group.unMuteUser(targetUserName, targetActor);
            targetActor.tell(new Messages.TextMessage(Constants.GROUP_UN_MUTE_AUTO), ActorRef.noSender());
        }
        timer.cancel(); // Terminate the thread
    }
}

Assignment 2
=========================

##### Eran Levav 203394465
##### Yaniv Fleischer 203817002


**all functions are documented.

TODOS:
0. we must write that group leave messages, we understood that broadcast will be done as well
(same as above, in addition:)
1. README - containing the names and ids of the group.
It will also contain the design of the Actor Model you’ve implemented using Akka,
the different actors, hierarchy, implemented behaviors,
and kinds of messages passed and their effect on behavior.
Be sure to detail it to the fullest!


## Client
ClientMain.java is the system actor.
this class reads the keyboard input and sends messages to the manager actor and to other 
target actors.
clientMain is in an infinite loop, it will break it if Server is offline and an exception has been thrown. (unless trying to disconnect)
in the infinite loop, while user is not connected, he can only do disconnected actions wich are:
connect to server.
once connection has been established, a connection flag will be true and user will be able to perform connection actions. (all the rest), or answer "yes","no" to group invitations.

in addition, client Name will be updated and client Actor (ClientActor.java) will be created with the following props:
AtomicBoolean isInviteAnswer - will contain answer from user regarding group invitation
AtomicBoolean expectingInviteAnswer - ClientActor will change this flag to true once an invitation has been recieved so ClientMain will recieve inputs only regarding group invitation answer. ("yes" or "no")
Object waitingObject - ClientActor will wait on this object untill user answered "yes" or "no" on keyboard, and will wake up ClientActor after updating isInviteAnswer with  the answer.

some requests require the manager to validate the request,
meaning the manager checks if all requirments for this request are valid and if so, 
respond to sender with an answer.
the client will take the answer and send to target a message/ connect/ disconnect...




## Server



## How to run

1. run MainServer
2. for every client run ClientMain



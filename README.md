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
1. AtomicBoolean isInviteAnswer - will contain answer from user regarding group invitation
2. AtomicBoolean expectingInviteAnswer - ClientActor will change this flag to true once an invitation has been recieved so ClientMain will recieve inputs only regarding group invitation answer. ("yes" or "no")
3. Object waitingObject - ClientActor will wait on this object untill user answered "yes" or "no" on keyboard, and will wake up ClientActor after updating isInviteAnswer with  the answer.

some requests require the manager to validate the request,
meaning the manager checks if all requirments for this request are valid and if so, 
respond to sender with an answer.
these requests are done by Patterns.ask. after getting the response from the server, 
the client will send to target a message.
(for example, sending a message to a different user, a validation request will be sent to manager, manager will return an error message or an adress message. if its an error -> print error, if its an adrress message, validation was succesfull and message will be sent to the target adress.)

ClientActor will recieve messages from other clients and manager responds that weren't sent by ask.


## Server
MainServer.java creates manager actor.
Manager.java is the manager actor.
manager recieves messages, for every message recieved it validates it.
if validation passed it will act according to to wanted action and send the user a positive response message. (can be an adress message, text message or isSucc message)
if validation fails it will send the user an error message.

the manager handles the following information:
1. UsersMap, maps user name to User Info (explaind in utils)
2. groupsMap, maps group name to Group Info (explaind in utils)


##### unMutedAutomatically
this is a timer, once user has been muted, the timer is activated.
once time expires, if user is still muted, he will be moved back to user.

## Utils
this module contains shared messages, functions and data structors.
#### Groups: 
##### GroupInfo: 
every group is saved as GroupInfo, it has the following information
    1. String groupName: group name
    2. String admin: user whome created the group, can only be one!
    3. List<String> coAdmins:list of co-admins
    4. List<String> mutedusers:list of muted-users
    5. List<String> users:list of users
    6. GroupRouter groupRouter: explained below
  
##### GroupRouter:
GroupRouter creates a unique ActorRef that can broadcast a message to all paths given to him.
it has the following information:
List<String> paths: paths are the adress path.toString() of all users in group that are not muted.
every time getBroadcastRouter(ActorRef) is called, it creates a router that can broadcast messages to all other users in group that are not muted and are not the ActorRef given.
this is because when a client wants to broadcast a message, he should not be one of the recipients.
  
  
#### SharedMessages:
##### Messages:
all comminacation between actors in our project is done by Messages,
they are all Serializable and have final fields.





## How to run

1. run MainServer
2. for every client run ClientMain



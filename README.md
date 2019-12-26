Assignment 2
=========================

##### Eran Levav 203394465
##### Yaniv Fleischer 203817002


**all functions are documented.

In this assignment, We created 3 packages: Client , Server and Utils. 

## Package Client
- ClientMain.java is the system actor.
- ClientActor.java is the client actor.

The ClientMain reads the keyboard input and sends messages to the manager actor and to other target actors. ClientMain is in an infinite loop, it will break if Server is offline and an exception has been thrown (unless trying to disconnect).
In the infinite loop, while user is not connected, he can only perform disconnected actions which are:
    1. connect to server.
Once connection has been established, a connection flag will be turned on and user will be able to perform connection actions. (all the rest), or answer {"yes", "no"} to group invitations.
In addition, client Name will be updated and client Actor (ClientActor.java) will be created with the following props:
    1. AtomicBoolean isInviteAnswer - will contain answer from user regarding group              invitation
    2. AtomicBoolean expectingInviteAnswer - ClientActor will change this flag to true once an invitation has been received so ClientMain will receive inputs only regarding group invitation answer. ("yes" or "no")
    3. Object waitingObject - ClientActor will wait on this object until user answered "yes" or "no" on keyboard, and will wake up ClientActor after updating isInviteAnswer with  the answer.
Some requests require the manager to validate the request,
this means that the manager checks if all requirements for this request are valid and if so, 
responds to sender with an answer. These requests are done by Patterns.ask. After getting the response from the server,  the client will continue its action.
For example, sending a message to a different user. a validation request will be sent to manager, manager will return an error message or an address message. if it's an error -> print error, if it's an address message, validation was successful and message will be sent to the target address.

Client Actor will receive messages from other clients and manager responds that weren't sent by ask.
ClientActor will send and receive group invitation requests.


## Package  Server
- MainServer.java creates manager actor.
- Manager.java is the manager actor.
- unMutedAutomatically.java 

Manager receives messages, for every message received, it validates it.
If validation passed it will act according to the wanted action and send the user a positive response message (can be an address message, text message or isSucc message).
If validation fails it will send the user an error message.
The manager managing the following information:
    1. UsersMap, maps user name to UserInfo (explained in utils) .
    2. groupsMap, maps group name to GroupInfo (explained in utils).
The manager handles these features:
Creating a user (connect, disconnect), Creating a group, Deleting a group.

unMutedAutomatically - This is a timer, once user has been muted, the timer is activated.
once time expires, if user is still muted, he will be moved back to user.

## Package  Utils
This contains shared messages, functions and data structures.

#### Groups: 
##### GroupInfo: 
Every group is saved as GroupInfo, it has the following information
    1. String groupName: group name
    2. String admin: user who created the group, can only be one!
    3. List<String> coAdmins: list of co-admins
    4. List<String> mutedusers: list of muted-users
    5. List<String> users: list of users
    6. GroupRouter groupRouter: explained below
  
##### GroupRouter:
GroupRouter creates a unique ActorRef that can broadcast a message to all paths given to him. It has the following information:
List<String> paths: paths are the address path.toString() of all users in group that are not muted.
Every time getBroadcastRouter(ActorRef) is called, it creates a  unique router that can broadcast messages to all other users in group that are not muted and are not the ActorRef given.
This is because when a client wants to broadcast a message, he should not be one of the recipients.
  
  
#### SharedMessages:
##### Messages:
All communication between actors in our project is done by Messages,
they are all Serializable and have final fields. this gives us a unique way of receiving messages and using their data.
Using the CreateRecieve function, upon receiving a certain message class, it will go to the correct function and all the required fields will be in the message.

#### Users:
##### UserInfo:
for every user, the manager saves a UserInfo containing the following information:
    1.  List<String> groups: list of groups user is in.
    2.  ActorRef actor: ActorRef of this user.

## Additional Information:
- On request /group leave <groupname>, it is written: "same as above"...
  We understood this as also to broadcast the same message.
  This is why, the onGroupLeaveMessage(GroupLeaveMessage leaveMsg) is implemented using switch case, and ADMIN will broadcast admin, coadmin and user message. Coadmin will broadcast coadmin and user message. user will broadcast user message.
  this is what we understood that is requested, to change this is easy and requires an if statement in every case.
- On request /group remove <groupname> <targetuser>, we understood from Previledge described in assignment's instructions that coAdmin can`t remove other coAdmin. 
- On request /group coadmin add / remove <groupname> <targetuser>, we understood from Previledge described in assignment's instructions that co-admin can`t promote/demote other co-admin. 
- On request /group promotes/demote we understood from Previledge described in assignment`s instructions that ADMIN may promote/demote muted/co-admin to co-admin/muted directly.

## How to run

1. run MainServer
2. for every client run ClientMain

Assignment 2
=========================

##### Eran Levav 203394465  <br />
##### Yaniv Fleischer 203817002  <br />


**all functions are documented.  <br />

In this assignment, We created 3 packages: Client , Server and Utils.  <br />

## Package Client
- ClientMain.java is the system actor.  <br />
- ClientActor.java is the client actor.  <br />

The ClientMain reads the keyboard input and sends messages to the manager actor and to other target actors. ClientMain is in an infinite loop, it will break if Server is offline and an exception has been thrown (unless trying to disconnect).  <br />
In the infinite loop, while user is not connected, he can only perform disconnected actions which are:  <br />
    1. connect to server.  <br />
Once connection has been established, a connection flag will be turned on and user will be able to perform connection actions. (all the rest), or answer {"yes", "no"} to group invitations.  <br />
In addition, client Name will be updated and client Actor (ClientActor.java) will be created with the following props:  <br />
    1. AtomicBoolean isInviteAnswer - will contain answer from user regarding group              invitation  <br />
    2. AtomicBoolean expectingInviteAnswer - ClientActor will change this flag to true once an invitation has been received so ClientMain will receive inputs only regarding group invitation answer. ("yes" or "no")  <br />
    3. Object waitingObject - ClientActor will wait on this object until user answered "yes" or "no" on keyboard, and will wake up ClientActor after updating isInviteAnswer with  the answer.  <br />
Some requests require the manager to validate the request,  <br />
this means that the manager checks if all requirements for this request are valid and if so,  <br />
responds to sender with an answer. These requests are done by Patterns.ask. After getting the response from the server,  the client will continue its action.  <br />
For example, sending a message to a different user. a validation request will be sent to manager, manager will return an error message or an address message. if it's an error -> print error, if it's an address message, validation was successful and message will be sent to the target address.  <br />

Client Actor will receive messages from other clients and manager responds that weren't sent by ask.  <br />
ClientActor will send and receive group invitation requests.  <br />


## Package  Server
- MainServer.java creates manager actor.  <br />
- Manager.java is the manager actor.  <br />
- unMutedAutomatically.java   <br />

Manager receives messages, for every message received, it validates it.  <br />
If validation passed it will act according to the wanted action and send the user a positive response message (can be an address message, text message or isSucc message).  <br />
If validation fails it will send the user an error message.  <br />
The manager managing the following information:  <br />
    1. UsersMap, maps user name to UserInfo (explained in utils) .  <br />
    2. groupsMap, maps group name to GroupInfo (explained in utils).  <br />
The manager handles these features:  <br />
Creating a user (connect, disconnect), Creating a group, Deleting a group.  <br />

unMutedAutomatically - This is a timer, once user has been muted, the timer is activated.  <br />
once time expires, if user is still muted, he will be moved back to user.  <br />

## Package  Utils
This contains shared messages, functions and data structures.  <br />

#### Groups: 
##### GroupInfo: 
Every group is saved as GroupInfo, it has the following information  <br />
    1. String groupName: group name  <br />
    2. String admin: user who created the group, can only be one!  <br />
    3. List<String> coAdmins: list of co-admins  <br />
    4. List<String> mutedusers: list of muted-users  <br />
    5. List<String> users: list of users  <br />
    6. GroupRouter groupRouter: explained below  <br />
  
##### GroupRouter:
GroupRouter creates a unique ActorRef that can broadcast a message to all paths given to him. It has the following information:  <br />
List<String> paths: paths are the address path.toString() of all users in group that are not muted.  <br />
Every time getBroadcastRouter(ActorRef) is called, it creates a  unique router that can broadcast messages to all other users in group that are not muted and are not the ActorRef given.  <br />
This is because when a client wants to broadcast a message, he should not be one of the recipients.  <br />
  
  
#### SharedMessages:
##### Messages:
All communication between actors in our project is done by Messages,  <br />
they are all Serializable and have final fields. this gives us a unique way of receiving messages and using their data.  <br />
Using the CreateRecieve function, upon receiving a certain message class, it will go to the correct function and all the required fields will be in the message.  <br />

#### Users:
##### UserInfo:
for every user, the manager saves a UserInfo containing the following information:  <br />
    1.  List<String> groups: list of groups user is in.  <br />
    2.  ActorRef actor: ActorRef of this user.  <br />

## Additional Information:
- On request /group leave <groupname>, it is written: "same as above"...  <br />
  We understood this as also to broadcast the same message.  <br />
  This is why, the onGroupLeaveMessage(GroupLeaveMessage leaveMsg) is implemented using switch case, and ADMIN will broadcast admin, coadmin and user message. Coadmin will broadcast coadmin and user message. user will broadcast user message.  <br />
  this is what we understood that is requested, to change this is easy and requires an if statement in every case.  <br />
- On request /group remove <groupname> <targetuser>, we understood from Previledge described in assignment's instructions that coAdmin can`t remove other coAdmin.  <br />
- On request /group coadmin add / remove <groupname> <targetuser>, we understood from Previledge described in assignment's instructions that co-admin can`t promote/demote other co-admin.  <br />
- On request /group promotes/demote we understood from Previledge described in assignment`s instructions that ADMIN may promote/demote muted/co-admin to co-admin/muted directly. <br />

## How to run
1. go to assignment directory <br />
 a. mvn install <br />
 b. mvn compile <br />
2. go to Utils/  <br />
 a. mvn install <br />
 b. mvn compile <br />
 c. mvn exec:java <br />
3. go to Server/  <br />
 a. mvn install <br />
 b. mvn compile <br />
 c. mvn exec:java <br />
4. go to Client/ 
 a. mvn install <br />
 b. mvn compile <br />
 c. mvn exec:java <br />
5. for every additional client, in Client/ do:  <br />
 mvn exec:java <br />

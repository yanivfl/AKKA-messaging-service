Akka Remote Example in Java
=========================

TODOS:
0. we must write that group leave messages, we understood that broadcast will be done as well
(same as above, in addition:)
1. README - containing the names and ids of the group.
It will also contain the design of the Actor Model you’ve implemented using Akka,
the different actors, hierarchy, implemented behaviors,
and kinds of messages passed and their effect on behavior.
Be sure to detail it to the fullest!

2. kill clientref when disconnected, poison pill or something else.
 - done - poisonpill, also killed all routers after broadcast. deleted killing after
 server disconnect. 
 
3. document all fucntions
4. eliminate logs after client disconnect. other clients in group get info logs - done
5. reuse send to server request. - ndone
6. in manager, all for loops -> stream.map - done

##### Client

##### Server

## How to run
You can run the program like every ordinary Java main program. Make sure you have `mvn clean install`ed the project before to get the Akka dependency.
It's important to run the projects in the following order:

1. LoggerEnvironment
2. Server
3. Client

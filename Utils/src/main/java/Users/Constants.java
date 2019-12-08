package Users;

public class Constants{

    public static final String MANAGER = "Manager";
    public static final String SERVER= "WhatsupServer";
    public static final String CLIENT= "Client";
    public static final String PATH_TO_MANAGER = "akka.tcp://"+ SERVER + "@127.0.0.1:3553/user/"+MANAGER;



    public static final String SERVER_IS_OFFLINE=  "server is offline!";
    public static final String CONNECT_FAIL(String username){ return username +  " is in use!";}
    public static final String CONNECT_SUCC(String username){ return username +  " is in use!";}

}


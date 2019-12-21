package Users;

import SharedMessages.Messages;

import java.io.File;
import java.nio.file.Paths;

public class SharedFucntions {

    /***
     * get targetfilepath, create directories if needed
     * @param userName
     * @return target path name
     */
    public static String getTargetFilePath(String userName, String fileName){
        String outputpath =  Paths.get( Constants.CHAT_DOWNLOADS_DIR, userName).toString();
        new File(outputpath).mkdirs();
        return Paths.get( outputpath, fileName).toString();
    }
}

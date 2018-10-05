package duplicator;

import duplicator.serives.SynchronizerAPI;
import duplicator.settings.FixedSettings;
import duplicator.settings.Settings;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Duplicator {

    public static void main(String[] args) {
        List<String> sourceFolderList = null;
        String destFolder = ""; 
        Settings settings = Settings.getInstance();
        if (args.length == 0) {
            sourceFolderList = settings.getList(FixedSettings.SOURCE_FOLDER);
            destFolder = settings.get(FixedSettings.DESTINATION_FOLDER);
        }
        if (sourceFolderList == null || sourceFolderList.isEmpty()
                || destFolder == null || destFolder.isEmpty()) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Exiting, one of paths is empty. Source folder is = "
                    + sourceFolderList + ", destination folder is " + destFolder);
            System.exit(1);
        }

        Duplicator duplicator = new Duplicator();
        duplicator.diffCopy(sourceFolderList, destFolder);
    }

    void diffCopy(List<String> sourceFolderList, String destFolder) {
        SynchronizerAPI synchronizerAPI = new SynchronizerAPI();
        synchronizerAPI.diffCopy(sourceFolderList, destFolder);
    }
}

package duplicator;

import com.sun.org.apache.bcel.internal.generic.DLOAD;
import duplicator.log.DLogger;
import duplicator.serives.SynchronizerAPI;
import duplicator.settings.FixedSettings;
import duplicator.settings.Settings;

import java.util.List;
import java.util.logging.Level;

public class Duplicator {

    public static void main(String[] args) {
        try {
            List<String> sourceFolderList = null;
            List<String> sourceFileList = null;
            List<String> sourceFileZipList = null;
            String destFolder = "";
            Settings settings = Settings.getInstance();
            if (args.length == 0) {
                sourceFolderList = settings.getList(FixedSettings.SOURCE_FOLDER);
                sourceFileList = settings.getList(FixedSettings.SOURCE_FILE);
                sourceFileZipList = settings.getList(FixedSettings.SOURCE_FILE_ZIP);
                destFolder = settings.get(FixedSettings.DESTINATION_FOLDER);
            }
            if (sourceFolderList == null || sourceFolderList.isEmpty()
                    || destFolder == null || destFolder.isEmpty()) {
                DLogger.getAnonymousLogger().log(Level.SEVERE, "Exiting, one of paths is empty. Source folder is = "
                        + sourceFolderList + ", destination folder is " + destFolder);
                System.exit(1);
            }

            Duplicator duplicator = new Duplicator();
            duplicator.diffCopy(sourceFolderList, sourceFileList, sourceFileZipList, destFolder);
        } finally {
            DLogger.shutdown();
        }
    }

    void diffCopy(List<String> sourceFolderList, List<String> sourceFileList, List<String> sourceFileZipList, String destFolder) {
        SynchronizerAPI synchronizerAPI = SynchronizerAPI.newInstanceDefaultLog();
        synchronizerAPI.diffCopy(sourceFolderList, sourceFileList, sourceFileZipList, destFolder);
    }
}

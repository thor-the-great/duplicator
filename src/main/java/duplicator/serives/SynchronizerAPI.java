package duplicator.serives;

import duplicator.log.DLogger;
import duplicator.log.LOGGER_TYPE;
import duplicator.pojo.FileObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SynchronizerAPI {

    final static String _ILLEGAL_CHARS_IN_FILENAME = "[\\\\/:*?\"<>|]";

    private final static DLogger _MAIN_LOGGER = DLogger.getInstance(SynchronizerAPI.class.getName() + " Main events", LOGGER_TYPE.MAIN);

    private static SynchronizerAPI instance;

    public static SynchronizerAPI newInstanceDefaultLog() {
        if (instance == null) {
            instance = new SynchronizerAPI();
        }
        return instance;
    }

    public void diffCopy(List<String> sourceFolderList, List<String> sourceFileList, List<String> sourceFileZipList, String destinationFolder) {
        try {
            //check for multiple identical source folders (lower level folder name can be the same => @destination it will be a problem, folders with be merged)
            Map<String, String> folderToUniqueFolderMap = getFolderMapping(sourceFolderList);
            HashMap<String, FileObject> destinationFolderObjectMap = getMapOfFilesInFolder(destinationFolder, folderToUniqueFolderMap);
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
            getRollingLogger();
            for (String sourceFolder : sourceFolderList) {
                HashMap<String, FileObject> sourceFolderObjectMap = getMapOfFilesInFolder(sourceFolder, true, folderToUniqueFolderMap);
                String msg = "Processing folder " + sourceFolder + " with " + sourceFolderObjectMap.size() + " object(s)";
                DLogger.getAnonymousLogger().log(Level.INFO, msg);
                _MAIN_LOGGER.log(Level.INFO, msg);
                //iterate over source folder content, check if the same object in destination, if it's missing on not
                //up-to-date copy it (overwrite if needed)
                File sourceFolderFile = new File(sourceFolder);
                File destinationFolderFile = new File(destinationFolder);
                for (String sourceKey : sourceFolderObjectMap.keySet()) {
                    exec.execute(()->{
                        //check if file need to be copied
                        if (needCopyFile(sourceFolderObjectMap, destinationFolderObjectMap, sourceKey)) {
                            try {
                                //create file object (not the file in fs)
                                File destinationFile = createDestinationFileObject(sourceFolderObjectMap, destinationFolderObjectMap, sourceKey,
                                        sourceFolderFile, destinationFolderFile, folderToUniqueFolderMap);
                                //do actual file copy operation
                                FileUtils.copyFile(sourceFolderObjectMap.get(sourceKey).getPath().toFile(), destinationFile);
                            } catch (IOException e) {
                                String msg1 = new Timestamp(System.currentTimeMillis()) + " - Exception while copying file " + sourceKey + ". Exception is " + e;
                                DLogger.getAnonymousLogger().log(Level.SEVERE, msg1);
                                _MAIN_LOGGER.log(Level.SEVERE, msg1);
                            }
                            String msgCopied = "Copied file " + sourceKey;
                            try {
                                getRollingLogger().log(Level.INFO, msgCopied);
                            } catch (IOException e) {
                                String msgEx = new Timestamp(System.currentTimeMillis()) + " - Can't synchronize folders. Exception is " + e;
                                DLogger.getAnonymousLogger().log(Level.SEVERE, msgEx);
                                _MAIN_LOGGER.log(Level.SEVERE, msgEx);
                            }
                        }
                    });
                }
            }
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.MINUTES);
            Path destinationFilesFolderPath = Paths.get(destinationFolder + File.separator + "files");
            for (String sourceFile : sourceFileList) {
                String msg = "Processing file " + sourceFile;
                DLogger.getAnonymousLogger().log(Level.INFO, msg);
                _MAIN_LOGGER.log(Level.INFO, msg);
                //up-to-date copy it (overwrite if needed)
                File sourceFileFile = new File(sourceFile);
                Path destFilePath = Paths.get(destinationFilesFolderPath.toString(), sourceFileFile.toPath().getFileName().toString());
                if (isNeedToCopy(sourceFileFile, destFilePath)) {
                    FileUtils.copyFile(sourceFileFile, destFilePath.toFile());
                }
            }

            for (String sourceFile : sourceFileZipList) {
                String msg = "Processing file with zip option " + sourceFile;
                DLogger.getAnonymousLogger().log(Level.INFO, msg);
                _MAIN_LOGGER.log(Level.INFO, msg);
                //up-to-date copy it (overwrite if needed)
                File sourceFileFile = new File(sourceFile);
                Path sourceFilePath = sourceFileFile.toPath();

                File outputSourceZipFile = new File(sourceFile + ".zip");
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputSourceZipFile));
                ZipEntry zipEntry = new ZipEntry(sourceFilePath.getFileName().toString());
                zos.putNextEntry(zipEntry);

                FileInputStream in = new FileInputStream(sourceFile);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                in.close();
                zos.closeEntry();
                zos.close();

                Path destFilePath = Paths.get(destinationFilesFolderPath.toString(), outputSourceZipFile.toPath().getFileName().toString());
                FileUtils.copyFile(outputSourceZipFile, destFilePath.toFile());
                FileUtils.forceDelete(outputSourceZipFile);
            }
        } catch (IOException e) {
            String msgEx = new Timestamp(System.currentTimeMillis()) + " - Can't synchronize folders. Exception is " + e;
            DLogger.getAnonymousLogger().log(Level.SEVERE, msgEx);
            _MAIN_LOGGER.log(Level.SEVERE, msgEx);
            System.exit(1);
        } catch (InterruptedException e) {
            String msgEx = new Timestamp(System.currentTimeMillis()) + " - Can't synchronize folders. Exception is " + e;
            DLogger.getAnonymousLogger().log(Level.SEVERE, msgEx);
            _MAIN_LOGGER.log(Level.SEVERE, msgEx);
            System.exit(1);
        }
    }

    private boolean isNeedToCopy(File sourceFileFile, Path destFilePath) throws IOException {
        if (!destFilePath.toFile().exists()) {
            return true;
        } else {
            File destFile = destFilePath.toFile();
            return (sourceFileFile.lastModified() != destFile.lastModified()
                    || Files.size(destFilePath) != Files.size(sourceFileFile.toPath()));
        }
    }

    private DLogger getRollingLogger() throws IOException {
        return DLogger.getInstance(SynchronizerAPI.class.getName() + " Rolling operations", LOGGER_TYPE.ROLLING);
    }

    /**
     * Return map with path-to-folder - unique_folder_name. We need unique folder names for cases when several paths in
     * source have same lower level folder, otherwise it will be the same folder name in destination
     *
     * @param sourceFolderList
     * @return
     */
    private Map<String, String> getFolderMapping(List<String> sourceFolderList) {
        Map<String, String> folderToUniqueFolderMap = new HashMap<>();
        Map<String, LinkedList<String>> folderToFolderPathsMap = new HashMap<>();
        for (String sourceFolder : sourceFolderList) {
            Path sourceFolderPath = new File(sourceFolder).toPath();
            String folder = sourceFolderPath.getName(sourceFolderPath.getNameCount() - 1).toString();
            if (!folderToFolderPathsMap.containsKey(folder)) {
                LinkedList paths = new LinkedList();
                paths.add(sourceFolder);
                folderToFolderPathsMap.put(folder, paths);
            } else {
                folderToFolderPathsMap.get(folder).add(sourceFolder);
            }
        }

        for (String folder: folderToFolderPathsMap.keySet()) {
            LinkedList<String> paths = folderToFolderPathsMap.get(folder);
            if (paths.size() == 1) {
                String folderPath = paths.getFirst();
                folderToUniqueFolderMap.put(folderPath, folder);
            } else {
                for (String folderPath : paths) {
                    Path folderPathAsPath = new File(folderPath).toPath();
                    String parentFolder = folderPathAsPath.getRoot().toString();
                    parentFolder = parentFolder.replaceAll(_ILLEGAL_CHARS_IN_FILENAME, "");
                    if (folderPathAsPath.getNameCount() > 1) {
                        parentFolder = folderPathAsPath.getName(folderPathAsPath.getNameCount() - 2).toString();
                    }
                    String newFolder = folder + "_" + parentFolder;
                    folderToUniqueFolderMap.put(folderPath, newFolder);
                }
            }
        }
        return folderToUniqueFolderMap;
    }

    private boolean needCopyFile(HashMap<String, FileObject> sourceMap, HashMap<String, FileObject> destMap, String sourceKey) {
        //if file not in destination - copy it
        if (!destMap.containsKey(sourceKey))
            return true;
        //if file is there, but it's different in size or modified datetime is different - copy it as well
        FileObject sourceFO = sourceMap.get(sourceKey);
        FileObject destinationFO = destMap.get(sourceKey);
        if ((sourceFO.getSize() != destinationFO.getSize())
                || (sourceFO.getModifTime().compareTo(destinationFO.getModifTime()) != 0))
            return true;
        //fallback is not to copy
        return false;
    }

    File createDestinationFileObject(HashMap<String, FileObject> sourceMap, HashMap<String, FileObject> destMap,
                                     String sourceKey, File sourceFolderFile, File destinationFolderFile, Map<String, String> folderToUniqueFolderMap) {
        if (destMap.containsKey(sourceKey))
            return destMap.get(sourceKey).getPath().toFile();
        else {
            Path sourcePath = sourceMap.get(sourceKey).getPath();
            String folderMapped = folderToUniqueFolderMap.get(sourceFolderFile.toString());
            Path relativeFilePath = sourcePath.subpath(sourceFolderFile.toPath().getNameCount(), sourcePath.getNameCount());
            Path newDestinationPath = Paths.get(destinationFolderFile.getAbsolutePath(), folderMapped, relativeFilePath.toString());
            return newDestinationPath.toFile();
        }
    }

    private HashMap<String, FileObject> getMapOfFilesInFolder(String sourceFolder, Map<String, String> folderToUniqueFolderMap) throws IOException {
        return getMapOfFilesInFolder(sourceFolder, false, folderToUniqueFolderMap);
    }

    private HashMap<String, FileObject> getMapOfFilesInFolder(String folder, boolean includeParent, Map<String, String> folderToUniqueFolderMap) throws IOException {
        HashMap<String, FileObject> diffCache = new HashMap<>();
        File sourceRootFolder = new File(folder);
        if (!sourceRootFolder.exists()) {
            DLogger.getAnonymousLogger().log(Level.SEVERE, "Folder does not exist - " + folder);
            throw new IOException("Folder does not exist - " + folder);
        }
        Iterator it = FileUtils.iterateFiles(sourceRootFolder, null, true);
        while(it.hasNext()) {
            File file = (File) it.next();
            FileObject newFO = new FileObject.Builder()
                    .path(file.toPath())
                    .isFolder(file.isDirectory())
                    .modifTime(FileTime.fromMillis(file.lastModified()))
                    .size(Files.size(file.toPath()))
                    .build();
            String key = getFileRelativePathKey(sourceRootFolder, file,  includeParent, folderToUniqueFolderMap);
            diffCache.put(key, newFO);
        }
        return diffCache;
    }

    private String getFileRelativePathKey(File sourceRootFolder, File file,  boolean includeParent, Map<String, String> folderToUniqueFolderMap) {
        Path rootFolderPath = sourceRootFolder.toPath();
        Path filePath = file.toPath();
        Path relativePathPart = filePath.subpath(rootFolderPath.getNameCount(), filePath.getNameCount());
        if (includeParent) {
            String folderMappedName = folderToUniqueFolderMap.get(sourceRootFolder.toString());
            relativePathPart = Paths.get(folderMappedName, relativePathPart.toString());
        }
        return relativePathPart.toString();
    }
}

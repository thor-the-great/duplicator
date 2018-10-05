package duplicator.serives;

import duplicator.pojo.FileObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynchronizerAPI {

    final static String _ILLEGAL_CHARS_IN_FILENAME = "[\\\\/:*?\"<>|]";

    public void diffCopy(List<String> sourceFolderList, String destinationFolder) {
        try {
            //check for multiple identical source folders (lower level folder name can be the same => @destination it will be a problem, folders with be merged)
            Map<String, String> folderToUniqueFolderMap = getFolderMapping(sourceFolderList);
            HashMap<String, FileObject> destinationFolderObjectMap = getMapOfFilesInFolder(destinationFolder, folderToUniqueFolderMap);
            for (String sourceFolder : sourceFolderList) {
                HashMap<String, FileObject> sourceFolderObjectMap = getMapOfFilesInFolder(sourceFolder, true, folderToUniqueFolderMap);
                Logger.getAnonymousLogger().log(Level.INFO, "Processing folder " + sourceFolder +" with " + sourceFolderObjectMap.size() +" object(s)");
                //iterate over source folder content, check if the same object in destination, if it's missing on not
                //up-to-date copy it (overwrite if needed)
                File sourceFolderFile = new File(sourceFolder);
                File destinationFolderFile = new File(destinationFolder);
                for (String sourceKey : sourceFolderObjectMap.keySet()) {
                    //check if file need to be copied
                    if (needCopyFile(sourceFolderObjectMap, destinationFolderObjectMap, sourceKey, folderToUniqueFolderMap)) {
                        try {
                            File destinationFile = createDestinationFile(sourceFolderObjectMap, destinationFolderObjectMap, sourceKey,
                                    sourceFolderFile, destinationFolderFile, folderToUniqueFolderMap);
                            FileUtils.copyFile(sourceFolderObjectMap.get(sourceKey).getPath().toFile(), destinationFile);
                        } catch (IOException e) {
                            System.err.println("Exception while copying file " + sourceKey + ". Exception is " + e);
                            Logger.getAnonymousLogger().log(Level.SEVERE, "Exception while copying file " + sourceKey + ". Exception is " + e);
                        }
                        System.out.println("Copied file " + sourceKey);
                    }
                }
            }
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Can't synchronize folders. Exception is " + e);
            System.exit(1);
        }
    }

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

    private boolean needCopyFile(HashMap<String, FileObject> sourceMap, HashMap<String, FileObject> destMap, String sourceKey, Map<String, String> folderToUniqueFolderMap) {
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

    File createDestinationFile(HashMap<String, FileObject> sourceMap, HashMap<String, FileObject> destMap,
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

    private HashMap<String, FileObject> getMapOfFilesInFolder(String sourceFolder, boolean includeParent, Map<String, String> folderToUniqueFolderMap) throws IOException {
        HashMap<String, FileObject> diffCache = new HashMap<>();
        File sourceRootFolder = new File(sourceFolder);
        if (!sourceRootFolder.exists()) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Folder does not exist - " + sourceFolder);
            throw new IOException("Folder does not exist - " + sourceFolder);
        }
        Iterator it = FileUtils.iterateFiles(sourceRootFolder, null, true);
        while(it.hasNext()) {
            File file = (File) it.next();
            FileObject newFO = new FileObject();
            newFO.setPath(file.toPath());
            newFO.setFolder(file.isDirectory());
            newFO.setModifTime(FileTime.fromMillis(file.lastModified()));
            try {
                newFO.setSize(Files.size(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
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

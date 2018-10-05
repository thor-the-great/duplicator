package duplicator.pojo;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileObject {
    boolean isFolder;
    long size;
    FileTime modifTime;
    Path path;

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileTime getModifTime() {
        return modifTime;
    }

    public void setModifTime(FileTime modifTime) {
        this.modifTime = modifTime;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}

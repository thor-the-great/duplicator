package duplicator.pojo;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileObject {
    boolean isFolder;
    long size;
    FileTime modifTime;
    Path path;

    private FileObject(Builder builder) {
        isFolder = builder.isFolder;
        size = builder.size;
        modifTime = builder.modifTime;
        path = builder.path;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public long getSize() {
        return size;
    }

    public FileTime getModifTime() {
        return modifTime;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public static class Builder {
        boolean isFolder;
        long size;
        FileTime modifTime;
        Path path;

        public Builder isFolder(boolean folder) {
            this.isFolder = folder;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder modifTime(FileTime modifTime) {
            this.modifTime = modifTime;
            return this;
        }

        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public FileObject build() {
            return new FileObject(this);
        }
    }
}

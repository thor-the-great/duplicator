package duplicator.settings;

public enum FixedSettings {
    SOURCE_FOLDER("source.folder"),
    SOURCE_FILE("source.file"),
    SOURCE_FILE_ZIP("source.file.zip"),
    DESTINATION_FOLDER("destination.folder");

    private String val;

    FixedSettings(String val) {
        this.val = val;
    }

    String getVal(){
        return this.val;
    }
}

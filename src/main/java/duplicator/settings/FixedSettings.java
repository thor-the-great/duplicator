package duplicator.settings;

public enum FixedSettings {
    SOURCE_FOLDER("source.folder"), SOURCE_FILE("source.file"), DESTINATION_FOLDER("destination.folder");

    private String val;

    FixedSettings(String val) {
        this.val = val;
    }

    String getVal(){
        return this.val;
    }
}

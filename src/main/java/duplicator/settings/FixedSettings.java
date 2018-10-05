package duplicator.settings;

public enum FixedSettings {
    SOURCE_FOLDER("source.folder"), DESTINATION_FOLDER("destination.folder");

    private String val;

    FixedSettings(String val) {
        this.val = val;
    }

    String getVal(){
        return this.val;
    }
}

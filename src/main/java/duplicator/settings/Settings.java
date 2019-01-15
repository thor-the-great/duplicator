package duplicator.settings;

import duplicator.DLogger;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;


public class Settings {
    static Settings instance;
    Configuration config;

    private Settings() {
        File settingsFile = new File("./settings.properties");
        if (!settingsFile.exists()) {
            DLogger.getAnonymousLogger().log(Level.SEVERE, "Can't find file with main settings, exiting");
            System.exit(1);
        }
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties()
                                .setFile(settingsFile)
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
        try
        {
            config = builder.getConfiguration();
        }
        catch(ConfigurationException cex)
        {
            // loading of the configuration file failed
        }
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public String get(FixedSettings setting) {
        if (config != null) {
            return config.getString(setting.getVal());
        }
        return "";
    }

    public List<String> getList(FixedSettings setting) {
        if (config != null) {
            List<Object> settings = config.getList(setting.getVal());
            List<String> ret = new ArrayList<>();
            for(Object settingObj : settings)
                ret.add(settingObj.toString());
            return ret;
        }
        return Collections.EMPTY_LIST;
    }
}


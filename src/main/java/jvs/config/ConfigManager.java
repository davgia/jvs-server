package jvs.config;

import io.vertx.core.json.JsonObject;
import jvs.utils.JsonUtils;
import jvs.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Configuration manager used to read server configuration
 */
public class ConfigManager {

    private static Configuration config = null;

    /**
     * Imports the configuration values from a json file.
     * @param path The path to the json file.
     */
    public static void importConfig(final String path) {
        File f = new File(path);
        if(f.exists() && !f.isDirectory()) {
            try{
                JsonObject jsonObject = JsonUtils.readJsonFromFile(path);
                if (jsonObject != null) {
                    config = new Configuration(jsonObject);
                }
            } catch (Exception e) {
                Logger.error("Unable to import configuration from config file:");
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the configuration
     * @return The configuration object.
     */
    public static Configuration getConfig() {
        return config;
    }

    /**
     * Method to check if the configuration is available.
     * @return True, if the configuration is available; otherwise false.
     */
    public static Boolean isConfigAvailable() {
        return config != null;
    }
}

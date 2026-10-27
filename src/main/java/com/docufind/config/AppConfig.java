package com.docufind.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and provides application configuration settings.
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // Default configuration values
        properties.setProperty("db.url", "jdbc:oracle:thin:@localhost:1521:orcl");
        properties.setProperty("db.user", "system");
        properties.setProperty("db.password", "livewire");
        properties.setProperty("app.name", "DocuFind - Offline Personal Search Engine");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("app.max_file_size_mb", "25");
        properties.setProperty("app.preview_char_limit", "3000");
        properties.setProperty("app.search_max_results", "500");

        File configFile = new File("config/application.properties");
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                properties.load(in);
                LOGGER.info("Loaded configuration from " + configFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read configuration file: " + configFile.getAbsolutePath(), e);
            }
        } else {
            LOGGER.info("Config file not found at " + configFile.getAbsolutePath() + "; using default settings.");
        }

        // Allow environment variable overrides
        String envDbUrl = System.getenv("DOCUFIND_DB_URL");
        if (envDbUrl != null && !envDbUrl.isBlank()) {
            properties.setProperty("db.url", envDbUrl);
        }
        String envDbUser = System.getenv("DOCUFIND_DB_USER");
        if (envDbUser != null && !envDbUser.isBlank()) {
            properties.setProperty("db.user", envDbUser);
        }
        String envDbPass = System.getenv("DOCUFIND_DB_PASSWORD");
        if (envDbPass != null && !envDbPass.isBlank()) {
            properties.setProperty("db.password", envDbPass);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            String val = properties.getProperty(key);
            return val != null ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        try {
            String val = properties.getProperty(key);
            return val != null ? Long.parseLong(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
    }
}

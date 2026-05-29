package com.empresa.rcp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PreferencesManager {

    private static final Logger log = LoggerFactory.getLogger(PreferencesManager.class);
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.rcpgestion/config";
    private static final String FILE_NAME = "workspace.properties";
    private static final File configFile = new File(CONFIG_DIR, FILE_NAME);

    private static final Properties props = new Properties();

    static {
        loadPreferences();
    }

    private static void loadPreferences() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                log.info("Preferencias del espacio de trabajo cargadas con éxito.");
            } catch (IOException e) {
                log.error("Error al cargar preferencias del espacio de trabajo: {}", e.getMessage(), e);
            }
        }
    }

    public static void savePreference(String key, String value) {
        props.setProperty(key, value);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "RCP Gestion Workspace Properties");
        } catch (IOException e) {
            log.error("Error al guardar preferencia ({}={}): {}", key, value, e.getMessage(), e);
        }
    }

    public static String getPreference(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static double getDoublePreference(String key, double defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanPreference(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }
}

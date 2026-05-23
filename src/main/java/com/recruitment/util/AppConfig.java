package com.recruitment.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Application configuration loader.
 * Reads settings from data/config.properties at runtime.
 */
public class AppConfig {

    private static final String CONFIG_FILE = "data/config.properties";
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    private static void load() {
        if (loaded) return;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("[AppConfig] Could not load config.properties: " + e.getMessage());
        }
        loaded = true;
    }

    public static String get(String key, String defaultValue) {
        load();
        return props.getProperty(key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        load();
        String v = props.getProperty(key);
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    public static String getDeepSeekApiKey() {
        return get("deepseek.api.key", "");
    }

    public static String getDeepSeekUrl() {
        return get("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions");
    }

    public static String getDeepSeekModel() {
        return get("deepseek.model", "deepseek-chat");
    }

    public static boolean isDeepSeekEnabled() {
        return getBoolean("deepseek.enabled", false)
                && !getDeepSeekApiKey().isEmpty();
    }
}

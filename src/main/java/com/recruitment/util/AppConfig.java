package com.recruitment.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Application configuration loader.
 * <p>
 * Reads settings lazily from {@code data/config.properties} on first access.
 * Missing or unreadable files are logged to stderr; callers receive default values.
 * Reads settings from data/config.properties at runtime.
 */
public class AppConfig {

    private static final String CONFIG_FILE = "data/config.properties";
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    /** Loads {@code config.properties} once if not already loaded. */
    private static void load() {
        if (loaded) return;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("[AppConfig] Could not load config.properties: " + e.getMessage());
        }
        loaded = true;
    }

    /**
     * Returns a string property value.
     *
     * @param key          property key
     * @param defaultValue value used when the key is absent or the file failed to load
     * @return configured value or {@code defaultValue}
     */
    public static String get(String key, String defaultValue) {
        load();
        return props.getProperty(key, defaultValue);
    }

    /**
     * Returns a boolean property value.
     *
     * @param key          property key
     * @param defaultValue value used when the key is absent or not parseable as boolean
     * @return configured flag or {@code defaultValue}
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        load();
        String v = props.getProperty(key);
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    /**
     * @return DeepSeek API key from {@code deepseek.api.key}, or empty string if unset
     */
    public static String getDeepSeekApiKey() {
        return get("deepseek.api.key", "");
    }

    /**
     * @return DeepSeek chat completions endpoint URL
     */
    public static String getDeepSeekUrl() {
        return get("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions");
    }

    /**
     * @return DeepSeek model name (e.g. {@code deepseek-chat})
     */
    public static String getDeepSeekModel() {
        return get("deepseek.model", "deepseek-chat");
    }

    /**
     * Returns whether AI features should be active.
     * <p>
     * Requires {@code deepseek.enabled=true} and a non-empty API key.
     *
     * @return {@code true} when DeepSeek integration is configured and enabled
     */
    public static boolean isDeepSeekEnabled() {
        return getBoolean("deepseek.enabled", false)
                && !getDeepSeekApiKey().isEmpty();
    }
}

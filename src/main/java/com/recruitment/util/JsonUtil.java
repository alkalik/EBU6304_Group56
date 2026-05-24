package com.recruitment.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON persistence helper for the application's file-based data store.
 * <p>
 * Reads and writes UTF-8 JSON files under the {@code data/} directory using Gson
 * with pretty printing. {@link LocalDateTime} values are serialised in ISO-8601
 * local date-time format. Missing files yield empty lists rather than throwing.
 */
public class JsonUtil {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private static final String DATA_DIR = "data";

    static {
        Path dataPath = Paths.get(DATA_DIR);
        if (!Files.exists(dataPath)) {
            try {
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gson type adapter for {@link LocalDateTime} using {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    static {
        Path dataPath = Paths.get(DATA_DIR);
        if (!Files.exists(dataPath)) {
            try {
                Files.createDirectories(dataPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deserialises a JSON array from {@code data/<filename>} into a list.
     *
     * @param <T>      element type
     * @param filename file name relative to the data directory
     * @param type     Gson {@link Type} token for {@code List<T>} (e.g. from {@code TypeToken})
     * @return populated list, or an empty list if the file is missing, empty, or unreadable
     */
    public static <T> List<T> loadList(String filename, Type type) {
        Path filePath = Paths.get(DATA_DIR, filename);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath.toFile()), StandardCharsets.UTF_8)) {
            List<T> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Serialises a list to {@code data/<filename>} as pretty-printed JSON.
     * <p>
     * Creates parent directories if needed. IO failures are logged to stderr and swallowed.
     *
     * @param <T>      element type
     * @param filename file name relative to the data directory
     * @param list     list to persist (may be empty)
     */
    public static <T> void saveList(String filename, List<T> list) {
        Path filePath = Paths.get(DATA_DIR, filename);
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath.toFile()), StandardCharsets.UTF_8)) {
                gson.toJson(list, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serialises an arbitrary object to a JSON string.
     *
     * @param obj object to convert (may be {@code null}, which yields {@code "null"})
     * @return JSON representation
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Deserialises a JSON string into an instance of the given class.
     *
     * @param <T>   target type
     * @param json  JSON source string
     * @param clazz class of the desired object
     * @return deserialized instance, or {@code null} if {@code json} is {@code null}
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}

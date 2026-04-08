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
import com.google.gson.reflect.TypeToken;

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

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}

package com.recruitment.util;

import java.util.UUID;

public class IDGenerator {
    public static String generate() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generate(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

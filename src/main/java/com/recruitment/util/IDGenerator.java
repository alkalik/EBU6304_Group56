package com.recruitment.util;

import java.util.UUID;

/**
 * Utility for generating short, human-readable unique identifiers.
 * <p>
 * Identifiers are derived from the first eight characters of a random UUID,
 * optionally prefixed for entity-specific namespaces (e.g. {@code USR-}, {@code JOB-}).
 */
public class IDGenerator {

    /**
     * Generates an eight-character alphanumeric id without a prefix.
     *
     * @return short unique id string
     */
    public static String generate() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generates a prefixed id in the form {@code prefix-xxxxxxxx}.
     *
     * @param prefix namespace prefix (e.g. {@code "USR"} or {@code "APP"})
     * @return prefixed unique id string
     */
    public static String generate(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

package com.recruitment.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing and verification using PBKDF2-HMAC-SHA256.
 * <p>
 * New passwords are stored in a self-describing string format:
 * {@code pbkdf2$<iterations>$<base64-salt>$<base64-hash>}.
 * {@link #matches(String, String)} also accepts legacy plaintext stored values
 * for one-time migration after successful login.
 */
public class PasswordUtil {
    private static final String SCHEME = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom secureRandom = new SecureRandom();

    /** Prevents instantiation. */
    private PasswordUtil() {}

    /**
     * Hashes a plaintext password with a freshly generated salt.
     *
     * @param password plaintext password to hash
     * @return encoded hash string in {@code pbkdf2$...} format
     * @throws IllegalArgumentException if {@code password} is {@code null}
     * @throws IllegalStateException    if the JCE provider cannot perform PBKDF2
     */
    public static String hashPassword(String password) {
        if (password == null) throw new IllegalArgumentException("password is null");
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        return SCHEME
                + "$" + ITERATIONS
                + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derived);
    }

    /**
     * Verifies a candidate password against a stored value.
     * <p>
     * Supports PBKDF2-encoded hashes and legacy plaintext (exact string equality).
     *
     * @param password candidate plaintext password
     * @param stored   value from persistence (hashed or legacy plaintext)
     * @return {@code true} if the password matches; {@code false} if either argument is
     *         {@code null}, the stored format is invalid, or verification fails
     */
    public static boolean matches(String password, String stored) {
        if (password == null || stored == null) return false;
        if (!stored.startsWith(SCHEME + "$")) {
            // Legacy plaintext fallback (will be migrated by the caller after a successful login)
            return stored.equals(password);
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) return false;
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * Returns whether the stored value uses the PBKDF2 encoding scheme.
     *
     * @param stored value from persistence
     * @return {@code true} if {@code stored} starts with {@code pbkdf2$}
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(SCHEME + "$");
    }

    /**
     * Derives key bytes via PBKDF2.
     *
     * @throws IllegalStateException if key derivation fails
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}


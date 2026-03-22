package com.recruitment.util;

public class PasswordUtilSelfTest {
    public static void main(String[] args) {
        String password = "pass123";

        String h1 = PasswordUtil.hashPassword(password);
        String h2 = PasswordUtil.hashPassword(password);

        if (h1.equals(password)) throw new AssertionError("Hash must not equal plaintext");
        if (!h1.startsWith("pbkdf2$")) throw new AssertionError("Hash scheme prefix missing");
        if (h1.equals(h2)) throw new AssertionError("Hashes should differ due to random salt");

        if (!PasswordUtil.matches(password, h1)) throw new AssertionError("Correct password must match hash");
        if (PasswordUtil.matches("wrongpass", h1)) throw new AssertionError("Wrong password must not match hash");

        // Legacy plaintext compatibility check (for migration)
        if (!PasswordUtil.matches("abc", "abc")) throw new AssertionError("Legacy plaintext should match itself");
        if (PasswordUtil.matches("abc", "abd")) throw new AssertionError("Legacy plaintext should not match different");

        System.out.println("PasswordUtilSelfTest OK");
    }
}


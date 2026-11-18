package com.spendly.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utility for cryptographic password hashing and verification.
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte cryptographically secure random salt in hex format.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes password with salt using SHA-256.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies plain text password against stored salt and expected hash.
     */
    public static boolean verifyPassword(String plainPassword, String salt, String expectedHash) {
        if (plainPassword == null || salt == null || expectedHash == null) {
            return false;
        }
        String computedHash = hashPassword(plainPassword, salt);
        return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

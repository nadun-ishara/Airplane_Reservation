package com.airline.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility for BCrypt password hashing and verification.
 * Provides progressive migration support for legacy plain-text passwords.
 */
public class PasswordUtil {

    private static final int LOG_ROUNDS = 12;

    /**
     * Hashes a plain-text password using BCrypt with a workload factor of 12.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Checks whether a plain-text password matches a stored password.
     * Supports both BCrypt hashes ($2a$, $2b$, $2y$) and legacy plain-text values.
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        if (isBCryptHash(storedHash)) {
            try {
                return BCrypt.checkpw(plainPassword, storedHash);
            } catch (Exception e) {
                return false;
            }
        }
        // Fallback for legacy plain-text passwords
        return plainPassword.equals(storedHash);
    }

    /**
     * Determines whether the stored hash needs to be upgraded/re-hashed.
     * Returns true if the stored value is not yet a BCrypt hash.
     */
    public static boolean needsRehash(String storedHash) {
        return !isBCryptHash(storedHash);
    }

    /**
     * Checks if a string has the structure of a BCrypt hash.
     */
    public static boolean isBCryptHash(String str) {
        if (str == null || str.length() != 60) {
            return false;
        }
        return str.startsWith("$2a$") || str.startsWith("$2b$") || str.startsWith("$2y$");
    }
}

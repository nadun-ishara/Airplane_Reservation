package com.airline.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    @DisplayName("Hash password produces valid 60-char BCrypt string")
    void testHashPasswordValid() {
        String raw = "secretPassword123";
        String hash = PasswordUtil.hashPassword(raw);

        assertNotNull(hash);
        assertEquals(60, hash.length());
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$") || hash.startsWith("$2y$12$"));
    }

    @Test
    @DisplayName("BCrypt checkPassword succeeds with correct plain password")
    void testCheckPasswordSuccess() {
        String raw = "admin123";
        String hash = PasswordUtil.hashPassword(raw);

        assertTrue(PasswordUtil.checkPassword("admin123", hash));
    }

    @Test
    @DisplayName("BCrypt checkPassword fails with wrong password")
    void testCheckPasswordFailure() {
        String raw = "admin123";
        String hash = PasswordUtil.hashPassword(raw);

        assertFalse(PasswordUtil.checkPassword("wrongPass", hash));
    }

    @Test
    @DisplayName("Graceful migration: Plain-text legacy passwords match and trigger needsRehash")
    void testLegacyPlainTextSupport() {
        String legacyPlain = "oldPlainPassword";

        assertTrue(PasswordUtil.checkPassword("oldPlainPassword", legacyPlain));
        assertFalse(PasswordUtil.checkPassword("wrongPassword", legacyPlain));

        assertTrue(PasswordUtil.needsRehash(legacyPlain));

        String modernHash = PasswordUtil.hashPassword(legacyPlain);
        assertFalse(PasswordUtil.needsRehash(modernHash));
    }

    @Test
    @DisplayName("isBCryptHash correctly classifies strings")
    void testIsBCryptHash() {
        assertFalse(PasswordUtil.isBCryptHash("admin123"));
        assertFalse(PasswordUtil.isBCryptHash(null));
        assertFalse(PasswordUtil.isBCryptHash(""));

        String realHash = PasswordUtil.hashPassword("testPass");
        assertTrue(PasswordUtil.isBCryptHash(realHash));
    }
}

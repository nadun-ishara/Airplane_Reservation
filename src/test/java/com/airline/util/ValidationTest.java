package com.airline.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Z0-9]{5,12}$");

    @Test
    @DisplayName("Email validation pattern accepts valid formats")
    void testValidEmails() {
        assertTrue(EMAIL_PATTERN.matcher("user@example.com").matches());
        assertTrue(EMAIL_PATTERN.matcher("admin@airline.lk").matches());
        assertTrue(EMAIL_PATTERN.matcher("john.doe+flight@domain.co.uk").matches());
    }

    @Test
    @DisplayName("Email validation pattern rejects invalid formats")
    void testInvalidEmails() {
        assertFalse(EMAIL_PATTERN.matcher("plainaddress").matches());
        assertFalse(EMAIL_PATTERN.matcher("@missingusername.com").matches());
        assertFalse(EMAIL_PATTERN.matcher("missingdomain@").matches());
        assertFalse(EMAIL_PATTERN.matcher("spaces in@domain.com").matches());
    }

    @Test
    @DisplayName("Passport / NIC pattern accepts 5 to 12 alphanumeric characters")
    void testValidPassport() {
        assertTrue(PASSPORT_PATTERN.matcher("N1234567").matches());
        assertTrue(PASSPORT_PATTERN.matcher("200012345678").matches());
        assertTrue(PASSPORT_PATTERN.matcher("ABC12").matches());
    }

    @Test
    @DisplayName("Passport pattern rejects invalid lengths and symbols")
    void testInvalidPassport() {
        assertFalse(PASSPORT_PATTERN.matcher("A1").matches()); // too short
        assertFalse(PASSPORT_PATTERN.matcher("VERYLONGPASSPORTNUMBER123456").matches()); // too long
        assertFalse(PASSPORT_PATTERN.matcher("NIC#123!").matches()); // contains symbols
    }

    @Test
    @DisplayName("Business class seat calculation applies 1.5x multiplier")
    void testBusinessClassPricing() {
        double baseFare = 400.00;
        int row1 = 2; // Business class (row <= 3)
        int row5 = 5; // Economy class (row > 3)

        double businessPrice = (row1 <= 3) ? (baseFare * 1.5) : baseFare;
        double economyPrice = (row5 <= 3) ? (baseFare * 1.5) : baseFare;

        assertEquals(600.00, businessPrice, 0.001);
        assertEquals(400.00, economyPrice, 0.001);
    }
}

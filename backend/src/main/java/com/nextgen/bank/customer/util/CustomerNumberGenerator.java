package com.nextgen.bank.customer.util;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Utility for generating and validating public customer numbers.
 * Format: CUST-XXXXXXXX (8-character alphanumeric payload from base-32 un-ambiguous charset).
 */
public final class CustomerNumberGenerator {

    public static final String PREFIX = "CUST-";
    public static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final int PAYLOAD_LENGTH = 8;
    private static final Pattern PATTERN = Pattern.compile("^CUST-[" + ALPHABET + "]{8}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private CustomerNumberGenerator() {
        // Utility class
    }

    /**
     * Generates a cryptographically random, non-sequential public customer number.
     * Example: CUST-7K4P9M2Q
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < PAYLOAD_LENGTH; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Validates whether the given string matches the public customer number format.
     */
    public static boolean isValid(String customerNumber) {
        if (customerNumber == null) {
            return false;
        }
        return PATTERN.matcher(customerNumber.trim()).matches();
    }
}

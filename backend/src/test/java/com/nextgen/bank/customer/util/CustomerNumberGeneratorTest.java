package com.nextgen.bank.customer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomerNumberGeneratorTest {

    @Test
    @DisplayName("Should generate valid customer number with correct prefix and length")
    void testGenerateFormat() {
        String customerNumber = CustomerNumberGenerator.generate();

        assertNotNull(customerNumber);
        assertTrue(customerNumber.startsWith("CUST-"));
        assertEquals(13, customerNumber.length()); // "CUST-" (5) + 8 payload chars
        assertTrue(CustomerNumberGenerator.isValid(customerNumber));
    }

    @Test
    @DisplayName("Should only contain un-ambiguous base-32 characters (excluding 0, O, 1, I)")
    void testPayloadCharacterSet() {
        for (int i = 0; i < 200; i++) {
            String customerNumber = CustomerNumberGenerator.generate();
            String payload = customerNumber.substring(5);

            for (char c : payload.toCharArray()) {
                assertTrue(CustomerNumberGenerator.ALPHABET.indexOf(c) >= 0,
                        "Payload character '" + c + "' is not in allowed alphabet");
                assertFalse("0O1I".indexOf(c) >= 0,
                        "Payload must not contain ambiguous characters 0, O, 1, I");
            }
        }
    }

    @Test
    @DisplayName("Should generate unique customer numbers without collisions")
    void testUniqueness() {
        Set<String> generatedSet = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            String customerNumber = CustomerNumberGenerator.generate();
            assertTrue(generatedSet.add(customerNumber), "Collision detected for customer number: " + customerNumber);
        }

        assertEquals(count, generatedSet.size());
    }

    @Test
    @DisplayName("Should validate format correctly")
    void testValidation() {
        assertTrue(CustomerNumberGenerator.isValid("CUST-7K4P9M2Q"));
        assertTrue(CustomerNumberGenerator.isValid("CUST-9R2N8X5W"));
        assertTrue(CustomerNumberGenerator.isValid("CUST-4H7QK9MP"));

        // Negative cases
        assertFalse(CustomerNumberGenerator.isValid(null));
        assertFalse(CustomerNumberGenerator.isValid(""));
        assertFalse(CustomerNumberGenerator.isValid("CUST-1234567")); // Too short
        assertFalse(CustomerNumberGenerator.isValid("CUST-123456789")); // Too long
        assertFalse(CustomerNumberGenerator.isValid("cust-7k4p9m2q")); // Lowercase
        assertFalse(CustomerNumberGenerator.isValid("CUST-7K4P9M20")); // Contains '0'
        assertFalse(CustomerNumberGenerator.isValid("CUST-7K4P9M2O")); // Contains 'O'
        assertFalse(CustomerNumberGenerator.isValid("CUST-7K4P9M21")); // Contains '1'
        assertFalse(CustomerNumberGenerator.isValid("CUST-7K4P9M2I")); // Contains 'I'
        assertFalse(CustomerNumberGenerator.isValid("ACC-7K4P9M2Q")); // Wrong prefix
    }
}

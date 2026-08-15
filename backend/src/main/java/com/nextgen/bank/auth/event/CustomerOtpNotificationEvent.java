package com.nextgen.bank.auth.event;

import java.time.Instant;

/**
 * Ephemeral in-memory Spring event emitted when an OTP is generated for customer communication.
 * The separate Notification module subscribes to this contract to deliver SMS/Email.
 */
public record CustomerOtpNotificationEvent(
        String identifier,
        String otp,
        String purpose,
        Instant expiresAt
) {
}

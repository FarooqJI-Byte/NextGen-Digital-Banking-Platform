package com.nextgen.bank.auth.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Ephemeral in-memory Spring event emitted when a BANK_STAFF activation link is ready for delivery.
 * The separate Notification module subscribes to this contract to compose and send the activation email.
 *
 * NOTE: This event is ephemeral in JVM memory and MUST NEVER be written to persistent Outbox/DB.
 */
public record StaffActivationNotificationEvent(
        UUID staffUserId,
        String username,
        String email,
        String activationUrl,
        Instant expiresAt
) {
}

package com.nextgen.bank.auth.event;

import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class UserRegisteredEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID userId;
    private final String email;
    private final UserRole role;
    private final Instant occurredOn;

    public UserRegisteredEvent(UUID userId, String email, UserRole role) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.occurredOn = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateType() {
        return "USER";
    }

    @Override
    public String getAggregateId() {
        return userId != null ? userId.toString() : "";
    }

    @Override
    public String getEventType() {
        return "UserRegisteredEvent";
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public Object getPayload() {
        return Map.of(
                "eventId", eventId,
                "userId", userId,
                "email", email,
                "role", role != null ? role.name() : "",
                "timestamp", occurredOn
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}

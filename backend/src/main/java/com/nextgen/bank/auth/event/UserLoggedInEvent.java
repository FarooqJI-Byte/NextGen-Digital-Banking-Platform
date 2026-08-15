package com.nextgen.bank.auth.event;

import com.nextgen.bank.common.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class UserLoggedInEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID userId;
    private final String ipAddress;
    private final String userAgent;
    private final Instant occurredOn;

    public UserLoggedInEvent(UUID userId, String ipAddress, String userAgent) {
        this.eventId = UUID.randomUUID();
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
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
        return "UserLoggedInEvent";
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
                "ipAddress", ipAddress != null ? ipAddress : "unknown",
                "userAgent", userAgent != null ? userAgent : "unknown",
                "timestamp", occurredOn
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}

package com.nextgen.bank.auth.event;

import com.nextgen.bank.common.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class StaffActivatedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID staffUserId;
    private final String email;
    private final Instant occurredOn;

    public StaffActivatedEvent(UUID staffUserId, String email) {
        this.eventId = UUID.randomUUID();
        this.staffUserId = staffUserId;
        this.email = email;
        this.occurredOn = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "StaffActivatedEvent";
    }

    @Override
    public String getAggregateType() {
        return "USER";
    }

    @Override
    public String getAggregateId() {
        return staffUserId != null ? staffUserId.toString() : "";
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public Object getPayload() {
        return Map.of(
                "eventId", eventId,
                "staffUserId", staffUserId,
                "email", email,
                "timestamp", occurredOn
        );
    }

    public UUID getStaffUserId() {
        return staffUserId;
    }

    public String getEmail() {
        return email;
    }
}

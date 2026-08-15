package com.nextgen.bank.customer.event;

import com.nextgen.bank.common.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class CustomerCreatedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID customerId;
    private final UUID userId;
    private final String phone;
    private final String email;
    private final Instant occurredOn;

    public CustomerCreatedEvent(UUID customerId, UUID userId, String phone, String email) {
        this.eventId = UUID.randomUUID();
        this.customerId = customerId;
        this.userId = userId;
        this.phone = phone;
        this.email = email;
        this.occurredOn = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateType() {
        return "CUSTOMER";
    }

    @Override
    public String getAggregateId() {
        return customerId != null ? customerId.toString() : "";
    }

    @Override
    public String getEventType() {
        return "CustomerCreatedEvent";
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public Object getPayload() {
        return Map.of(
                "eventId", eventId,
                "customerId", customerId,
                "userId", userId,
                "phone", phone,
                "email", email,
                "timestamp", occurredOn
        );
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}

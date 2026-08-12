package com.nextgen.bank.customer.event;

import com.nextgen.bank.common.event.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class KYCRejectedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID customerId;
    private final UUID verifiedBy;
    private final String remarks;
    private final Instant occurredOn;

    public KYCRejectedEvent(UUID customerId, UUID verifiedBy, String remarks) {
        this.eventId = UUID.randomUUID();
        this.customerId = customerId;
        this.verifiedBy = verifiedBy;
        this.remarks = remarks;
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
        return "KYCRejectedEvent";
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
                "verifiedBy", verifiedBy != null ? verifiedBy.toString() : "system",
                "remarks", remarks != null ? remarks : "",
                "timestamp", occurredOn
        );
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public String getRemarks() {
        return remarks;
    }
}

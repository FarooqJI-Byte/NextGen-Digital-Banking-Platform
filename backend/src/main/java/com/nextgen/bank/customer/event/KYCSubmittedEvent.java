package com.nextgen.bank.customer.event;

import com.nextgen.bank.common.event.DomainEvent;
import com.nextgen.bank.customer.domain.enums.DocumentType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class KYCSubmittedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID customerId;
    private final UUID documentId;
    private final DocumentType documentType;
    private final Instant occurredOn;

    public KYCSubmittedEvent(UUID customerId, UUID documentId, DocumentType documentType) {
        this.eventId = UUID.randomUUID();
        this.customerId = customerId;
        this.documentId = documentId;
        this.documentType = documentType;
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
        return "KYCSubmittedEvent";
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
                "documentId", documentId,
                "documentType", documentType != null ? documentType.name() : "",
                "timestamp", occurredOn
        );
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }
}

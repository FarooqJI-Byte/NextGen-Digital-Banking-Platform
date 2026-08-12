package com.nextgen.bank.customer.domain;

import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.domain.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cust_kyc_docs")
public class KYCDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number_enc", nullable = false, length = 255)
    private String documentNumberEnc;

    @Column(name = "file_reference", nullable = false, length = 255)
    private String fileReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public KYCDocument() {
    }

    public KYCDocument(UUID customerId, DocumentType documentType, String documentNumberEnc, String fileReference) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.documentType = Objects.requireNonNull(documentType, "Document type cannot be null");
        this.documentNumberEnc = Objects.requireNonNull(documentNumberEnc, "Document number cannot be null");
        this.fileReference = Objects.requireNonNull(fileReference, "File reference cannot be null");
        this.verificationStatus = VerificationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.verificationStatus == null) {
            this.verificationStatus = VerificationStatus.PENDING;
        }
    }

    public void approve(UUID staffUserId) {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.verifiedBy = staffUserId;
        this.verifiedAt = Instant.now();
    }

    public void reject(UUID staffUserId) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedBy = staffUserId;
        this.verifiedAt = Instant.now();
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumberEnc() {
        return documentNumberEnc;
    }

    public void setDocumentNumberEnc(String documentNumberEnc) {
        this.documentNumberEnc = documentNumberEnc;
    }

    public String getFileReference() {
        return fileReference;
    }

    public void setFileReference(String fileReference) {
        this.fileReference = fileReference;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UUID getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(UUID verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}

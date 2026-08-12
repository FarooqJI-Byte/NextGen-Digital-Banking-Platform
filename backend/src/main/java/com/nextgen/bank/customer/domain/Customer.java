package com.nextgen.bank.customer.domain;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.enums.RiskCategory;
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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cust_profiles")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    private KYCStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private RiskCategory riskCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Customer() {
    }

    public Customer(UUID userId, String firstName, String lastName, LocalDate dateOfBirth, String phone, String email) {
        this(userId, firstName, lastName, dateOfBirth, phone, email, KYCStatus.PENDING, RiskCategory.MEDIUM);
    }

    public Customer(UUID userId, String firstName, String lastName, LocalDate dateOfBirth, String phone, String email, KYCStatus kycStatus, RiskCategory riskCategory) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null");
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.kycStatus = kycStatus != null ? kycStatus : KYCStatus.PENDING;
        this.riskCategory = riskCategory != null ? riskCategory : RiskCategory.MEDIUM;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.kycStatus == null) {
            this.kycStatus = KYCStatus.PENDING;
        }
        if (this.riskCategory == null) {
            this.riskCategory = RiskCategory.MEDIUM;
        }
    }

    public void verifyKyc() {
        this.kycStatus = KYCStatus.VERIFIED;
    }

    public void rejectKyc() {
        this.kycStatus = KYCStatus.REJECTED;
    }

    public boolean isKycVerified() {
        return this.kycStatus == KYCStatus.VERIFIED;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public KYCStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KYCStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

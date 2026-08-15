package com.nextgen.bank.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cust_nominees")
public class Nominee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "nominee_id", nullable = false, updatable = false)
    private UUID nomineeId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "allocation_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    public Nominee() {
    }

    public Nominee(UUID customerId, String fullName, String relationship, LocalDate dateOfBirth, String phone, BigDecimal allocationPercentage) {
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.relationship = Objects.requireNonNull(relationship, "Relationship cannot be null");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null");
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
        this.allocationPercentage = allocationPercentage != null ? allocationPercentage : new BigDecimal("100.00");
    }

    @PrePersist
    protected void onCreate() {
        if (this.allocationPercentage == null) {
            this.allocationPercentage = new BigDecimal("100.00");
        }
    }

    public UUID getNomineeId() {
        return nomineeId;
    }

    public void setNomineeId(UUID nomineeId) {
        this.nomineeId = nomineeId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
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

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }
}

package com.nextgen.bank.card.domain;

import com.nextgen.bank.common.enums.CardStatus;
import com.nextgen.bank.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "card_cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id", nullable = false, updatable = false)
    private UUID cardId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "masked_number", nullable = false, length = 19)
    private String maskedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cvv_hash", nullable = false, length = 255)
    private String cvvHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.INACTIVE;

    @Column(name = "block_reason", length = 50)
    private String blockReason;

    @Column(name = "daily_pos_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyPosLimit = new BigDecimal("50000.00");

    @Column(name = "daily_atm_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyAtmLimit = new BigDecimal("25000.00");

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    public Card() {
    }

    public Card(UUID accountId, UUID customerId, String maskedNumber, CardType cardType, LocalDate expiryDate, String cvvHash) {
        this.accountId = Objects.requireNonNull(accountId, "Account ID is mandatory");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID is mandatory");
        this.maskedNumber = Objects.requireNonNull(maskedNumber, "Masked card number is mandatory");
        this.cardType = Objects.requireNonNull(cardType, "Card type is mandatory");
        this.expiryDate = Objects.requireNonNull(expiryDate, "Expiry date is mandatory");
        this.cvvHash = Objects.requireNonNull(cvvHash, "CVV hash is mandatory");
        this.status = CardStatus.INACTIVE;
        this.dailyPosLimit = new BigDecimal("50000.00");
        this.dailyAtmLimit = new BigDecimal("25000.00");
    }

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
        if (status == null) {
            status = CardStatus.INACTIVE;
        }
        if (dailyPosLimit == null) {
            dailyPosLimit = new BigDecimal("50000.00");
        }
        if (dailyAtmLimit == null) {
            dailyAtmLimit = new BigDecimal("25000.00");
        }
    }

    public void activate() {
        if (this.status != CardStatus.INACTIVE && this.status != CardStatus.ACTIVE) {
            throw new BusinessException("Cannot activate card in state: " + this.status, HttpStatus.BAD_REQUEST, "INVALID_CARD_STATE");
        }
        this.status = CardStatus.ACTIVE;
    }

    public void block(String reason) {
        if (this.status == CardStatus.CANCELLED || this.status == CardStatus.EXPIRED) {
            throw new BusinessException("Cannot block card in state: " + this.status, HttpStatus.BAD_REQUEST, "INVALID_CARD_STATE");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Block reason is mandatory", HttpStatus.BAD_REQUEST, "MISSING_BLOCK_REASON");
        }
        this.status = CardStatus.BLOCKED;
        this.blockReason = reason;
    }

    public void unblock() {
        if (this.status != CardStatus.BLOCKED) {
            throw new BusinessException("Cannot unblock card in state: " + this.status, HttpStatus.BAD_REQUEST, "INVALID_CARD_STATE");
        }
        this.status = CardStatus.ACTIVE;
        this.blockReason = null;
    }

    public void updateLimits(BigDecimal posLimit, BigDecimal atmLimit) {
        if (posLimit != null && posLimit.compareTo(BigDecimal.ZERO) >= 0) {
            this.dailyPosLimit = posLimit;
        }
        if (atmLimit != null && atmLimit.compareTo(BigDecimal.ZERO) >= 0) {
            this.dailyAtmLimit = atmLimit;
        }
    }

    // Getters and Setters
    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public void setMaskedNumber(String maskedNumber) {
        this.maskedNumber = maskedNumber;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvvHash() {
        return cvvHash;
    }

    public void setCvvHash(String cvvHash) {
        this.cvvHash = cvvHash;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public BigDecimal getDailyPosLimit() {
        return dailyPosLimit;
    }

    public void setDailyPosLimit(BigDecimal dailyPosLimit) {
        this.dailyPosLimit = dailyPosLimit;
    }

    public BigDecimal getDailyAtmLimit() {
        return dailyAtmLimit;
    }

    public void setDailyAtmLimit(BigDecimal dailyAtmLimit) {
        this.dailyAtmLimit = dailyAtmLimit;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}

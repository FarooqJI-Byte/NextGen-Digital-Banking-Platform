package com.nextgen.bank.account.domain;

import com.nextgen.bank.common.enums.AccountStatus;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.vo.Money;
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
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "acc_accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.PENDING_APPROVAL;

    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Account() {
    }

    public Account(String accountNumber, UUID customerId, AccountType accountType, String currency) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.accountType = Objects.requireNonNull(accountType, "Account type cannot be null");
        this.currency = currency != null ? currency : "INR";
        this.balance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.status = AccountStatus.PENDING_APPROVAL;
    }

    @PrePersist
    protected void onCreate() {
        if (openedAt == null) {
            openedAt = Instant.now();
        }
        if (currency == null) {
            currency = "INR";
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        if (status == null) {
            status = AccountStatus.PENDING_APPROVAL;
        }
    }

    // State machine methods
    public void activate() {
        if (this.status != AccountStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "Cannot activate account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void reject(String reason) {
        if (this.status != AccountStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "Cannot reject account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = AccountStatus.REJECTED;
        this.freezeReason = reason;
    }

    public void freeze(String reason) {
        if (this.status != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "Cannot freeze account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(
                    "Freeze reason is mandatory",
                    HttpStatus.BAD_REQUEST,
                    "MISSING_FREEZE_REASON"
            );
        }
        this.status = AccountStatus.FROZEN;
        this.freezeReason = reason;
    }

    public void unfreeze() {
        if (this.status != AccountStatus.FROZEN) {
            throw new BusinessException(
                    "Cannot unfreeze account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = AccountStatus.ACTIVE;
        this.freezeReason = null;
    }

    public void requestClosure() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "Cannot request closure for account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        this.status = AccountStatus.CLOSURE_REQUESTED;
    }

    public void close() {
        if (this.status != AccountStatus.CLOSURE_REQUESTED && this.status != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "Cannot close account in state: " + this.status,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE_TRANSITION"
            );
        }
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "Account balance must be zero for closure. Current balance: " + balance,
                    HttpStatus.BAD_REQUEST,
                    "NON_ZERO_BALANCE_CLOSURE"
            );
        }
        this.status = AccountStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    // Value Object helper methods
    public Money getBalanceAsMoney() {
        return new Money(balance, currency);
    }

    public Money getAvailableBalanceAsMoney() {
        return new Money(availableBalance, currency);
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getFreezeReason() {
        return freezeReason;
    }

    public void setFreezeReason(String freezeReason) {
        this.freezeReason = freezeReason;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}

package com.nextgen.bank.account.domain;

import com.nextgen.bank.common.enums.AccountStatus;
import com.nextgen.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for Account domain state machine (06-state-machines.md)
 * and business rules (07-business-rules.md). No Spring context needed.
 */
class AccountDomainTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account("500100000001", UUID.randomUUID(), AccountType.SAVINGS, "INR");
    }

    // ── State Machine: Happy Paths ──────────────────────────────────────────

    @Test
    @DisplayName("PENDING_APPROVAL → ACTIVE: activate() succeeds")
    void activate_fromPendingApproval_setsStatusActive() {
        account.activate();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("PENDING_APPROVAL → REJECTED: reject() succeeds")
    void reject_fromPendingApproval_setsStatusRejected() {
        account.reject("Incomplete documents");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.REJECTED);
    }

    @Test
    @DisplayName("ACTIVE → FROZEN: freeze() succeeds with reason")
    void freeze_fromActive_setsStatusFrozen() {
        account.activate();
        account.freeze("Suspicious activity");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.FROZEN);
        assertThat(account.getFreezeReason()).isEqualTo("Suspicious activity");
    }

    @Test
    @DisplayName("FROZEN → ACTIVE: unfreeze() succeeds and clears reason")
    void unfreeze_fromFrozen_setsStatusActiveAndClearsReason() {
        account.activate();
        account.freeze("Suspicious activity");
        account.unfreeze();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getFreezeReason()).isNull();
    }

    @Test
    @DisplayName("ACTIVE → CLOSURE_REQUESTED: requestClosure() succeeds")
    void requestClosure_fromActive_setsStatusClosureRequested() {
        account.activate();
        account.requestClosure();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSURE_REQUESTED);
    }

    @Test
    @DisplayName("CLOSURE_REQUESTED → CLOSED: close() succeeds with zero balance (BR-ACC-005)")
    void close_fromClosureRequestedWithZeroBalance_setsStatusClosed() {
        account.activate();
        account.requestClosure();
        account.close();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.getClosedAt()).isNotNull();
    }

    // ── State Machine: Invalid Transitions (Failure Cases) ─────────────────

    @Test
    @DisplayName("ACTIVE → activate() throws — already active")
    void activate_fromActive_throwsBusinessException() {
        account.activate();
        assertThatThrownBy(account::activate)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot activate account in state: ACTIVE");
    }

    @Test
    @DisplayName("PENDING_APPROVAL → freeze() throws — wrong state")
    void freeze_fromPendingApproval_throwsBusinessException() {
        assertThatThrownBy(() -> account.freeze("reason"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot freeze account in state: PENDING_APPROVAL");
    }

    @Test
    @DisplayName("ACTIVE → unfreeze() throws — not frozen")
    void unfreeze_fromActive_throwsBusinessException() {
        account.activate();
        assertThatThrownBy(account::unfreeze)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot unfreeze account in state: ACTIVE");
    }

    @Test
    @DisplayName("FROZEN → requestClosure() throws — wrong state")
    void requestClosure_fromFrozen_throwsBusinessException() {
        account.activate();
        account.freeze("Fraud");
        assertThatThrownBy(account::requestClosure)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot request closure for account in state: FROZEN");
    }

    @Test
    @DisplayName("CLOSED → activate() throws — terminal state")
    void activate_fromClosed_throwsBusinessException() {
        account.activate();
        account.requestClosure();
        account.close();
        assertThatThrownBy(account::activate)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot activate account in state: CLOSED");
    }

    // ── Business Rules: Failure Cases ───────────────────────────────────────

    @Test
    @DisplayName("BR-ACC-005: close() with non-zero balance throws BusinessException")
    void close_withNonZeroBalance_throwsBusinessException() {
        account.activate();
        account.setBalance(new BigDecimal("500.00"));
        account.requestClosure();
        assertThatThrownBy(account::close)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account balance must be zero for closure");
    }

    @Test
    @DisplayName("freeze() with blank reason throws BusinessException")
    void freeze_withBlankReason_throwsBusinessException() {
        account.activate();
        assertThatThrownBy(() -> account.freeze("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Freeze reason is mandatory");
    }

    @Test
    @DisplayName("freeze() with null reason throws BusinessException")
    void freeze_withNullReason_throwsBusinessException() {
        account.activate();
        assertThatThrownBy(() -> account.freeze(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Freeze reason is mandatory");
    }

    @Test
    @DisplayName("BR-ACC-005: close() directly from ACTIVE with zero balance succeeds")
    void close_fromActiveWithZeroBalance_succeeds() {
        account.activate();
        account.close();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }
}

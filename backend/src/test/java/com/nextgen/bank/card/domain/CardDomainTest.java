package com.nextgen.bank.card.domain;

import com.nextgen.bank.common.enums.CardStatus;
import com.nextgen.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for Card domain state machine (06-state-machines.md)
 * and business rules (07-business-rules.md). No Spring context needed.
 */
class CardDomainTest {

    private Card card;

    @BeforeEach
    void setUp() {
        card = new Card(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "XXXX-XXXX-XXXX-1234",
                CardType.DEBIT,
                LocalDate.now().plusYears(3),
                "$2a$10$hashedCvv"
        );
    }

    // ── State Machine: Happy Paths ──────────────────────────────────────────

    @Test
    @DisplayName("INACTIVE → ACTIVE: activate() succeeds")
    void activate_fromInactive_setsStatusActive() {
        card.activate();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE → BLOCKED: block() succeeds with reason")
    void block_fromActive_setsStatusBlocked() {
        card.activate();
        card.block("LOST");
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(card.getBlockReason()).isEqualTo("LOST");
    }

    @Test
    @DisplayName("BLOCKED → ACTIVE: unblock() succeeds and clears reason")
    void unblock_fromBlocked_setsStatusActiveAndClearsReason() {
        card.activate();
        card.block("LOST");
        card.unblock();
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getBlockReason()).isNull();
    }

    // ── State Machine: Invalid Transitions (Failure Cases) ─────────────────

    @Test
    @DisplayName("BLOCKED → activate() throws — cannot activate blocked card")
    void activate_fromBlocked_throwsBusinessException() {
        card.activate();
        card.block("STOLEN");
        assertThatThrownBy(card::activate)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot activate card in state: BLOCKED");
    }

    @Test
    @DisplayName("ACTIVE → unblock() throws — card not blocked")
    void unblock_fromActive_throwsBusinessException() {
        card.activate();
        assertThatThrownBy(card::unblock)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot unblock card in state: ACTIVE");
    }

    @Test
    @DisplayName("INACTIVE → unblock() throws — card not blocked")
    void unblock_fromInactive_throwsBusinessException() {
        assertThatThrownBy(card::unblock)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot unblock card in state: INACTIVE");
    }

    @Test
    @DisplayName("block() with null reason throws BusinessException")
    void block_withNullReason_throwsBusinessException() {
        card.activate();
        assertThatThrownBy(() -> card.block(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Block reason is mandatory");
    }

    @Test
    @DisplayName("block() with blank reason throws BusinessException")
    void block_withBlankReason_throwsBusinessException() {
        card.activate();
        assertThatThrownBy(() -> card.block("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Block reason is mandatory");
    }

    // ── Business Rules ───────────────────────────────────────────────────────

    @Test
    @DisplayName("BR-CARD-003: Default POS limit is ₹50,000")
    void defaultDailyPosLimit_isCorrect() {
        assertThat(card.getDailyPosLimit()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("BR-CARD-003: Default ATM limit is ₹25,000")
    void defaultDailyAtmLimit_isCorrect() {
        assertThat(card.getDailyAtmLimit()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("updateLimits() updates POS and ATM limits correctly")
    void updateLimits_updatesCorrectly() {
        card.updateLimits(new BigDecimal("30000.00"), new BigDecimal("10000.00"));
        assertThat(card.getDailyPosLimit()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(card.getDailyAtmLimit()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("updateLimits() ignores negative values — limits unchanged")
    void updateLimits_withNegativeValues_keepsDefaults() {
        card.updateLimits(new BigDecimal("-100"), new BigDecimal("-50"));
        assertThat(card.getDailyPosLimit()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(card.getDailyAtmLimit()).isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    @Test
    @DisplayName("Card starts in INACTIVE status")
    void newCard_hasInactiveStatus() {
        assertThat(card.getStatus()).isEqualTo(CardStatus.INACTIVE);
    }
}

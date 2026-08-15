package com.nextgen.bank.card.dto;

import com.nextgen.bank.card.domain.Card;
import com.nextgen.bank.card.domain.CardType;
import com.nextgen.bank.common.enums.CardStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponseDto(
        UUID cardId,
        UUID accountId,
        UUID customerId,
        String maskedNumber,
        CardType cardType,
        LocalDate expiryDate,
        CardStatus status,
        String blockReason,
        BigDecimal dailyPosLimit,
        BigDecimal dailyAtmLimit,
        Instant issuedAt
) {
    public static CardResponseDto fromEntity(Card card) {
        return new CardResponseDto(
                card.getCardId(),
                card.getAccountId(),
                card.getCustomerId(),
                card.getMaskedNumber(),
                card.getCardType(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBlockReason(),
                card.getDailyPosLimit(),
                card.getDailyAtmLimit(),
                card.getIssuedAt()
        );
    }
}

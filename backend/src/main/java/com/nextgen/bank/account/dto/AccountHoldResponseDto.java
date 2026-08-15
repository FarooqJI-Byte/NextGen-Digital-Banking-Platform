package com.nextgen.bank.account.dto;

import com.nextgen.bank.account.domain.AccountHold;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountHoldResponseDto(
        UUID holdId,
        UUID accountId,
        BigDecimal amount,
        String reason,
        String status,
        Instant createdAt
) {
    public static AccountHoldResponseDto fromEntity(AccountHold hold) {
        return new AccountHoldResponseDto(
                hold.getHoldId(),
                hold.getAccount().getAccountId(),
                hold.getAmount(),
                hold.getReason(),
                hold.getStatus(),
                hold.getCreatedAt()
        );
    }
}

package com.nextgen.bank.account.dto;

import com.nextgen.bank.account.domain.Account;
import com.nextgen.bank.account.domain.AccountType;
import com.nextgen.bank.common.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponseDto(
        UUID accountId,
        String accountNumber,
        UUID customerId,
        AccountType accountType,
        BigDecimal balance,
        BigDecimal availableBalance,
        String currency,
        AccountStatus status,
        String freezeReason,
        Instant openedAt,
        Instant closedAt
) {
    public static AccountResponseDto fromEntity(Account account) {
        return new AccountResponseDto(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getAccountType(),
                account.getBalance(),
                account.getAvailableBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getFreezeReason(),
                account.getOpenedAt(),
                account.getClosedAt()
        );
    }
}

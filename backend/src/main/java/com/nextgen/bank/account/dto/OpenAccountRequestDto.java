package com.nextgen.bank.account.dto;

import com.nextgen.bank.account.domain.AccountType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenAccountRequestDto(
        @NotNull(message = "Customer ID is mandatory")
        UUID customerId,

        @NotNull(message = "Account type is mandatory")
        AccountType accountType,

        String currency
) {}

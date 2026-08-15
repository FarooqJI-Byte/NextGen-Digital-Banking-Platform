package com.nextgen.bank.card.dto;

import com.nextgen.bank.card.domain.CardType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IssueCardRequestDto(
        @NotNull(message = "Account ID is mandatory")
        UUID accountId,

        @NotNull(message = "Customer ID is mandatory")
        UUID customerId,

        @NotNull(message = "Card type is mandatory")
        CardType cardType
) {}

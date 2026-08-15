package com.nextgen.bank.card.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockCardRequestDto(
        @NotBlank(message = "Block reason is mandatory")
        String reason,

        String comments
) {}

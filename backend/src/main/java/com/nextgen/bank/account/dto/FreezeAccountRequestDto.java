package com.nextgen.bank.account.dto;

import jakarta.validation.constraints.NotBlank;

public record FreezeAccountRequestDto(
        @NotBlank(message = "Freeze reason is required")
        String freezeReason
) {}

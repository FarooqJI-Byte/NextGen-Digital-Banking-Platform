package com.nextgen.bank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountHoldRequestDto(
        @NotNull(message = "Amount is mandatory")
        @DecimalMin(value = "0.01", message = "Hold amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Hold reason is mandatory")
        String reason
) {}

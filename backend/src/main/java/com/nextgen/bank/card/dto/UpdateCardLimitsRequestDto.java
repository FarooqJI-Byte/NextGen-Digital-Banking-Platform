package com.nextgen.bank.card.dto;

import java.math.BigDecimal;

public record UpdateCardLimitsRequestDto(
        BigDecimal dailyPosLimit,
        BigDecimal dailyAtmLimit
) {}

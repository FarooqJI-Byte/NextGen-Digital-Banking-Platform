package com.nextgen.bank.card.service;

import com.nextgen.bank.card.dto.BlockCardRequestDto;
import com.nextgen.bank.card.dto.CardResponseDto;
import com.nextgen.bank.card.dto.IssueCardRequestDto;
import com.nextgen.bank.card.dto.UpdateCardLimitsRequestDto;

import java.util.List;
import java.util.UUID;

public interface CardService {

    CardResponseDto issueCard(IssueCardRequestDto requestDto);

    CardResponseDto getCardById(UUID cardId);

    List<CardResponseDto> getCardsByCustomerId(UUID customerId);

    List<CardResponseDto> getCardsByAccountId(UUID accountId);

    CardResponseDto activateCard(UUID cardId);

    CardResponseDto blockCard(UUID cardId, BlockCardRequestDto requestDto);

    CardResponseDto unblockCard(UUID cardId);

    CardResponseDto updateCardLimits(UUID cardId, UpdateCardLimitsRequestDto requestDto);
}

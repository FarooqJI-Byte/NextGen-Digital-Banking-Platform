package com.nextgen.bank.card.service.impl;

import com.nextgen.bank.account.service.AccountService;
import com.nextgen.bank.card.domain.Card;
import com.nextgen.bank.card.dto.BlockCardRequestDto;
import com.nextgen.bank.card.dto.CardResponseDto;
import com.nextgen.bank.card.dto.IssueCardRequestDto;
import com.nextgen.bank.card.dto.UpdateCardLimitsRequestDto;
import com.nextgen.bank.card.repository.CardRepository;
import com.nextgen.bank.card.service.CardService;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.ResourceNotFoundException;
import com.nextgen.bank.card.domain.CardType;
import com.nextgen.bank.common.enums.CardStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final OutboxEventWriter outboxEventWriter;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public CardServiceImpl(CardRepository cardRepository,
                           AccountService accountService,
                           OutboxEventWriter outboxEventWriter,
                           PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.outboxEventWriter = outboxEventWriter;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CardResponseDto issueCard(IssueCardRequestDto requestDto) {
        // Verify that the account exists via the public AccountService interface
        try {
            accountService.getAccountById(requestDto.accountId());
        } catch (Exception e) {
            // Propagate as ResourceNotFoundException to keep existing semantics
            throw new ResourceNotFoundException("Account not found with ID: " + requestDto.accountId());
        }

        // BR-CARD-001: Maximum 1 active Debit Card and 1 active Credit Card per account
        List<CardStatus> activeStatuses = List.of(CardStatus.INACTIVE, CardStatus.ACTIVE, CardStatus.BLOCKED);
        boolean cardAlreadyExists = cardRepository.existsByAccountIdAndCardTypeAndStatusIn(
                requestDto.accountId(), requestDto.cardType(), activeStatuses);
        if (cardAlreadyExists) {
            throw new BusinessException(
                    "An active " + requestDto.cardType().name() + " card already exists for this account. " +
                    "Maximum 1 active card per type is allowed per account (BR-CARD-001).",
                    HttpStatus.CONFLICT,
                    "CARD_ALREADY_EXISTS"
            );
        }

        int last4 = 1000 + random.nextInt(9000);
        String maskedNumber = "XXXX-XXXX-XXXX-" + last4;
        String rawCvv = String.format("%03d", random.nextInt(1000));
        String cvvHash = passwordEncoder.encode(rawCvv);
        LocalDate expiryDate = LocalDate.now().plusYears(3);

        Card card = new Card(
                requestDto.accountId(),
                requestDto.customerId(),
                maskedNumber,
                requestDto.cardType(),
                expiryDate,
                cvvHash
        );
        card.activate();

        Card saved = cardRepository.save(card);

        outboxEventWriter.write(
                "CARD",
                saved.getCardId().toString(),
                "CardIssuedEvent",
                Map.of(
                        "cardId", saved.getCardId(),
                        "accountId", saved.getAccountId(),
                        "customerId", saved.getCustomerId(),
                        "cardType", saved.getCardType().name()
                )
        );

        return CardResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponseDto getCardById(UUID cardId) {
        Card card = findCardOrThrow(cardId);
        return CardResponseDto.fromEntity(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByCustomerId(UUID customerId) {
        return cardRepository.findByCustomerId(customerId)
                .stream()
                .map(CardResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardsByAccountId(UUID accountId) {
        return cardRepository.findByAccountId(accountId)
                .stream()
                .map(CardResponseDto::fromEntity)
                .toList();
    }

    @Override
    public CardResponseDto activateCard(UUID cardId) {
        Card card = findCardOrThrow(cardId);
        card.activate();
        Card updated = cardRepository.save(card);
        return CardResponseDto.fromEntity(updated);
    }

    @Override
    public CardResponseDto blockCard(UUID cardId, BlockCardRequestDto requestDto) {
        Card card = findCardOrThrow(cardId);
        card.block(requestDto.reason());
        Card updated = cardRepository.save(card);

        // 02-business-events.md: CardBlockedEvent with {cardId, maskedNumber, reason, blockedAt}
        outboxEventWriter.write(
                "CARD",
                updated.getCardId().toString(),
                "CardBlockedEvent",
                Map.of(
                        "cardId", updated.getCardId(),
                        "maskedNumber", updated.getMaskedNumber(),
                        "reason", requestDto.reason(),
                        "blockedAt", Instant.now().toString()
                )
        );

        return CardResponseDto.fromEntity(updated);
    }


    @Override
    public CardResponseDto unblockCard(UUID cardId) {
        Card card = findCardOrThrow(cardId);
        card.unblock();
        Card updated = cardRepository.save(card);

        outboxEventWriter.write(
                "CARD",
                updated.getCardId().toString(),
                "CardStatusUpdatedEvent",
                Map.of(
                        "cardId", updated.getCardId(),
                        "status", updated.getStatus().name(),
                        "reason", "Unblocked by customer request"
                )
        );

        return CardResponseDto.fromEntity(updated);
    }

    @Override
    public CardResponseDto updateCardLimits(UUID cardId, UpdateCardLimitsRequestDto requestDto) {
        Card card = findCardOrThrow(cardId);
        card.updateLimits(requestDto.dailyPosLimit(), requestDto.dailyAtmLimit());
        Card updated = cardRepository.save(card);
        return CardResponseDto.fromEntity(updated);
    }

    private Card findCardOrThrow(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));
    }
}

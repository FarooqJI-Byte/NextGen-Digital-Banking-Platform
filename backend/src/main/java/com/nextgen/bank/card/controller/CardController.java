package com.nextgen.bank.card.controller;

import com.nextgen.bank.card.dto.BlockCardRequestDto;
import com.nextgen.bank.card.dto.CardResponseDto;
import com.nextgen.bank.card.dto.IssueCardRequestDto;
import com.nextgen.bank.card.dto.UpdateCardLimitsRequestDto;
import com.nextgen.bank.card.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CardResponseDto> issueCard(@Valid @RequestBody IssueCardRequestDto requestDto) {
        CardResponseDto response = cardService.issueCard(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<List<CardResponseDto>> getCardsByCustomer(@RequestParam UUID customerId) {
        return ResponseEntity.ok(cardService.getCardsByCustomerId(customerId));
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<CardResponseDto> getCardById(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PostMapping("/{cardId}/activate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CardResponseDto> activateCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.activateCard(cardId));
    }

    @PostMapping("/{cardId}/block")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CardResponseDto> blockCard(@PathVariable UUID cardId,
                                                      @Valid @RequestBody BlockCardRequestDto requestDto) {
        return ResponseEntity.ok(cardService.blockCard(cardId, requestDto));
    }

    @PostMapping("/{cardId}/unblock")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CardResponseDto> unblockCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardService.unblockCard(cardId));
    }

    @PutMapping("/{cardId}/limits")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CardResponseDto> updateCardLimits(@PathVariable UUID cardId,
                                                             @RequestBody UpdateCardLimitsRequestDto requestDto) {
        return ResponseEntity.ok(cardService.updateCardLimits(cardId, requestDto));
    }
}

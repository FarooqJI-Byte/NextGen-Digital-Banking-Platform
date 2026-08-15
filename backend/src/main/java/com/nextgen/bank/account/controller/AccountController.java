package com.nextgen.bank.account.controller;

import com.nextgen.bank.account.dto.AccountHoldRequestDto;
import com.nextgen.bank.account.dto.AccountHoldResponseDto;
import com.nextgen.bank.account.dto.AccountResponseDto;
import com.nextgen.bank.account.dto.FreezeAccountRequestDto;
import com.nextgen.bank.account.dto.OpenAccountRequestDto;
import com.nextgen.bank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<AccountResponseDto> openAccount(@Valid @RequestBody OpenAccountRequestDto requestDto) {
        AccountResponseDto response = accountService.openAccount(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomer(@RequestParam UUID customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/{accountId}/balance")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable UUID accountId) {
        AccountResponseDto account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", account.accountId(),
                "balance", account.balance(),
                "availableBalance", account.availableBalance(),
                "currency", account.currency()
        ));
    }

    @PostMapping("/{accountId}/freeze")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<AccountResponseDto> freezeAccount(@PathVariable UUID accountId,
                                                             @Valid @RequestBody FreezeAccountRequestDto requestDto) {
        return ResponseEntity.ok(accountService.freezeAccount(accountId, requestDto));
    }

    @PostMapping("/{accountId}/unfreeze")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<AccountResponseDto> unfreezeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountId));
    }

    @PostMapping("/{accountId}/close")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<AccountResponseDto> closeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.closeAccount(accountId));
    }

    @PostMapping("/{accountId}/holds")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<AccountHoldResponseDto> placeHold(@PathVariable UUID accountId,
                                                             @Valid @RequestBody AccountHoldRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.placeHold(accountId, requestDto));
    }

    @DeleteMapping("/{accountId}/holds/{holdId}")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<Void> releaseHold(@PathVariable UUID accountId, @PathVariable UUID holdId) {
        accountService.releaseHold(accountId, holdId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}/holds")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<List<AccountHoldResponseDto>> getAccountHolds(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountHolds(accountId));
    }
}

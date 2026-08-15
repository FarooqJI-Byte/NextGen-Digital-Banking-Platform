package com.nextgen.bank.account.service.impl;

import com.nextgen.bank.account.domain.Account;
import com.nextgen.bank.account.domain.AccountHold;
import com.nextgen.bank.account.dto.AccountHoldRequestDto;
import com.nextgen.bank.account.dto.AccountHoldResponseDto;
import com.nextgen.bank.account.dto.AccountResponseDto;
import com.nextgen.bank.account.dto.FreezeAccountRequestDto;
import com.nextgen.bank.account.dto.OpenAccountRequestDto;
import com.nextgen.bank.account.repository.AccountHoldRepository;
import com.nextgen.bank.account.repository.AccountRepository;
import com.nextgen.bank.account.service.AccountService;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountHoldRepository holdRepository;
    private final OutboxEventWriter outboxEventWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(AccountRepository accountRepository,
                              AccountHoldRepository holdRepository,
                              OutboxEventWriter outboxEventWriter,
                              ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.holdRepository = holdRepository;
        this.outboxEventWriter = outboxEventWriter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AccountResponseDto openAccount(OpenAccountRequestDto requestDto) {
        String accountNumber = generateUniqueAccountNumber();

        Account account = new Account(
                accountNumber,
                requestDto.customerId(),
                requestDto.accountType(),
                requestDto.currency() != null ? requestDto.currency() : "INR"
        );
        account.activate();

        Account savedAccount = accountRepository.save(account);

        outboxEventWriter.write(
                "ACCOUNT",
                savedAccount.getAccountId().toString(),
                "AccountOpenedEvent",
                Map.of(
                        "accountId", savedAccount.getAccountId(),
                        "accountNumber", savedAccount.getAccountNumber(),
                        "customerId", savedAccount.getCustomerId(),
                        "accountType", savedAccount.getAccountType().name()
                )
        );

        return AccountResponseDto.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        return AccountResponseDto.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        return AccountResponseDto.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(AccountResponseDto::fromEntity)
                .toList();
    }

    @Override
    public AccountResponseDto freezeAccount(UUID accountId, FreezeAccountRequestDto requestDto) {
        Account account = findAccountOrThrow(accountId);
        String oldStatus = account.getStatus().name();
        account.freeze(requestDto.freezeReason());
        Account updated = accountRepository.save(account);

        outboxEventWriter.write(
                "ACCOUNT",
                updated.getAccountId().toString(),
                "AccountStatusChangedEvent",
                Map.of(
                        "accountId", updated.getAccountId(),
                        "oldStatus", oldStatus,
                        "newStatus", updated.getStatus().name(),
                        "reason", requestDto.freezeReason()
                )
        );

        return AccountResponseDto.fromEntity(updated);
    }

    @Override
    public AccountResponseDto unfreezeAccount(UUID accountId) {
        Account account = findAccountOrThrow(accountId);
        String oldStatus = account.getStatus().name();
        account.unfreeze();
        Account updated = accountRepository.save(account);

        outboxEventWriter.write(
                "ACCOUNT",
                updated.getAccountId().toString(),
                "AccountStatusChangedEvent",
                Map.of(
                        "accountId", updated.getAccountId(),
                        "oldStatus", oldStatus,
                        "newStatus", updated.getStatus().name(),
                        "reason", "Unfrozen by administrative request"
                )
        );

        return AccountResponseDto.fromEntity(updated);
    }

    @Override
    public AccountResponseDto closeAccount(UUID accountId) {
        Account account = findAccountOrThrow(accountId);

        List<AccountHold> activeHolds = holdRepository.findByAccountAccountIdAndStatus(accountId, "ACTIVE");
        if (!activeHolds.isEmpty()) {
            throw new BusinessException("Cannot close account with active holds", HttpStatus.BAD_REQUEST, "ACTIVE_HOLDS_EXIST");
        }

        String oldStatus = account.getStatus().name();
        account.requestClosure();
        account.close();
        Account updated = accountRepository.save(account);

        outboxEventWriter.write(
                "ACCOUNT",
                updated.getAccountId().toString(),
                "AccountStatusChangedEvent",
                Map.of(
                        "accountId", updated.getAccountId(),
                        "oldStatus", oldStatus,
                        "newStatus", updated.getStatus().name(),
                        "reason", "Account closed"
                )
        );

        return AccountResponseDto.fromEntity(updated);
    }

    @Override
    public AccountHoldResponseDto placeHold(UUID accountId, AccountHoldRequestDto requestDto) {
        Account account = findAccountOrThrow(accountId);

        if (account.getAvailableBalance().compareTo(requestDto.amount()) < 0) {
            throw new BusinessException("Insufficient available balance to place hold", HttpStatus.BAD_REQUEST, "INSUFFICIENT_HOLD_BALANCE");
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(requestDto.amount()));
        accountRepository.save(account);

        AccountHold hold = new AccountHold(account, requestDto.amount(), requestDto.reason());
        AccountHold savedHold = holdRepository.save(hold);

        return AccountHoldResponseDto.fromEntity(savedHold);
    }

    @Override
    public void releaseHold(UUID accountId, UUID holdId) {
        Account account = findAccountOrThrow(accountId);
        AccountHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found with ID: " + holdId));

        if (!hold.getAccount().getAccountId().equals(accountId)) {
            throw new BusinessException("Hold does not belong to specified account", HttpStatus.BAD_REQUEST, "HOLD_ACCOUNT_MISMATCH");
        }

        if ("RELEASED".equalsIgnoreCase(hold.getStatus())) {
            return;
        }

        hold.release();
        holdRepository.save(hold);

        account.setAvailableBalance(account.getAvailableBalance().add(hold.getAmount()));
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountHoldResponseDto> getAccountHolds(UUID accountId) {
        findAccountOrThrow(accountId);
        return holdRepository.findByAccountAccountId(accountId)
                .stream()
                .map(AccountHoldResponseDto::fromEntity)
                .toList();
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new BusinessException("Unable to generate unique account number", HttpStatus.INTERNAL_SERVER_ERROR, "GEN_ACC_NUM_FAILED");
            }
            long number = 501000000000L + Math.abs(random.nextLong() % 498999999999L);
            accountNumber = String.valueOf(number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}

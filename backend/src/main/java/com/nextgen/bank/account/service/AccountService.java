package com.nextgen.bank.account.service;

import com.nextgen.bank.account.dto.AccountHoldRequestDto;
import com.nextgen.bank.account.dto.AccountHoldResponseDto;
import com.nextgen.bank.account.dto.AccountResponseDto;
import com.nextgen.bank.account.dto.FreezeAccountRequestDto;
import com.nextgen.bank.account.dto.OpenAccountRequestDto;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto openAccount(OpenAccountRequestDto requestDto);

    AccountResponseDto getAccountById(UUID accountId);

    AccountResponseDto getAccountByNumber(String accountNumber);

    List<AccountResponseDto> getAccountsByCustomerId(UUID customerId);

    AccountResponseDto freezeAccount(UUID accountId, FreezeAccountRequestDto requestDto);

    AccountResponseDto unfreezeAccount(UUID accountId);

    AccountResponseDto closeAccount(UUID accountId);

    AccountHoldResponseDto placeHold(UUID accountId, AccountHoldRequestDto requestDto);

    void releaseHold(UUID accountId, UUID holdId);

    List<AccountHoldResponseDto> getAccountHolds(UUID accountId);
}

package com.nextgen.bank.account.repository;

import com.nextgen.bank.account.domain.AccountHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountHoldRepository extends JpaRepository<AccountHold, UUID> {

    List<AccountHold> findByAccountAccountIdAndStatus(UUID accountId, String status);

    List<AccountHold> findByAccountAccountId(UUID accountId);
}

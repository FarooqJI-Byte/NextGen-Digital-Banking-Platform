package com.nextgen.bank.card.repository;

import com.nextgen.bank.card.domain.Card;
import com.nextgen.bank.card.domain.CardType;
import com.nextgen.bank.common.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByCustomerId(UUID customerId);

    List<Card> findByAccountId(UUID accountId);

    // BR-CARD-001: check if an active (non-CANCELLED, non-EXPIRED) card of the same type already exists on this account
    boolean existsByAccountIdAndCardTypeAndStatusIn(UUID accountId, CardType cardType, List<CardStatus> statuses);
}

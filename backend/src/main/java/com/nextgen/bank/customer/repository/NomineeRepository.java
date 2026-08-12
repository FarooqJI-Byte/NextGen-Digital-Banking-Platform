package com.nextgen.bank.customer.repository;

import com.nextgen.bank.customer.domain.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NomineeRepository extends JpaRepository<Nominee, UUID> {

    List<Nominee> findByCustomerId(UUID customerId);
}

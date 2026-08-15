package com.nextgen.bank.customer.repository;

import com.nextgen.bank.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUserId(UUID userId);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    boolean existsByUserId(UUID userId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByCustomerNumber(String customerNumber);
}

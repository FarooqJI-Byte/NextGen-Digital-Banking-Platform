package com.nextgen.bank.customer.repository;

import com.nextgen.bank.customer.domain.CustomerAddress;
import com.nextgen.bank.customer.domain.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerId(UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndAddressType(UUID customerId, AddressType addressType);
}

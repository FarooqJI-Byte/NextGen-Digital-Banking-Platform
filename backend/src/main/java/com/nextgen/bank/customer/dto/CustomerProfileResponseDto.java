package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.Customer;
import com.nextgen.bank.customer.domain.enums.RiskCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerProfileResponseDto(
        UUID customerId,
        String customerNumber,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        String email,
        KYCStatus kycStatus,
        RiskCategory riskCategory,
        List<CustomerAddressDto> addresses,
        List<NomineeDto> nominees,
        Instant createdAt,
        boolean hasSubmittedKycDocument
) {
    public CustomerProfileResponseDto(
            UUID customerId,
            UUID userId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String phone,
            String email,
            KYCStatus kycStatus,
            RiskCategory riskCategory,
            List<CustomerAddressDto> addresses,
            List<NomineeDto> nominees,
            Instant createdAt
    ) {
        this(customerId, null, userId, firstName, lastName, dateOfBirth, phone, email, kycStatus, riskCategory, addresses, nominees, createdAt, false);
    }

    public CustomerProfileResponseDto(
            UUID customerId,
            String customerNumber,
            UUID userId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String phone,
            String email,
            KYCStatus kycStatus,
            RiskCategory riskCategory,
            List<CustomerAddressDto> addresses,
            List<NomineeDto> nominees,
            Instant createdAt
    ) {
        this(customerId, customerNumber, userId, firstName, lastName, dateOfBirth, phone, email, kycStatus, riskCategory, addresses, nominees, createdAt, false);
    }

    public static CustomerProfileResponseDto fromEntity(
            Customer customer,
            List<CustomerAddressDto> addresses,
            List<NomineeDto> nominees
    ) {
        return fromEntity(customer, addresses, nominees, false);
    }

    public static CustomerProfileResponseDto fromEntity(
            Customer customer,
            List<CustomerAddressDto> addresses,
            List<NomineeDto> nominees,
            boolean hasSubmittedKycDocument
    ) {
        return new CustomerProfileResponseDto(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getKycStatus(),
                customer.getRiskCategory(),
                addresses,
                nominees,
                customer.getCreatedAt(),
                hasSubmittedKycDocument
        );
    }
}

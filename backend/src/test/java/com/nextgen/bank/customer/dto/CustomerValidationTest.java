package com.nextgen.bank.customer.dto;

import com.nextgen.bank.customer.domain.enums.AddressType;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid CreateCustomerProfileRequestDto should pass all validations")
    void testValidProfileRequest_Passes() {
        CreateCustomerProfileRequestDto dto = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                Collections.emptyList(),
                Collections.emptyList()
        );

        Set<ConstraintViolation<CreateCustomerProfileRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Invalid Indian mobile number format in profile request should fail validation")
    void testInvalidPhoneFormat_Fails() {
        CreateCustomerProfileRequestDto dto = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "12345", // Invalid phone
                "john.doe@example.com",
                Collections.emptyList(),
                Collections.emptyList()
        );

        Set<ConstraintViolation<CreateCustomerProfileRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    @DisplayName("Invalid email format in profile request should fail validation")
    void testInvalidEmailFormat_Fails() {
        CreateCustomerProfileRequestDto dto = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "invalid-email",
                Collections.emptyList(),
                Collections.emptyList()
        );

        Set<ConstraintViolation<CreateCustomerProfileRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Valid postal code (6-digit Indian PIN) in address should pass validation")
    void testValidPostalCode_Passes() {
        CustomerAddressDto addressDto = new CustomerAddressDto(
                AddressType.PERMANENT,
                "123 Main St",
                "Mumbai",
                "Maharashtra",
                "400001",
                "India"
        );

        Set<ConstraintViolation<CustomerAddressDto>> violations = validator.validate(addressDto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Invalid postal code (e.g. 5 digits or letters) in address should fail validation")
    void testInvalidPostalCode_Fails() {
        CustomerAddressDto addressDto = new CustomerAddressDto(
                AddressType.PERMANENT,
                "123 Main St",
                "Mumbai",
                "Maharashtra",
                "01234", // Invalid PIN (starts with 0 / 5 digits)
                "India"
        );

        Set<ConstraintViolation<CustomerAddressDto>> violations = validator.validate(addressDto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("postalCode"));
    }

    @Test
    @DisplayName("Nominee with allocation percentage > 100.00 should fail validation")
    void testNomineeAllocationOver100_Fails() {
        NomineeDto nomineeDto = new NomineeDto(
                "Jane Doe",
                "Spouse",
                LocalDate.of(1992, 8, 20),
                "+919876543211",
                new BigDecimal("105.00") // Over 100%
        );

        Set<ConstraintViolation<NomineeDto>> violations = validator.validate(nomineeDto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("allocationPercentage"));
    }

    @Test
    @DisplayName("Nominee with allocation percentage <= 0 should fail validation")
    void testNomineeAllocationZeroOrNegative_Fails() {
        NomineeDto nomineeDto = new NomineeDto(
                "Jane Doe",
                "Spouse",
                LocalDate.of(1992, 8, 20),
                "+919876543211",
                new BigDecimal("0.00") // 0%
        );

        Set<ConstraintViolation<NomineeDto>> violations = validator.validate(nomineeDto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("allocationPercentage"));
    }

    @Test
    @DisplayName("Valid KYCSubmissionRequestDto should pass validation")
    void testValidKycSubmission_Passes() {
        KYCSubmissionRequestDto dto = new KYCSubmissionRequestDto(
                DocumentType.PAN,
                "ABCDE1234F",
                "s3://bank-kyc-docs/pan_john.pdf"
        );

        Set<ConstraintViolation<KYCSubmissionRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}

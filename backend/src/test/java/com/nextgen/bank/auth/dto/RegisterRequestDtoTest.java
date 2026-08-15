package com.nextgen.bank.auth.dto;

import com.nextgen.bank.common.enums.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid registration request with explicit role should pass validations (BR-AUTH-001)")
    void testValidPassword_Passes() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "StrongPass123!",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Valid registration request without role should pass validations")
    void testValidRegistrationWithoutRole_Passes() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "StrongPass123!"
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
        assertThat(dto.role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Password shorter than 8 characters should fail BR-AUTH-001")
    void testPasswordTooShort_Fails() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "Sh1!a",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without uppercase character should fail BR-AUTH-001")
    void testPasswordMissingUppercase_Fails() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "password123!",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without digit should fail BR-AUTH-001")
    void testPasswordMissingDigit_Fails() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "Password!",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Password without special character should fail BR-AUTH-001")
    void testPasswordMissingSpecialChar_Fails() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "Password123",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Invalid email format should fail validation")
    void testInvalidEmail_Fails() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "johndoe",
                "not-an-email",
                "Password123!",
                UserRole.CUSTOMER
        );

        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}

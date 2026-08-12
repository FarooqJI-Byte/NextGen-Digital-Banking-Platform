package com.nextgen.bank.customer.dto;

import com.nextgen.bank.customer.domain.Nominee;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NomineeDto(
        @NotBlank(message = "Nominee full name is required")
        String fullName,

        @NotBlank(message = "Relationship is required")
        String relationship,

        @NotNull(message = "Nominee date of birth is required")
        LocalDate dateOfBirth,

        @NotBlank(message = "Nominee phone is required")
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "Phone must be a valid Indian mobile number (+91XXXXXXXXXX)")
        String phone,

        @NotNull(message = "Allocation percentage is required")
        @DecimalMin(value = "0.01", message = "Allocation percentage must be greater than 0")
        @DecimalMax(value = "100.00", message = "Allocation percentage cannot exceed 100")
        BigDecimal allocationPercentage
) {
    public static NomineeDto fromEntity(Nominee entity) {
        return new NomineeDto(
                entity.getFullName(),
                entity.getRelationship(),
                entity.getDateOfBirth(),
                entity.getPhone(),
                entity.getAllocationPercentage()
        );
    }
}

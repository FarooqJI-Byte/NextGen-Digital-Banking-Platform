package com.nextgen.bank.customer.dto;

import com.nextgen.bank.customer.domain.CustomerAddress;
import com.nextgen.bank.customer.domain.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CustomerAddressDto(
        @NotNull(message = "Address type is required")
        AddressType addressType,

        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Postal code must be a valid 6-digit Indian PIN code")
        String postalCode,

        String country
) {
    public static CustomerAddressDto fromEntity(CustomerAddress entity) {
        return new CustomerAddressDto(
                entity.getAddressType(),
                entity.getStreet(),
                entity.getCity(),
                entity.getState(),
                entity.getPostalCode(),
                entity.getCountry()
        );
    }
}

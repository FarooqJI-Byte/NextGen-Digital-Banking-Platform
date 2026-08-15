package com.nextgen.bank.customer.controller;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.dto.CreateCustomerProfileRequestDto;
import com.nextgen.bank.customer.dto.CustomerProfileResponseDto;
import com.nextgen.bank.customer.dto.KYCSubmissionRequestDto;
import com.nextgen.bank.customer.dto.KYCSubmissionResponseDto;
import com.nextgen.bank.customer.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    public CustomerController(CustomerService customerService, UserRepository userRepository) {
        this.customerService = customerService;
        this.userRepository = userRepository;
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponseDto> createProfile(
            @Valid @RequestBody CreateCustomerProfileRequestDto requestDto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID authenticatedUserId = getAuthenticatedUserId(request, authentication);
        CustomerProfileResponseDto response = customerService.createCustomerProfile(requestDto, authenticatedUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_STAFF', 'ADMIN')")
    public ResponseEntity<CustomerProfileResponseDto> getProfile(
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID authenticatedUserId = getAuthenticatedUserId(request, authentication);
        CustomerProfileResponseDto response = customerService.getCustomerProfile(authenticatedUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<KYCSubmissionResponseDto> uploadKyc(
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("documentNumber") String documentNumber,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID authenticatedUserId = getAuthenticatedUserId(request, authentication);
        KYCSubmissionResponseDto response = customerService.uploadKycDocument(documentType, documentNumber, file, authenticatedUserId);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping(value = "/kyc", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<KYCSubmissionResponseDto> submitKyc(
            @Valid @RequestBody KYCSubmissionRequestDto requestDto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID authenticatedUserId = getAuthenticatedUserId(request, authentication);
        KYCSubmissionResponseDto response = customerService.submitKyc(requestDto, authenticatedUserId);
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    private UUID getAuthenticatedUserId(HttpServletRequest request, Authentication authentication) {
        if (request != null) {
            Object attr = request.getAttribute("authenticatedUserId");
            if (attr instanceof UUID uuid) {
                return uuid;
            }
        }
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByUsername(authentication.getName())
                    .map(User::getUserId)
                    .orElseThrow(() -> new BusinessException("Authenticated user not found", HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        }
        throw new BusinessException("User is not authenticated", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}

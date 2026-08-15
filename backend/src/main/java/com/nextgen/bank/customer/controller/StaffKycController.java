package com.nextgen.bank.customer.controller;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.customer.dto.KYCVerificationRequestDto;
import com.nextgen.bank.customer.dto.KYCVerificationResponseDto;
import com.nextgen.bank.customer.dto.PendingKycItemDto;
import com.nextgen.bank.customer.dto.StaffCustomerKycDetailDto;
import com.nextgen.bank.customer.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/kyc")
public class StaffKycController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

    public StaffKycController(CustomerService customerService, UserRepository userRepository) {
        this.customerService = customerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<List<PendingKycItemDto>> getPendingKycQueue() {
        List<PendingKycItemDto> queue = customerService.getPendingKycQueue();
        return ResponseEntity.ok(queue);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<StaffCustomerKycDetailDto> getStaffCustomerKycDetail(@PathVariable UUID customerId) {
        StaffCustomerKycDetailDto detail = customerService.getStaffCustomerKycDetail(customerId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('BANK_STAFF', 'ADMIN')")
    public ResponseEntity<KYCVerificationResponseDto> verifyKyc(
            @Valid @RequestBody KYCVerificationRequestDto requestDto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID staffUserId = getAuthenticatedUserId(request, authentication);
        KYCVerificationResponseDto response = customerService.verifyKyc(requestDto, staffUserId);
        return ResponseEntity.ok(response);
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
                    .orElseThrow(() -> new BusinessException("Authenticated staff user not found", HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        }
        throw new BusinessException("User is not authenticated", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}

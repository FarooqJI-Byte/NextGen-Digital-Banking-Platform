package com.nextgen.bank.auth.controller;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.dto.CreateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffUserResponseDto;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.auth.service.AdminStaffService;
import com.nextgen.bank.common.exception.BusinessException;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;
    private final UserRepository userRepository;

    public AdminStaffController(AdminStaffService adminStaffService, UserRepository userRepository) {
        this.adminStaffService = adminStaffService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StaffUserResponseDto> createStaff(
            @Valid @RequestBody CreateStaffRequestDto requestDto,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID adminUserId = getAuthenticatedUserId(request, authentication);
        StaffUserResponseDto response = adminStaffService.createStaff(requestDto, adminUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{userId}/resend-activation")
    public ResponseEntity<StaffUserResponseDto> resendStaffActivation(
            @PathVariable UUID userId,
            HttpServletRequest request,
            Authentication authentication
    ) {
        UUID adminUserId = getAuthenticatedUserId(request, authentication);
        StaffUserResponseDto response = adminStaffService.resendStaffActivation(userId, adminUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StaffUserResponseDto>> listStaff() {
        List<StaffUserResponseDto> response = adminStaffService.listStaff();
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
                    .orElseThrow(() -> new BusinessException("Authenticated admin not found", HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        }
        throw new BusinessException("User is not authenticated", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}

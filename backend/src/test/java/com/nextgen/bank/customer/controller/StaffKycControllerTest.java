package com.nextgen.bank.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.GlobalExceptionHandler;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.domain.enums.RiskCategory;
import com.nextgen.bank.customer.dto.KYCVerificationRequestDto;
import com.nextgen.bank.customer.dto.KYCVerificationResponseDto;
import com.nextgen.bank.customer.dto.PendingKycItemDto;
import com.nextgen.bank.customer.dto.StaffCustomerKycDetailDto;
import com.nextgen.bank.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StaffKycControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private CustomerService customerService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StaffKycController staffKycController;

    private UUID staffUserId;
    private UUID customerId;
    private UsernamePasswordAuthenticationToken staffPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(staffKycController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        staffUserId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        staffPrincipal = new UsernamePasswordAuthenticationToken(
                "bankstaff1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BANK_STAFF"))
        );
    }

    @Test
    @DisplayName("GET /api/v1/staff/kyc/pending should return 200 OK with pending queue items")
    void testGetPendingKycQueue_Success() throws Exception {
        PendingKycItemDto item = new PendingKycItemDto(
                customerId,
                "CUST-7K4P9M2Q",
                "John Doe",
                "john.doe@example.com",
                "+919876543210",
                KYCStatus.PENDING,
                UUID.randomUUID(),
                DocumentType.PAN,
                "uploads/kyc/doc_123.jpg",
                Instant.now()
        );

        when(customerService.getPendingKycQueue()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/staff/kyc/pending")
                        .requestAttr("authenticatedUserId", staffUserId)
                        .principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].customerNumber").value("CUST-7K4P9M2Q"))
                .andExpect(jsonPath("$[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$[0].documentType").value("PAN"));
    }

    @Test
    @DisplayName("GET /api/v1/staff/kyc/{customerId} should return 200 OK with detailed customer KYC information")
    void testGetStaffCustomerKycDetail_Success() throws Exception {
        StaffCustomerKycDetailDto detail = new StaffCustomerKycDetailDto(
                customerId,
                "CUST-7K4P9M2Q",
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "john.doe@example.com",
                "+919876543210",
                KYCStatus.PENDING,
                RiskCategory.MEDIUM,
                Instant.now(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(customerService.getStaffCustomerKycDetail(customerId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/staff/kyc/" + customerId)
                        .requestAttr("authenticatedUserId", staffUserId)
                        .principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerNumber").value("CUST-7K4P9M2Q"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/staff/kyc/verify should return 200 OK when staff approves KYC")
    void testVerifyKyc_Approve_Success() throws Exception {
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.VERIFIED,
                "PAN card verified with NSDL portal"
        );

        KYCVerificationResponseDto response = new KYCVerificationResponseDto(
                customerId,
                KYCStatus.VERIFIED,
                Instant.now()
        );

        when(customerService.verifyKyc(any(KYCVerificationRequestDto.class), eq(staffUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/staff/kyc/verify")
                        .requestAttr("authenticatedUserId", staffUserId)
                        .principal(staffPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.kycStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("POST /api/v1/staff/kyc/verify should return 200 OK when staff rejects KYC")
    void testVerifyKyc_Reject_Success() throws Exception {
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.REJECTED,
                "Document photo illegible"
        );

        KYCVerificationResponseDto response = new KYCVerificationResponseDto(
                customerId,
                KYCStatus.REJECTED,
                Instant.now()
        );

        when(customerService.verifyKyc(any(KYCVerificationRequestDto.class), eq(staffUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/staff/kyc/verify")
                        .requestAttr("authenticatedUserId", staffUserId)
                        .principal(staffPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.kycStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/v1/staff/kyc/verify should return 422 Unprocessable Entity when no docs exist")
    void testVerifyKyc_NoDocuments() throws Exception {
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.VERIFIED,
                "Verify"
        );

        when(customerService.verifyKyc(any(KYCVerificationRequestDto.class), eq(staffUserId)))
                .thenThrow(new BusinessException("No KYC documents found", HttpStatus.UNPROCESSABLE_ENTITY, "NO_KYC_DOCUMENTS"));

        mockMvc.perform(post("/api/v1/staff/kyc/verify")
                        .requestAttr("authenticatedUserId", staffUserId)
                        .principal(staffPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("NO_KYC_DOCUMENTS"));
    }
}

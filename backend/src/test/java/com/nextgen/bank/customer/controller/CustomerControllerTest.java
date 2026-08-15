package com.nextgen.bank.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.GlobalExceptionHandler;
import com.nextgen.bank.customer.domain.enums.AddressType;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.domain.enums.RiskCategory;
import com.nextgen.bank.customer.dto.CreateCustomerProfileRequestDto;
import com.nextgen.bank.customer.dto.CustomerAddressDto;
import com.nextgen.bank.customer.dto.CustomerProfileResponseDto;
import com.nextgen.bank.customer.dto.KYCSubmissionRequestDto;
import com.nextgen.bank.customer.dto.KYCSubmissionResponseDto;
import com.nextgen.bank.customer.dto.NomineeDto;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private CustomerService customerService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerController customerController;

    private UUID userId;
    private UUID customerId;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        authPrincipal = new UsernamePasswordAuthenticationToken(
                "customer1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/customers/profile should return 201 Created on valid input")
    void testCreateProfile_Success() throws Exception {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                List.of(new CustomerAddressDto(AddressType.PERMANENT, "123 MG Road", "Bengaluru", "Karnataka", "560001", "India")),
                List.of(new NomineeDto("Jane Doe", "Spouse", LocalDate.of(1992, 8, 20), "+919876543211", new BigDecimal("100.00")))
        );

        CustomerProfileResponseDto response = new CustomerProfileResponseDto(
                customerId, userId, "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210",
                "john.doe@example.com", KYCStatus.PENDING, RiskCategory.MEDIUM, Collections.emptyList(), Collections.emptyList(), Instant.now()
        );

        when(customerService.createCustomerProfile(any(CreateCustomerProfileRequestDto.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/profile")
                        .requestAttr("authenticatedUserId", userId)
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/customers/profile should return 409 Conflict when profile already exists")
    void testCreateProfile_AlreadyExists_Fails() throws Exception {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210", "john.doe@example.com",
                Collections.emptyList(), Collections.emptyList()
        );

        when(customerService.createCustomerProfile(any(CreateCustomerProfileRequestDto.class), eq(userId)))
                .thenThrow(new BusinessException("Customer profile already exists for this user account", HttpStatus.CONFLICT, "CUSTOMER_PROFILE_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/customers/profile")
                        .requestAttr("authenticatedUserId", userId)
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_PROFILE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/profile should return 200 OK with customer profile")
    void testGetProfile_Success() throws Exception {
        CustomerProfileResponseDto response = new CustomerProfileResponseDto(
                customerId, userId, "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210",
                "john.doe@example.com", KYCStatus.PENDING, RiskCategory.MEDIUM, Collections.emptyList(), Collections.emptyList(), Instant.now()
        );

        when(customerService.getCustomerProfile(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/profile")
                        .requestAttr("authenticatedUserId", userId)
                        .principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/customers/kyc (Multipart) should return 202 Accepted on valid KYC document upload")
    void testUploadKyc_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes()
        );

        UUID docId = UUID.randomUUID();
        KYCSubmissionResponseDto response = new KYCSubmissionResponseDto(
                docId,
                KYCStatus.PENDING,
                Instant.now()
        );

        when(customerService.uploadKycDocument(eq(DocumentType.PAN), eq("ABCDE1234F"), any(), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/customers/kyc")
                        .file(file)
                        .param("documentType", "PAN")
                        .param("documentNumber", "ABCDE1234F")
                        .requestAttr("authenticatedUserId", userId)
                        .principal(authPrincipal))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/customers/kyc (JSON) should return 202 Accepted on valid KYC document submission")
    void testSubmitKyc_Success() throws Exception {
        KYCSubmissionRequestDto request = new KYCSubmissionRequestDto(
                DocumentType.PAN,
                "ABCDE1234F",
                "s3://bank-kyc-docs/pan_john.pdf"
        );

        UUID docId = UUID.randomUUID();
        KYCSubmissionResponseDto response = new KYCSubmissionResponseDto(
                docId,
                KYCStatus.PENDING,
                Instant.now()
        );

        when(customerService.submitKyc(any(KYCSubmissionRequestDto.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/kyc")
                        .requestAttr("authenticatedUserId", userId)
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));
    }
}

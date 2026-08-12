package com.nextgen.bank.customer.service;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.ResourceNotFoundException;
import com.nextgen.bank.customer.domain.Customer;
import com.nextgen.bank.customer.domain.CustomerAddress;
import com.nextgen.bank.customer.domain.KYCDocument;
import com.nextgen.bank.customer.domain.Nominee;
import com.nextgen.bank.customer.domain.enums.AddressType;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.domain.enums.RiskCategory;
import com.nextgen.bank.customer.domain.enums.VerificationStatus;
import com.nextgen.bank.customer.dto.CreateCustomerProfileRequestDto;
import com.nextgen.bank.customer.dto.CustomerAddressDto;
import com.nextgen.bank.customer.dto.CustomerProfileResponseDto;
import com.nextgen.bank.customer.dto.KYCSubmissionRequestDto;
import com.nextgen.bank.customer.dto.KYCSubmissionResponseDto;
import com.nextgen.bank.customer.dto.KYCVerificationRequestDto;
import com.nextgen.bank.customer.dto.KYCVerificationResponseDto;
import com.nextgen.bank.customer.dto.NomineeDto;
import com.nextgen.bank.customer.event.CustomerCreatedEvent;
import com.nextgen.bank.customer.event.KYCApprovedEvent;
import com.nextgen.bank.customer.event.KYCRejectedEvent;
import com.nextgen.bank.customer.event.KYCSubmittedEvent;
import com.nextgen.bank.customer.repository.CustomerAddressRepository;
import com.nextgen.bank.customer.repository.CustomerRepository;
import com.nextgen.bank.customer.repository.KYCDocumentRepository;
import com.nextgen.bank.customer.repository.NomineeRepository;
import com.nextgen.bank.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private KYCDocumentRepository kycDocumentRepository;

    @Mock
    private NomineeRepository nomineeRepository;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private UUID userId;
    private UUID customerId;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        sampleCustomer = new Customer(
                userId,
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                KYCStatus.PENDING,
                RiskCategory.MEDIUM
        );
        sampleCustomer.setCustomerId(customerId);
        sampleCustomer.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should successfully create customer profile with address, nominee and outbox event")
    void testCreateCustomerProfile_Success() {
        CustomerAddressDto addressDto = new CustomerAddressDto(
                AddressType.PERMANENT,
                "123 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                "India"
        );

        NomineeDto nomineeDto = new NomineeDto(
                "Jane Doe",
                "Spouse",
                LocalDate.of(1992, 8, 20),
                "+919876543211",
                new BigDecimal("100.00")
        );

        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                List.of(addressDto),
                List.of(nomineeDto)
        );

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+919876543210")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerAddress savedAddress = new CustomerAddress(
                customerId,
                AddressType.PERMANENT,
                "123 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                "India"
        );
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(savedAddress);

        Nominee savedNominee = new Nominee(
                customerId,
                "Jane Doe",
                "Spouse",
                LocalDate.of(1992, 8, 20),
                "+919876543211",
                new BigDecimal("100.00")
        );
        when(nomineeRepository.save(any(Nominee.class))).thenReturn(savedNominee);

        CustomerProfileResponseDto response = customerService.createCustomerProfile(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.addresses()).hasSize(1);
        assertThat(response.nominees()).hasSize(1);
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.PENDING);

        verify(outboxEventWriter).write(any(CustomerCreatedEvent.class));
    }

    @Test
    @DisplayName("Should reject profile creation if customer is underage (< 18 years) (BR-CUST-003)")
    void testCreateCustomerProfile_Underage_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "Minor",
                "User",
                LocalDate.now().minusYears(16),
                "+919876543210",
                "minor@example.com",
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("minor@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+919876543210")).thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least 18 years old")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(customerRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Should reject profile creation if nominee allocation does not equal 100.00% (BR-CUST-004)")
    void testCreateCustomerProfile_InvalidNomineeAllocation_Fails() {
        NomineeDto nomineeDto = new NomineeDto(
                "Jane Doe",
                "Spouse",
                LocalDate.of(1992, 8, 20),
                "+919876543211",
                new BigDecimal("80.00") // Only 80%
        );

        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                Collections.emptyList(),
                List.of(nomineeDto)
        );

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+919876543210")).thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100.00%")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject profile creation if user already has a customer profile (BR-CUST-001)")
    void testCreateCustomerProfile_DuplicateUserId_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210", "john.doe@example.com",
                Collections.emptyList(), Collections.emptyList()
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Should reject profile creation if email is already registered (BR-CUST-001)")
    void testCreateCustomerProfile_DuplicateEmail_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210", "john.doe@example.com",
                Collections.emptyList(), Collections.emptyList()
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email is already registered")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Should reject profile creation if phone is already registered (BR-CUST-001)")
    void testCreateCustomerProfile_DuplicatePhone_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 5, 15), "+919876543210", "john.doe@example.com",
                Collections.emptyList(), Collections.emptyList()
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+919876543210")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phone number is already registered")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Should successfully get customer profile by authenticated userId")
    void testGetCustomerProfile_Success() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        CustomerProfileResponseDto response = customerService.getCustomerProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer profile does not exist")
    void testGetCustomerProfile_NotFound() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should successfully submit KYC document and emit KYCSubmittedEvent")
    void testSubmitKyc_Success() {
        KYCSubmissionRequestDto request = new KYCSubmissionRequestDto(
                DocumentType.PAN,
                "ABCDE1234F",
                "s3://bank-kyc-docs/pan_john.pdf"
        );

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.existsByDocumentTypeAndDocumentNumberEnc(any(DocumentType.class), anyString())).thenReturn(false);
        when(kycDocumentRepository.findByCustomerIdAndDocumentType(customerId, DocumentType.PAN)).thenReturn(Optional.empty());

        UUID docId = UUID.randomUUID();
        KYCDocument savedDoc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "s3://bank-kyc-docs/pan_john.pdf");
        savedDoc.setDocumentId(docId);
        when(kycDocumentRepository.save(any(KYCDocument.class))).thenReturn(savedDoc);

        KYCSubmissionResponseDto response = customerService.submitKyc(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(docId);
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.PENDING);

        verify(outboxEventWriter).write(any(KYCSubmittedEvent.class));
    }

    @Test
    @DisplayName("Should reject KYC submission if document number is already registered globally")
    void testSubmitKyc_DuplicateDocument_Fails() {
        KYCSubmissionRequestDto request = new KYCSubmissionRequestDto(
                DocumentType.PAN,
                "ABCDE1234F",
                "s3://bank-kyc-docs/pan_john.pdf"
        );

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.existsByDocumentTypeAndDocumentNumberEnc(any(DocumentType.class), anyString())).thenReturn(true);

        assertThatThrownBy(() -> customerService.submitKyc(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(kycDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully verify KYC documents as BANK_STAFF/ADMIN and emit KYCApprovedEvent")
    void testVerifyKyc_Approve_Success() {
        UUID staffId = UUID.randomUUID();
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.VERIFIED,
                "PAN verified with NSDL portal"
        );

        KYCDocument pendingDoc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "s3://pan.pdf");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(List.of(pendingDoc));

        KYCVerificationResponseDto response = customerService.verifyKyc(request, staffId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.VERIFIED);
        assertThat(sampleCustomer.getKycStatus()).isEqualTo(KYCStatus.VERIFIED);
        assertThat(pendingDoc.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(pendingDoc.getVerifiedBy()).isEqualTo(staffId);

        verify(outboxEventWriter).write(any(KYCApprovedEvent.class));
    }

    @Test
    @DisplayName("Should successfully reject KYC documents and emit KYCRejectedEvent")
    void testVerifyKyc_Reject_Success() {
        UUID staffId = UUID.randomUUID();
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.REJECTED,
                "PAN card image blurred"
        );

        KYCDocument pendingDoc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "s3://pan.pdf");
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(List.of(pendingDoc));

        KYCVerificationResponseDto response = customerService.verifyKyc(request, staffId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(sampleCustomer.getKycStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(pendingDoc.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);

        verify(outboxEventWriter).write(any(KYCRejectedEvent.class));
    }

    @Test
    @DisplayName("Should reject KYC verification when customer has no documents uploaded")
    void testVerifyKyc_NoDocuments_Fails() {
        UUID staffId = UUID.randomUUID();
        KYCVerificationRequestDto request = new KYCVerificationRequestDto(
                customerId,
                KYCStatus.VERIFIED,
                "Approved"
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> customerService.verifyKyc(request, staffId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No KYC documents found")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    @DisplayName("Should correctly return KYC verification status for cross-module queries (Account/Loan)")
    void testGetKycStatusAndIsVerified() {
        sampleCustomer.setKycStatus(KYCStatus.VERIFIED);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getKYCStatus(customerId)).isEqualTo(KYCStatus.VERIFIED);
        assertThat(customerService.isCustomerKycVerified(customerId)).isTrue();
    }
}

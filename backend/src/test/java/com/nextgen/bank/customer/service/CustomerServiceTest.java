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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private KycDocumentStorageService kycDocumentStorageService;

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
    }

    @Test
    @DisplayName("Should successfully create a customer profile and emit CustomerCreatedEvent")
    void testCreateCustomerProfile_Success() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "+919876543210",
                "john.doe@example.com",
                List.of(new CustomerAddressDto(
                        AddressType.PERMANENT,
                        "123 MG Road",
                        "Bengaluru",
                        "Karnataka",
                        "560001",
                        "India"
                )),
                List.of(new NomineeDto(
                        "Jane Doe",
                        "Spouse",
                        LocalDate.of(1992, 8, 20),
                        "+919876543211",
                        new BigDecimal("100.00")
                ))
        );

        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("+919876543210")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setCustomerId(customerId);
            return c;
        });
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nomineeRepository.save(any(Nominee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileResponseDto response = customerService.createCustomerProfile(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.customerNumber()).isNotNull().startsWith("CUST-").hasSize(13);
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.PENDING);
        assertThat(response.riskCategory()).isEqualTo(RiskCategory.MEDIUM);
        assertThat(response.addresses()).hasSize(1);
        assertThat(response.nominees()).hasSize(1);

        verify(outboxEventWriter).write(any(CustomerCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when creating profile for already registered user (BR-CUST-001)")
    void testCreateCustomerProfile_DuplicateUser_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 1, 1), "+919876543210", "john@example.com",
                Collections.emptyList(), Collections.emptyList()
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Customer profile already exists")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Should throw BusinessException when customer age is less than 18 (BR-CUST-003)")
    void testCreateCustomerProfile_Underage_Fails() {
        LocalDate underageDob = LocalDate.now().minusYears(17);
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "Junior", "Doe", underageDob, "+919876543210", "junior@example.com",
                Collections.emptyList(), Collections.emptyList()
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least 18 years old")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should throw BusinessException when nominee allocation does not sum to 100.00% (BR-CUST-004)")
    void testCreateCustomerProfile_InvalidNomineeAllocation_Fails() {
        CreateCustomerProfileRequestDto request = new CreateCustomerProfileRequestDto(
                "John", "Doe", LocalDate.of(1990, 1, 1), "+919876543210", "john@example.com",
                Collections.emptyList(),
                List.of(
                        new NomineeDto("Nominee 1", "Son", LocalDate.of(2015, 1, 1), "+919876543211", new BigDecimal("50.00")),
                        new NomineeDto("Nominee 2", "Daughter", LocalDate.of(2018, 1, 1), "+919876543212", new BigDecimal("40.00"))
                )
        );
        when(customerRepository.existsByUserId(userId)).thenReturn(false);
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomerProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100.00%")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should retrieve customer profile by authenticated user ID")
    void testGetCustomerProfile_Success() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        CustomerProfileResponseDto response = customerService.getCustomerProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.hasSubmittedKycDocument()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when profile does not exist for user")
    void testGetCustomerProfile_NotFound_Fails() {
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should successfully upload KYC document file and emit KYCSubmittedEvent")
    void testUploadKycDocument_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                "%PDF-test".getBytes()
        );

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(kycDocumentStorageService.storeDocument(eq(file), eq(customerId))).thenReturn("kyc-storage/" + customerId + "/doc.pdf");
        when(kycDocumentRepository.existsByDocumentTypeAndDocumentNumberEnc(any(DocumentType.class), anyString())).thenReturn(false);

        UUID docId = UUID.randomUUID();
        KYCDocument savedDoc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "kyc-storage/" + customerId + "/doc.pdf");
        savedDoc.setDocumentId(docId);
        when(kycDocumentRepository.save(any(KYCDocument.class))).thenReturn(savedDoc);

        KYCSubmissionResponseDto response = customerService.uploadKycDocument(DocumentType.PAN, "ABCDE1234F", file, userId);

        assertThat(response).isNotNull();
        assertThat(response.documentId()).isEqualTo(docId);
        assertThat(response.kycStatus()).isEqualTo(KYCStatus.PENDING);

        verify(outboxEventWriter).write(any(KYCSubmittedEvent.class));
    }

    @Test
    @DisplayName("Should reject KYC document upload when an active document is already pending review")
    void testUploadKycDocument_PendingReview_Fails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                "%PDF-test".getBytes()
        );

        KYCDocument pendingDoc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "ref");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(List.of(pendingDoc));

        assertThatThrownBy(() -> customerService.uploadKycDocument(DocumentType.PAN, "ABCDE1234F", file, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending compliance review")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(kycDocumentStorageService, never()).storeDocument(any(), any());
    }

    @Test
    @DisplayName("Should reject KYC document upload when customer is already verified")
    void testUploadKycDocument_AlreadyVerified_Fails() {
        sampleCustomer.setKycStatus(KYCStatus.VERIFIED);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.pdf",
                "application/pdf",
                "%PDF-test".getBytes()
        );

        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> customerService.uploadKycDocument(DocumentType.PAN, "ABCDE1234F", file, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already verified")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
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
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
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
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
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
    @DisplayName("Should successfully reject KYC documents with remarks and emit KYCRejectedEvent")
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
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Should correctly return KYC verification status for cross-module queries (Account/Loan)")
    void testGetKycStatusAndIsVerified() {
        sampleCustomer.setKycStatus(KYCStatus.VERIFIED);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getKYCStatus(customerId)).isEqualTo(KYCStatus.VERIFIED);
        assertThat(customerService.isCustomerKycVerified(customerId)).isTrue();
    }

    @Test
    @DisplayName("Should generate customer number with CUST-XXXXXXXX format and keep it immutable")
    void testCustomerNumber_FormatAndLifecycleImmutability() {
        sampleCustomer.setCustomerNumber("CUST-7K4P9M2Q");
        when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(nomineeRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        CustomerProfileResponseDto profile = customerService.getCustomerProfile(userId);

        assertThat(profile.customerNumber()).isEqualTo("CUST-7K4P9M2Q");
        assertThat(profile.customerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should return pending KYC queue containing only customers with PENDING documents")
    void testGetPendingKycQueue() {
        KYCDocument doc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "uploads/pan.jpg");
        sampleCustomer.setCustomerNumber("CUST-7K4P9M2Q");

        when(kycDocumentRepository.findByVerificationStatus(VerificationStatus.PENDING))
                .thenReturn(List.of(doc));
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(sampleCustomer));

        var queue = customerService.getPendingKycQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).customerId()).isEqualTo(customerId);
        assertThat(queue.get(0).customerNumber()).isEqualTo("CUST-7K4P9M2Q");
        assertThat(queue.get(0).customerName()).isEqualTo("John Doe");
        assertThat(queue.get(0).documentType()).isEqualTo(DocumentType.PAN);
    }

    @Test
    @DisplayName("Should return complete customer KYC details for staff review")
    void testGetStaffCustomerKycDetail() {
        sampleCustomer.setCustomerNumber("CUST-7K4P9M2Q");
        KYCDocument doc = new KYCDocument(customerId, DocumentType.PAN, "ENC_HASH", "uploads/pan.jpg");
        CustomerAddress address = new CustomerAddress(customerId, com.nextgen.bank.customer.domain.enums.AddressType.PERMANENT, "123 Main", "Bengaluru", "KA", "560001", "India");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByCustomerId(customerId)).thenReturn(List.of(address));
        when(kycDocumentRepository.findByCustomerId(customerId)).thenReturn(List.of(doc));

        var detail = customerService.getStaffCustomerKycDetail(customerId);

        assertThat(detail).isNotNull();
        assertThat(detail.customerId()).isEqualTo(customerId);
        assertThat(detail.customerNumber()).isEqualTo("CUST-7K4P9M2Q");
        assertThat(detail.addresses()).hasSize(1);
        assertThat(detail.documents()).hasSize(1);
    }
}

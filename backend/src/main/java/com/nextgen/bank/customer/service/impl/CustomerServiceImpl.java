package com.nextgen.bank.customer.service.impl;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.ResourceNotFoundException;
import com.nextgen.bank.customer.domain.Customer;
import com.nextgen.bank.customer.domain.CustomerAddress;
import com.nextgen.bank.customer.domain.KYCDocument;
import com.nextgen.bank.customer.domain.Nominee;
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
import com.nextgen.bank.customer.dto.PendingKycItemDto;
import com.nextgen.bank.customer.dto.StaffCustomerKycDetailDto;
import com.nextgen.bank.customer.dto.KYCDocumentResponseDto;
import com.nextgen.bank.customer.event.CustomerCreatedEvent;
import com.nextgen.bank.customer.event.KYCApprovedEvent;
import com.nextgen.bank.customer.event.KYCRejectedEvent;
import com.nextgen.bank.customer.event.KYCSubmittedEvent;
import com.nextgen.bank.customer.repository.CustomerAddressRepository;
import com.nextgen.bank.customer.repository.CustomerRepository;
import com.nextgen.bank.customer.repository.KYCDocumentRepository;
import com.nextgen.bank.customer.repository.NomineeRepository;
import com.nextgen.bank.customer.service.CustomerService;
import com.nextgen.bank.customer.service.KycDocumentStorageService;
import com.nextgen.bank.customer.util.CustomerNumberGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final KYCDocumentRepository kycDocumentRepository;
    private final NomineeRepository nomineeRepository;
    private final OutboxEventWriter outboxEventWriter;
    private final KycDocumentStorageService kycDocumentStorageService;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerAddressRepository customerAddressRepository,
            KYCDocumentRepository kycDocumentRepository,
            NomineeRepository nomineeRepository,
            OutboxEventWriter outboxEventWriter,
            KycDocumentStorageService kycDocumentStorageService
    ) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.nomineeRepository = nomineeRepository;
        this.outboxEventWriter = outboxEventWriter;
        this.kycDocumentStorageService = kycDocumentStorageService;
    }

    @Override
    public CustomerProfileResponseDto createCustomerProfile(CreateCustomerProfileRequestDto requestDto, UUID authenticatedUserId) {
        Objects.requireNonNull(authenticatedUserId, "Authenticated user ID cannot be null");
        Objects.requireNonNull(requestDto, "Create customer profile request cannot be null");

        String normalizedEmail = requestDto.email().trim().toLowerCase();
        String normalizedPhone = requestDto.phone().trim();

        // BR-CUST-001: Unique Identity Constraints
        if (customerRepository.existsByUserId(authenticatedUserId)) {
            throw new BusinessException(
                    "Customer profile already exists for this user account",
                    HttpStatus.CONFLICT,
                    "CUSTOMER_PROFILE_ALREADY_EXISTS"
            );
        }

        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(
                    "Email is already registered to another customer profile",
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS"
            );
        }

        if (customerRepository.existsByPhone(normalizedPhone)) {
            throw new BusinessException(
                    "Phone number is already registered to another customer profile",
                    HttpStatus.CONFLICT,
                    "PHONE_ALREADY_EXISTS"
            );
        }

        // BR-CUST-003: Age Restriction (>= 18 years old)
        if (requestDto.dateOfBirth() == null) {
            throw new BusinessException(
                    "Date of birth is required",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_OF_BIRTH"
            );
        }
        int age = Period.between(requestDto.dateOfBirth(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new BusinessException(
                    "Customer must be at least 18 years old to open an account (BR-CUST-003)",
                    HttpStatus.BAD_REQUEST,
                    "UNDERAGE_CUSTOMER"
            );
        }

        // BR-CUST-004: Nominee Allocation (if nominees present, sum must equal exactly 100.00%)
        if (requestDto.nominees() != null && !requestDto.nominees().isEmpty()) {
            BigDecimal totalAllocation = requestDto.nominees().stream()
                    .map(NomineeDto::allocationPercentage)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalAllocation.compareTo(new BigDecimal("100.00")) != 0) {
                throw new BusinessException(
                        "Total nominee allocation must equal exactly 100.00% (BR-CUST-004). Actual: " + totalAllocation + "%",
                        HttpStatus.BAD_REQUEST,
                        "INVALID_NOMINEE_ALLOCATION"
                );
            }
        }

        // Generate Unique Public Customer Number (CUST-XXXXXXXX)
        String customerNumber;
        int attempts = 0;
        do {
            if (attempts++ > 15) {
                throw new BusinessException(
                        "Unable to generate unique customer number",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "CUSTOMER_NUMBER_GENERATION_FAILED"
                );
            }
            customerNumber = CustomerNumberGenerator.generate();
        } while (customerRepository.existsByCustomerNumber(customerNumber));

        // Persist Customer Entity
        Customer customer = new Customer(
                authenticatedUserId,
                customerNumber,
                requestDto.firstName().trim(),
                requestDto.lastName().trim(),
                requestDto.dateOfBirth(),
                normalizedPhone,
                normalizedEmail,
                KYCStatus.PENDING,
                RiskCategory.MEDIUM
        );
        Customer savedCustomer = customerRepository.save(customer);

        // Persist Addresses
        List<CustomerAddressDto> savedAddresses = new ArrayList<>();
        if (requestDto.addresses() != null) {
            for (CustomerAddressDto addrDto : requestDto.addresses()) {
                CustomerAddress address = new CustomerAddress(
                        savedCustomer.getCustomerId(),
                        addrDto.addressType(),
                        addrDto.street().trim(),
                        addrDto.city().trim(),
                        addrDto.state().trim(),
                        addrDto.postalCode().trim(),
                        addrDto.country() != null ? addrDto.country().trim() : "India"
                );
                CustomerAddress savedAddr = customerAddressRepository.save(address);
                savedAddresses.add(CustomerAddressDto.fromEntity(savedAddr));
            }
        }

        // Persist Nominees
        List<NomineeDto> savedNominees = new ArrayList<>();
        if (requestDto.nominees() != null) {
            for (NomineeDto nomDto : requestDto.nominees()) {
                Nominee nominee = new Nominee(
                        savedCustomer.getCustomerId(),
                        nomDto.fullName().trim(),
                        nomDto.relationship().trim(),
                        nomDto.dateOfBirth(),
                        nomDto.phone().trim(),
                        nomDto.allocationPercentage()
                );
                Nominee savedNom = nomineeRepository.save(nominee);
                savedNominees.add(NomineeDto.fromEntity(savedNom));
            }
        }

        // Write transactional outbox event
        outboxEventWriter.write(new CustomerCreatedEvent(
                savedCustomer.getCustomerId(),
                savedCustomer.getUserId(),
                savedCustomer.getPhone(),
                savedCustomer.getEmail()
        ));

        return CustomerProfileResponseDto.fromEntity(savedCustomer, savedAddresses, savedNominees, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponseDto getCustomerProfile(UUID authenticatedUserId) {
        Customer customer = customerRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for user: " + authenticatedUserId
                ));

        List<CustomerAddressDto> addresses = customerAddressRepository.findByCustomerId(customer.getCustomerId())
                .stream()
                .map(CustomerAddressDto::fromEntity)
                .toList();

        List<NomineeDto> nominees = nomineeRepository.findByCustomerId(customer.getCustomerId())
                .stream()
                .map(NomineeDto::fromEntity)
                .toList();

        boolean hasSubmittedDocs = !kycDocumentRepository.findByCustomerId(customer.getCustomerId()).isEmpty();

        return CustomerProfileResponseDto.fromEntity(customer, addresses, nominees, hasSubmittedDocs);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponseDto getCustomerProfileById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found with ID: " + customerId
                ));

        List<CustomerAddressDto> addresses = customerAddressRepository.findByCustomerId(customer.getCustomerId())
                .stream()
                .map(CustomerAddressDto::fromEntity)
                .toList();

        List<NomineeDto> nominees = nomineeRepository.findByCustomerId(customer.getCustomerId())
                .stream()
                .map(NomineeDto::fromEntity)
                .toList();

        boolean hasSubmittedDocs = !kycDocumentRepository.findByCustomerId(customer.getCustomerId()).isEmpty();

        return CustomerProfileResponseDto.fromEntity(customer, addresses, nominees, hasSubmittedDocs);
    }

    @Override
    public KYCSubmissionResponseDto uploadKycDocument(
            DocumentType documentType,
            String documentNumber,
            MultipartFile file,
            UUID authenticatedUserId
    ) {
        Objects.requireNonNull(documentType, "Document type cannot be null");
        Objects.requireNonNull(documentNumber, "Document number cannot be null");
        Objects.requireNonNull(file, "Document file cannot be null");

        Customer customer = customerRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found. Please create your customer profile before submitting KYC documents."
                ));

        if (customer.getKycStatus() == KYCStatus.VERIFIED) {
            throw new BusinessException(
                    "Customer KYC is already verified. Additional document submissions are not allowed.",
                    HttpStatus.CONFLICT,
                    "KYC_ALREADY_VERIFIED"
            );
        }

        // Check if there is an active PENDING document under review
        List<KYCDocument> existingDocs = kycDocumentRepository.findByCustomerId(customer.getCustomerId());
        boolean hasPending = existingDocs.stream().anyMatch(d -> d.getVerificationStatus() == VerificationStatus.PENDING);
        if (hasPending) {
            throw new BusinessException(
                    "A KYC document is already submitted and pending compliance review. Please wait for staff review before submitting another document.",
                    HttpStatus.CONFLICT,
                    "KYC_SUBMISSION_PENDING_REVIEW"
            );
        }

        // Store file securely
        String fileReference = kycDocumentStorageService.storeDocument(file, customer.getCustomerId());

        String docNumberEnc = hashDocumentNumber(documentType, documentNumber.trim().toUpperCase());

        // Check duplicate document across platform
        if (kycDocumentRepository.existsByDocumentTypeAndDocumentNumberEnc(documentType, docNumberEnc)) {
            throw new BusinessException(
                    "This " + documentType + " document is already registered in the system",
                    HttpStatus.CONFLICT,
                    "DOCUMENT_ALREADY_EXISTS"
            );
        }

        KYCDocument document = new KYCDocument(
                customer.getCustomerId(),
                documentType,
                docNumberEnc,
                fileReference
        );
        KYCDocument savedDoc = kycDocumentRepository.save(document);

        customer.setKycStatus(KYCStatus.PENDING);
        customerRepository.save(customer);

        outboxEventWriter.write(new KYCSubmittedEvent(
                customer.getCustomerId(),
                savedDoc.getDocumentId(),
                savedDoc.getDocumentType()
        ));

        return new KYCSubmissionResponseDto(
                savedDoc.getDocumentId(),
                customer.getKycStatus(),
                Instant.now()
        );
    }

    @Override
    public KYCSubmissionResponseDto submitKyc(KYCSubmissionRequestDto requestDto, UUID authenticatedUserId) {
        Customer customer = customerRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found. Please create your customer profile before submitting KYC documents."
                ));

        if (customer.getKycStatus() == KYCStatus.VERIFIED) {
            throw new BusinessException(
                    "Customer KYC is already verified. Additional document submissions are not allowed.",
                    HttpStatus.CONFLICT,
                    "KYC_ALREADY_VERIFIED"
            );
        }

        List<KYCDocument> existingDocs = kycDocumentRepository.findByCustomerId(customer.getCustomerId());
        boolean hasPending = existingDocs.stream().anyMatch(d -> d.getVerificationStatus() == VerificationStatus.PENDING);
        if (hasPending) {
            throw new BusinessException(
                    "A KYC document is already submitted and pending compliance review. Please wait for staff review before submitting another document.",
                    HttpStatus.CONFLICT,
                    "KYC_SUBMISSION_PENDING_REVIEW"
            );
        }

        String docNumberEnc = hashDocumentNumber(requestDto.documentType(), requestDto.documentNumber().trim().toUpperCase());

        // Check if document already exists globally (e.g. same PAN used across multiple profiles)
        if (kycDocumentRepository.existsByDocumentTypeAndDocumentNumberEnc(requestDto.documentType(), docNumberEnc)) {
            throw new BusinessException(
                    "This " + requestDto.documentType() + " document is already registered in the system",
                    HttpStatus.CONFLICT,
                    "DOCUMENT_ALREADY_EXISTS"
            );
        }

        // Check if customer already has an APPROVED document of this type
        Optional<KYCDocument> existingDoc = kycDocumentRepository.findByCustomerIdAndDocumentType(
                customer.getCustomerId(),
                requestDto.documentType()
        );
        if (existingDoc.isPresent() && existingDoc.get().getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new BusinessException(
                    "A verified " + requestDto.documentType() + " document already exists for this customer",
                    HttpStatus.CONFLICT,
                    "KYC_ALREADY_VERIFIED"
            );
        }

        KYCDocument document = new KYCDocument(
                customer.getCustomerId(),
                requestDto.documentType(),
                docNumberEnc,
                requestDto.fileReference().trim()
        );
        KYCDocument savedDoc = kycDocumentRepository.save(document);

        if (customer.getKycStatus() != KYCStatus.VERIFIED) {
            customer.setKycStatus(KYCStatus.PENDING);
            customerRepository.save(customer);
        }

        outboxEventWriter.write(new KYCSubmittedEvent(
                customer.getCustomerId(),
                savedDoc.getDocumentId(),
                savedDoc.getDocumentType()
        ));

        return new KYCSubmissionResponseDto(
                savedDoc.getDocumentId(),
                customer.getKycStatus(),
                Instant.now()
        );
    }

    @Override
    public KYCVerificationResponseDto verifyKyc(KYCVerificationRequestDto requestDto, UUID staffUserId) {
        Customer customer = customerRepository.findById(requestDto.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found with ID: " + requestDto.customerId()
                ));

        List<KYCDocument> documents = kycDocumentRepository.findByCustomerId(customer.getCustomerId());
        if (documents.isEmpty()) {
            throw new BusinessException(
                    "No KYC documents found for customer ID: " + customer.getCustomerId(),
                    HttpStatus.BAD_REQUEST,
                    "NO_KYC_DOCUMENTS"
            );
        }

        if (requestDto.status() == KYCStatus.VERIFIED) {
            customer.verifyKyc();
            for (KYCDocument doc : documents) {
                if (doc.getVerificationStatus() == VerificationStatus.PENDING) {
                    doc.approve(staffUserId);
                    kycDocumentRepository.save(doc);
                }
            }
            customerRepository.save(customer);

            outboxEventWriter.write(new KYCApprovedEvent(
                    customer.getCustomerId(),
                    staffUserId,
                    requestDto.remarks() != null ? requestDto.remarks() : "KYC Approved"
            ));
        } else if (requestDto.status() == KYCStatus.REJECTED) {
            customer.rejectKyc();
            for (KYCDocument doc : documents) {
                if (doc.getVerificationStatus() == VerificationStatus.PENDING) {
                    doc.reject(staffUserId);
                    kycDocumentRepository.save(doc);
                }
            }
            customerRepository.save(customer);

            outboxEventWriter.write(new KYCRejectedEvent(
                    customer.getCustomerId(),
                    staffUserId,
                    requestDto.remarks() != null ? requestDto.remarks() : "Rejected during compliance review"
            ));
        } else {
            throw new BusinessException(
                    "Invalid verification status: " + requestDto.status() + ". Allowed: VERIFIED, REJECTED",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_STATUS"
            );
        }

        return new KYCVerificationResponseDto(
                customer.getCustomerId(),
                customer.getKycStatus(),
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public KYCStatus getKYCStatus(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(Customer::getKycStatus)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found with ID: " + customerId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCustomerKycVerified(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(Customer::isKycVerified)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingKycItemDto> getPendingKycQueue() {
        List<KYCDocument> pendingDocs = kycDocumentRepository.findByVerificationStatus(VerificationStatus.PENDING);
        if (pendingDocs.isEmpty()) {
            return Collections.emptyList();
        }

        List<PendingKycItemDto> queue = new ArrayList<>();
        for (KYCDocument doc : pendingDocs) {
            customerRepository.findById(doc.getCustomerId()).ifPresent(customer -> {
                queue.add(new PendingKycItemDto(
                        customer.getCustomerId(),
                        customer.getCustomerNumber(),
                        customer.getFirstName() + " " + customer.getLastName(),
                        customer.getEmail(),
                        customer.getPhone(),
                        customer.getKycStatus(),
                        doc.getDocumentId(),
                        doc.getDocumentType(),
                        doc.getFileReference(),
                        customer.getCreatedAt()
                ));
            });
        }
        return queue;
    }

    @Override
    @Transactional(readOnly = true)
    public StaffCustomerKycDetailDto getStaffCustomerKycDetail(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found with ID: " + customerId));

        List<CustomerAddressDto> addresses = customerAddressRepository.findByCustomerId(customerId)
                .stream()
                .map(CustomerAddressDto::fromEntity)
                .toList();

        List<KYCDocumentResponseDto> documents = kycDocumentRepository.findByCustomerId(customerId)
                .stream()
                .map(KYCDocumentResponseDto::fromEntity)
                .toList();

        return StaffCustomerKycDetailDto.fromEntities(customer, addresses, documents);
    }

    private String hashDocumentNumber(DocumentType documentType, String documentNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documentNumber.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("ENC_");
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

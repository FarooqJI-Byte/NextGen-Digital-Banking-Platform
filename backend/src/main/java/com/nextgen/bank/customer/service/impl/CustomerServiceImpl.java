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
import com.nextgen.bank.customer.event.CustomerCreatedEvent;
import com.nextgen.bank.customer.event.KYCApprovedEvent;
import com.nextgen.bank.customer.event.KYCRejectedEvent;
import com.nextgen.bank.customer.event.KYCSubmittedEvent;
import com.nextgen.bank.customer.repository.CustomerAddressRepository;
import com.nextgen.bank.customer.repository.CustomerRepository;
import com.nextgen.bank.customer.repository.KYCDocumentRepository;
import com.nextgen.bank.customer.repository.NomineeRepository;
import com.nextgen.bank.customer.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
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

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerAddressRepository customerAddressRepository,
            KYCDocumentRepository kycDocumentRepository,
            NomineeRepository nomineeRepository,
            OutboxEventWriter outboxEventWriter
    ) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.nomineeRepository = nomineeRepository;
        this.outboxEventWriter = outboxEventWriter;
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

        // BR-CUST-003: Age Restriction (Must be >= 18)
        int age = Period.between(requestDto.dateOfBirth(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new BusinessException(
                    "Customer must be at least 18 years old to create a profile and open an account (BR-CUST-003)",
                    HttpStatus.BAD_REQUEST,
                    "UNDERAGE_CUSTOMER"
            );
        }

        // BR-CUST-004: Nominee Allocation (Must equal exactly 100.00% if nominees provided)
        if (requestDto.nominees() != null && !requestDto.nominees().isEmpty()) {
            BigDecimal totalAllocation = requestDto.nominees().stream()
                    .map(NomineeDto::allocationPercentage)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalAllocation.compareTo(new BigDecimal("100.00")) != 0) {
                throw new BusinessException(
                        "Total nominee allocation percentage must equal exactly 100.00% (BR-CUST-004). Current total: " + totalAllocation,
                        HttpStatus.BAD_REQUEST,
                        "INVALID_NOMINEE_ALLOCATION"
                );
            }
        }

        // Create and persist Customer
        Customer customer = new Customer(
                authenticatedUserId,
                requestDto.firstName().trim(),
                requestDto.lastName().trim(),
                requestDto.dateOfBirth(),
                normalizedPhone,
                normalizedEmail,
                KYCStatus.PENDING,
                RiskCategory.MEDIUM
        );
        Customer savedCustomer = customerRepository.save(customer);

        // Persist addresses
        List<CustomerAddressDto> savedAddresses = new ArrayList<>();
        if (requestDto.addresses() != null) {
            for (CustomerAddressDto addrDto : requestDto.addresses()) {
                CustomerAddress addr = new CustomerAddress(
                        savedCustomer.getCustomerId(),
                        addrDto.addressType(),
                        addrDto.street().trim(),
                        addrDto.city().trim(),
                        addrDto.state().trim(),
                        addrDto.postalCode().trim(),
                        addrDto.country() != null ? addrDto.country().trim() : "India"
                );
                CustomerAddress savedAddr = customerAddressRepository.save(addr);
                savedAddresses.add(CustomerAddressDto.fromEntity(savedAddr));
            }
        }

        // Persist nominees
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

        // Transactional Outbox Event
        outboxEventWriter.write(new CustomerCreatedEvent(
                savedCustomer.getCustomerId(),
                savedCustomer.getUserId(),
                savedCustomer.getPhone(),
                savedCustomer.getEmail()
        ));

        return CustomerProfileResponseDto.fromEntity(savedCustomer, savedAddresses, savedNominees);
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

        return CustomerProfileResponseDto.fromEntity(customer, addresses, nominees);
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

        return CustomerProfileResponseDto.fromEntity(customer, addresses, nominees);
    }

    @Override
    public KYCSubmissionResponseDto submitKyc(KYCSubmissionRequestDto requestDto, UUID authenticatedUserId) {
        Customer customer = customerRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found. Please create your customer profile before submitting KYC documents."
                ));

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

        List<KYCDocument> docs = kycDocumentRepository.findByCustomerId(customer.getCustomerId());
        if (docs.isEmpty()) {
            throw new BusinessException(
                    "No KYC documents found for customer ID: " + requestDto.customerId(),
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_KYC_DOCUMENTS"
            );
        }

        if (requestDto.status() == KYCStatus.VERIFIED) {
            for (KYCDocument doc : docs) {
                if (doc.getVerificationStatus() == VerificationStatus.PENDING) {
                    doc.approve(staffUserId);
                    kycDocumentRepository.save(doc);
                }
            }
            customer.verifyKyc();
            customerRepository.save(customer);

            outboxEventWriter.write(new KYCApprovedEvent(
                    customer.getCustomerId(),
                    staffUserId,
                    requestDto.remarks() != null ? requestDto.remarks() : "KYC documents approved"
            ));
        } else if (requestDto.status() == KYCStatus.REJECTED) {
            for (KYCDocument doc : docs) {
                if (doc.getVerificationStatus() == VerificationStatus.PENDING) {
                    doc.reject(staffUserId);
                    kycDocumentRepository.save(doc);
                }
            }
            customer.rejectKyc();
            customerRepository.save(customer);

            outboxEventWriter.write(new KYCRejectedEvent(
                    customer.getCustomerId(),
                    staffUserId,
                    requestDto.remarks() != null ? requestDto.remarks() : "KYC documents rejected"
            ));
        } else {
            throw new BusinessException(
                    "Invalid KYC verification status: " + requestDto.status() + ". Allowed: VERIFIED or REJECTED",
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
                        "Customer not found with ID: " + customerId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCustomerKycVerified(UUID customerId) {
        return customerRepository.findById(customerId)
                .map(Customer::isKycVerified)
                .orElse(false);
    }

    private String hashDocumentNumber(DocumentType type, String docNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((type.name() + ":" + docNumber).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("ENC_");
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm unavailable", e);
        }
    }
}

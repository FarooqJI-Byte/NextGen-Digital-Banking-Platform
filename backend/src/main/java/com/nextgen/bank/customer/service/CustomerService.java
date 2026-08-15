package com.nextgen.bank.customer.service;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.dto.CreateCustomerProfileRequestDto;
import com.nextgen.bank.customer.dto.CustomerProfileResponseDto;
import com.nextgen.bank.customer.dto.KYCSubmissionRequestDto;
import com.nextgen.bank.customer.dto.KYCSubmissionResponseDto;
import com.nextgen.bank.customer.dto.KYCVerificationRequestDto;
import com.nextgen.bank.customer.dto.KYCVerificationResponseDto;
import com.nextgen.bank.customer.dto.PendingKycItemDto;
import com.nextgen.bank.customer.dto.StaffCustomerKycDetailDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerProfileResponseDto createCustomerProfile(CreateCustomerProfileRequestDto requestDto, UUID authenticatedUserId);

    CustomerProfileResponseDto getCustomerProfile(UUID authenticatedUserId);

    CustomerProfileResponseDto getCustomerProfileById(UUID customerId);

    KYCSubmissionResponseDto uploadKycDocument(DocumentType documentType, String documentNumber, MultipartFile file, UUID authenticatedUserId);

    KYCSubmissionResponseDto submitKyc(KYCSubmissionRequestDto requestDto, UUID authenticatedUserId);

    KYCVerificationResponseDto verifyKyc(KYCVerificationRequestDto requestDto, UUID staffUserId);

    List<PendingKycItemDto> getPendingKycQueue();

    StaffCustomerKycDetailDto getStaffCustomerKycDetail(UUID customerId);

    KYCStatus getKYCStatus(UUID customerId);

    boolean isCustomerKycVerified(UUID customerId);
}

package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.dto.OtpVerificationResponseDto;

public interface OtpService {

    void generateAndSendOtp(String identifier, String purpose);

    OtpVerificationResponseDto verifyOtp(String identifier, String purpose, String otp);

    void resendOtp(String identifier, String purpose);
}

package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.dto.LoginRequestDto;
import com.nextgen.bank.auth.dto.LoginResponseDto;
import com.nextgen.bank.auth.dto.RegisterRequestDto;
import com.nextgen.bank.auth.dto.RegisterResponseDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto requestDto);

    LoginResponseDto login(LoginRequestDto requestDto, String ipAddress, String userAgent);
}

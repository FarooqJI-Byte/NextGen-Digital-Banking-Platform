package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.dto.CreateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffUserResponseDto;

import java.util.List;
import java.util.UUID;

public interface AdminStaffService {

    StaffUserResponseDto createStaff(CreateStaffRequestDto requestDto, UUID adminUserId);

    StaffUserResponseDto resendStaffActivation(UUID staffUserId, UUID adminUserId);

    List<StaffUserResponseDto> listStaff();
}

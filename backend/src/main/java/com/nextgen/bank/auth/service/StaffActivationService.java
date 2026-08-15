package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.dto.ActivateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffActivationResponseDto;

public interface StaffActivationService {

    StaffActivationResponseDto activateStaff(ActivateStaffRequestDto requestDto);
}

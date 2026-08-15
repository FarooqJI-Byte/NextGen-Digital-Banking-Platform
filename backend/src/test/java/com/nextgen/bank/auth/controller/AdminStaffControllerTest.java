package com.nextgen.bank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.bank.auth.dto.CreateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffUserResponseDto;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.auth.service.AdminStaffService;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminStaffControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminStaffService adminStaffService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminStaffController adminStaffController;

    private final UUID adminUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminStaffController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/admin/staff should return 201 Created on valid request")
    void testCreateStaff_Success() throws Exception {
        CreateStaffRequestDto request = new CreateStaffRequestDto(
                "sarah_staff",
                "sarah.connor@nextgenbank.com"
        );

        UUID staffId = UUID.randomUUID();
        StaffUserResponseDto response = new StaffUserResponseDto(
                staffId,
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                UserRole.BANK_STAFF,
                false,
                Instant.now()
        );

        when(adminStaffService.createStaff(any(CreateStaffRequestDto.class), eq(adminUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/staff")
                        .requestAttr("authenticatedUserId", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(staffId.toString()))
                .andExpect(jsonPath("$.username").value("sarah_staff"))
                .andExpect(jsonPath("$.email").value("sarah.connor@nextgenbank.com"))
                .andExpect(jsonPath("$.role").value("BANK_STAFF"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/admin/staff/{userId}/resend-activation should return 200 OK")
    void testResendStaffActivation_Success() throws Exception {
        UUID staffId = UUID.randomUUID();
        StaffUserResponseDto response = new StaffUserResponseDto(
                staffId,
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                UserRole.BANK_STAFF,
                false,
                Instant.now()
        );

        when(adminStaffService.resendStaffActivation(eq(staffId), eq(adminUserId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/staff/" + staffId + "/resend-activation")
                        .requestAttr("authenticatedUserId", adminUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(staffId.toString()))
                .andExpect(jsonPath("$.username").value("sarah_staff"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/staff should return 400 Bad Request on blank username")
    void testCreateStaff_BlankUsername_Fails() throws Exception {
        CreateStaffRequestDto request = new CreateStaffRequestDto(
                "",
                "sarah.connor@nextgenbank.com"
        );

        mockMvc.perform(post("/api/v1/admin/staff")
                        .requestAttr("authenticatedUserId", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/admin/staff should return 400 Bad Request on invalid email")
    void testCreateStaff_InvalidEmail_Fails() throws Exception {
        CreateStaffRequestDto request = new CreateStaffRequestDto(
                "sarah_staff",
                "not-an-email"
        );

        mockMvc.perform(post("/api/v1/admin/staff")
                        .requestAttr("authenticatedUserId", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/admin/staff should return 200 OK with staff list")
    void testListStaff_Success() throws Exception {
        StaffUserResponseDto staff = new StaffUserResponseDto(
                UUID.randomUUID(),
                "sarah_staff",
                "sarah@bank.com",
                UserRole.BANK_STAFF,
                true,
                Instant.now()
        );

        when(adminStaffService.listStaff()).thenReturn(List.of(staff));

        mockMvc.perform(get("/api/v1/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("sarah_staff"))
                .andExpect(jsonPath("$[0].role").value("BANK_STAFF"));
    }
}

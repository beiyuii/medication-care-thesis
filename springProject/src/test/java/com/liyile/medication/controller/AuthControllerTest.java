package com.liyile.medication.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.entity.User;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.service.PatientService;
import com.liyile.medication.service.UserService;
import com.liyile.medication.vo.AuthProfileVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthControllerTest {

  @Test
  @DisplayName("profile 在患者列表查询失败时仍应返回基础认证信息")
  void shouldReturnBaseProfileWhenPatientLookupFails() {
    UserService userService = Mockito.mock(UserService.class);
    PatientService patientService = Mockito.mock(PatientService.class);
    PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);

    AuthController controller =
        new AuthController(userService, patientService, passwordEncoder, jwtTokenProvider);

    User user = new User();
    user.setId(7L);
    user.setUsername("care1");
    user.setRole("caregiver");

    when(jwtTokenProvider.getUsername("valid-token")).thenReturn("care1");
    when(jwtTokenProvider.getRole("valid-token")).thenReturn("caregiver");
    when(userService.findByUsername("care1")).thenReturn(user);
    when(patientService.findPatientsByUserId(7L, "caregiver"))
        .thenThrow(new RuntimeException("Table 'lyl.user_patient_relation' doesn't exist"));

    ApiResponse<AuthProfileVO> response = controller.profile("Bearer valid-token");

    assertEquals(200, response.getCode());
    assertNotNull(response.getData());
    assertEquals(7L, response.getData().getUserId());
    assertEquals("care1", response.getData().getUsername());
    assertEquals("caregiver", response.getData().getRole());
    assertNotNull(response.getData().getPatients());
    assertTrue(response.getData().getPatients().isEmpty());
  }
}

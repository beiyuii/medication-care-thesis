package com.liyile.medication.service;

import com.liyile.medication.common.CommonConstants;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.entity.User;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.vo.PatientSummaryVO;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 用户访问范围服务。
 *
 * <p>统一解析 Bearer Token 并校验用户是否有权限访问指定患者。</p>
 */
@Service
public class UserAccessService {
  private final JwtTokenProvider jwtTokenProvider;
  private final UserService userService;
  private final PatientService patientService;

  public UserAccessService(
      JwtTokenProvider jwtTokenProvider, UserService userService, PatientService patientService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userService = userService;
    this.patientService = patientService;
  }

  public ResolvedAuth resolve(String authorization) {
    String token = authorization != null && authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);
    User user = userService.findByUsername(username);
    if (user == null) {
      throw new IllegalArgumentException("未认证或令牌无效");
    }
    return new ResolvedAuth(user, role);
  }

  public List<PatientSummaryVO> getAccessiblePatients(String authorization) {
    ResolvedAuth auth = resolve(authorization);
    return patientService.findPatientsByUserId(auth.user().getId(), auth.role());
  }

  public void assertCanAccessPatient(String authorization, Long patientId) {
    boolean matched = getAccessiblePatients(authorization).stream()
        .anyMatch(patient -> patientId != null && patientId.equals(patient.getId()));
    if (!matched) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "当前账号无权访问该患者");
    }
  }

  public record ResolvedAuth(User user, String role) {}
}

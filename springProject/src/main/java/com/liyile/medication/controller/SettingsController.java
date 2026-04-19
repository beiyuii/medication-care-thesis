package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.CommonConstants;
import com.liyile.medication.common.ErrorCode;
import com.liyile.medication.entity.Settings;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.service.SettingsService;
import com.liyile.medication.service.UserService;
import com.liyile.medication.vo.SettingsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设置管理控制器。
 * <p>提供用户设置的读取与更新功能，仅老年人角色可写，其他角色只读。</p>
 *
 * @author Liyile
 */
@Tag(name = "设置管理", description = "用户设置的读取与更新接口")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
  /** 设置服务 */
  private final SettingsService settingsService;
  /** 用户服务 */
  private final UserService userService;
  /** JWT工具 */
  private final JwtTokenProvider jwtTokenProvider;

  /** 构造方法注入依赖。 */
  public SettingsController(
      SettingsService settingsService,
      UserService userService,
      JwtTokenProvider jwtTokenProvider) {
    this.settingsService = settingsService;
    this.userService = userService;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * 获取当前用户设置。
   * <p>返回当前登录用户的系统设置，如果不存在则返回默认值。</p>
   *
   * @param authorization Authorization头部（Bearer token）
   * @return 设置VO对象
   */
  @Operation(
      summary = "获取当前用户设置",
      description = "返回当前登录用户的系统设置，如果不存在则返回默认值")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回设置对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @GetMapping
  public ApiResponse<SettingsVO> get(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true)
      @RequestHeader("Authorization") String authorization) {
    String token = authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    com.liyile.medication.entity.User user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.failure(ErrorCode.UNAUTHORIZED, "用户不存在");
    }
    
    SettingsVO settings = settingsService.getSettings(user.getId());
    return ApiResponse.success(settings);
  }

  /**
   * 更新用户设置。
   * <p>更新当前用户的系统设置，仅老年人角色可写，其他角色返回403错误。</p>
   *
   * @param authorization Authorization头部（Bearer token）
   * @param settingsVO 设置VO对象（包含需要更新的字段）
   * @return 更新后的完整设置VO对象
   */
  @Operation(
      summary = "更新用户设置",
      description = "更新当前用户的系统设置，仅老年人角色可写，其他角色返回403错误")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功，返回更新后的设置对象")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足，仅老年人可更新设置")
  @PreAuthorize("hasRole('ELDER')")
  @PutMapping
  public ApiResponse<SettingsVO> update(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true)
      @RequestHeader("Authorization") String authorization,
      @Parameter(description = "设置对象，包含需要更新的字段（reminder/detection/privacy）")
      @RequestBody SettingsVO settingsVO) {
    String token = authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);
    
    // 权限校验：仅elder可写
    if (!"elder".equals(role)) {
      return ApiResponse.failure(ErrorCode.FORBIDDEN, "仅老年人可更新设置");
    }
    
    com.liyile.medication.entity.User user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.failure(ErrorCode.UNAUTHORIZED, "用户不存在");
    }
    
    // 将VO转换为实体
    Settings settings = new Settings();
    if (settingsVO.getReminder() != null) {
      settings.setReminderEnableVoice(settingsVO.getReminder().getEnableVoice());
      settings.setReminderAdvanceMinutes(settingsVO.getReminder().getAdvanceMinutes());
      settings.setReminderVolume(settingsVO.getReminder().getVolume());
    }
    if (settingsVO.getDetection() != null) {
      settings.setDetectionAutoStart(settingsVO.getDetection().getAutoStart());
      settings.setDetectionLowLightEnhance(settingsVO.getDetection().getLowLightEnhance());
      settings.setDetectionFallbackMode(settingsVO.getDetection().getFallbackMode());
    }
    if (settingsVO.getPrivacy() != null) {
      settings.setPrivacyCameraPermission(settingsVO.getPrivacy().getCameraPermission());
      settings.setPrivacyUploadConsent(settingsVO.getPrivacy().getUploadConsent());
      settings.setPrivacyShareToCaregiver(settingsVO.getPrivacy().getShareToCaregiver());
    }
    
    SettingsVO updated = settingsService.updateSettings(user.getId(), settings);
    return ApiResponse.success(updated);
  }
}


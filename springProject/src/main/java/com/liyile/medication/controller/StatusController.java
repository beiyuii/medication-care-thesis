package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.vo.RolePermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 状态检查控制器。
 * <p>提供系统状态检查和角色权限矩阵查询接口。</p>
 *
 * @author Liyile
 */
@Tag(name = "状态检查", description = "系统状态检查和角色权限矩阵接口")
@RestController
@RequestMapping("/api/status")
public class StatusController {
  /** JWT工具 */
  private final JwtTokenProvider jwtTokenProvider;

  /** 应用版本号 */
  @Value("${app.version:1.0.0}")
  private String version;

  /** 构造方法注入依赖。 */
  public StatusController(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * 联通性测试接口。
   * <p>返回系统健康状态和版本信息，用于监控和心跳检测。该接口无需鉴权。</p>
   *
   * @return 包含timestamp和version的响应
   */
  @Operation(summary = "联通性测试", description = "返回系统健康状态和版本信息，用于监控和心跳检测。该接口无需鉴权")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "系统正常")
  @GetMapping("/ping")
  public ApiResponse<Map<String, String>> ping() {
    Map<String, String> result = new HashMap<>();
    result.put("timestamp", Instant.now().toString());
    result.put("version", version);
    return ApiResponse.success(result);
  }

  /**
   * 获取角色权限矩阵。
   * <p>返回当前登录用户的角色权限矩阵，用于前端缓存和权限控制。</p>
   *
   * @param authorization Authorization头部（Bearer token）
   * @return 角色权限矩阵VO
   */
  @Operation(summary = "获取角色权限矩阵", description = "返回当前登录用户的角色权限矩阵，用于前端缓存和权限控制")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回权限矩阵")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @GetMapping("/role")
  public ApiResponse<RolePermissionVO> role(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true)
      @RequestHeader("Authorization") String authorization) {
    String token = authorization.startsWith("Bearer ") 
        ? authorization.substring(7) 
        : authorization;
    String role = jwtTokenProvider.getRole(token);
    
    Map<String, List<String>> permissions = new HashMap<>();
    
    if ("elder".equals(role)) {
      permissions.put("schedules", List.of("read", "write"));
      permissions.put("intakeEvents", List.of("read", "write"));
      permissions.put("alerts", List.of("read", "write"));
      permissions.put("settings", List.of("read", "write"));
      permissions.put("patients", List.of("read"));
    } else if ("caregiver".equals(role) || "child".equals(role)) {
      permissions.put("schedules", List.of("read"));
      permissions.put("intakeEvents", List.of("read"));
      permissions.put("alerts", List.of("read"));
      permissions.put("settings", List.of("read"));
      permissions.put("patients", List.of("read"));
    }
    
    RolePermissionVO vo = new RolePermissionVO(role, permissions);
    return ApiResponse.success(vo);
  }
}


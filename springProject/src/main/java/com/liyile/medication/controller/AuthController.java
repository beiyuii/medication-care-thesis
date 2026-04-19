package com.liyile.medication.controller;

import com.liyile.medication.common.CommonConstants;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.dto.LoginRequest;
import com.liyile.medication.dto.RegisterRequest;
import com.liyile.medication.entity.User;
import com.liyile.medication.security.JwtTokenProvider;
import com.liyile.medication.service.PatientService;
import com.liyile.medication.service.UserService;
import com.liyile.medication.vo.AuthProfileVO;
import com.liyile.medication.vo.PatientSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 认证控制器。
 * <p>本项目仅在本机运行，注册与登录接口均服务于本地开发调试。</p>
 *
 * @author Liyile
 */
@Tag(name = "用户认证管理", description = "用户注册、登录、个人信息查询等认证相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  /** 日志记录器 */
  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
  
  /** 用户服务 */
  private final UserService userService;
  /** 患者服务 */
  private final PatientService patientService;
  /** 密码编码器 */
  private final PasswordEncoder passwordEncoder;
  /** JWT 工具 */
  private final JwtTokenProvider jwtTokenProvider;

  @Value("${app.auth.allow-plain:false}")
  private boolean allowPlain;

  /** 构造方法注入依赖。 */
  public AuthController(
      UserService userService,
      PatientService patientService,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider) {
    this.userService = userService;
    this.patientService = patientService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * 注册接口。
   *
   * @param request 注册请求参数，包含用户名、密码、角色等信息
   * @return 注册成功后返回 token 与角色
   */
  @Operation(
      summary = "用户注册",
      description = "创建新用户账号，支持elder（老年人）、caregiver（护工）、child（子女）三种角色。注册成功后自动返回JWT token")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功，返回token和角色")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败或用户名已存在")
  @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<Map<String, String>> register(
      @Parameter(description = "注册请求参数，包含username（用户名）、password（密码）、role（角色：elder/caregiver/child）")
      @Valid @RequestBody RegisterRequest request) {
    logger.info("收到注册请求: username={}, role={}", request.getUsername(), request.getRole());
    
    User exists = userService.findByUsername(request.getUsername());
    if (exists != null) {
      logger.warn("注册失败: 用户名已存在, username={}", request.getUsername());
      return ApiResponse.failure(com.liyile.medication.common.ErrorCode.UNPROCESSABLE, "用户名已存在");
    }
    
    User user = new User();
    user.setUsername(request.getUsername());
    user.setPwdHash(passwordEncoder.encode(request.getPassword()));
    String normalizedRole = request.getRole().toLowerCase(Locale.ROOT);
    user.setRole(normalizedRole);
    userService.save(user);
    
    String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
    logger.info("注册成功: username={}, role={}, userId={}, token={}", 
        user.getUsername(), user.getRole(), user.getId(), token);
    
    Map<String, String> result = Map.of("token", token, "role", user.getRole());
    return ApiResponse.success(result);
  }

  /**
   * 登录接口。
   *
   * @param request 登录请求参数，包含用户名和密码
   * @return token 与角色信息
   */
  @Operation(
      summary = "用户登录",
      description = "使用用户名和密码登录系统，支持BCrypt加密密码和明文密码（开发模式）。登录成功返回JWT token和用户角色")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功，返回token和角色")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户名或密码错误")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "参数校验失败")
  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<Map<String, Object>> login(
      @Parameter(description = "登录请求参数，包含username（用户名）、password（密码）")
      @Valid @RequestBody LoginRequest request) {
    logger.info("收到登录请求: username={}", request.getUsername());
    
    User user = userService.findByUsername(request.getUsername());
    if (user == null) {
      logger.warn("登录失败: 用户不存在, username={}", request.getUsername());
      return ApiResponse.failure(com.liyile.medication.common.ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }
    
    String hash = user.getPwdHash();
    boolean match = isBcryptHash(hash)
        ? passwordEncoder.matches(request.getPassword(), hash)
        : (allowPlain && Objects.equals(request.getPassword(), hash));
    if (!match) {
      logger.warn("登录失败: 密码错误, username={}", request.getUsername());
      return ApiResponse.failure(com.liyile.medication.common.ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }
    
    String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
    logger.info("登录成功: username={}, role={}, userId={}, token={}", 
        user.getUsername(), user.getRole(), user.getId(), token);
    
    Map<String, Object> result = new HashMap<>();
    result.put("token", token);
    result.put("role", user.getRole());
    result.put("userId", user.getId());
    result.put("displayName", user.getUsername());
    return ApiResponse.success(result);
  }

  /**
   * 个人信息查询接口。
   *
   * @param authorization Authorization 头部（Bearer token）
   * @return 用户名与角色信息
   */
  @Operation(
      summary = "查询当前用户信息",
      description = "根据JWT token查询当前登录用户的个人信息，包括用户名和角色。需要在请求头中携带有效的JWT token")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功，返回用户名和角色")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或token无效")
  @GetMapping("/profile")
  public ApiResponse<AuthProfileVO> profile(
      @Parameter(name = "Authorization", description = "JWT token，格式为\"Bearer {token}\"", required = true, example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
      @RequestHeader("Authorization") String authorization) {
    String token = authorization != null && authorization.startsWith(CommonConstants.BEARER_PREFIX)
        ? authorization.substring(CommonConstants.BEARER_PREFIX.length())
        : authorization;
    String username = jwtTokenProvider.getUsername(token);
    String role = jwtTokenProvider.getRole(token);
    User user = userService.findByUsername(username);
    if (user == null) {
      logger.warn("profile 查询失败：token 用户不存在, username={}", username);
      return ApiResponse.failure(com.liyile.medication.common.ErrorCode.UNAUTHORIZED, "未认证或令牌无效");
    }
    
    List<PatientSummaryVO> patients = Collections.emptyList();
    try {
      patients = patientService.findPatientsByUserId(user.getId(), role);
    } catch (RuntimeException ex) {
      logger.warn("profile 查询患者列表失败，已降级为空列表: userId={}, role={}, reason={}",
          user.getId(), role, ex.getMessage());
    }
    
    AuthProfileVO profile = new AuthProfileVO(user.getId(), username, role, username, patients);
    return ApiResponse.success(profile);
  }

  /**
   * 判断是否为 BCrypt 哈希。
   *
   * @param hash 密码哈希
   * @return 是否为 BCrypt 哈希
   */
  private boolean isBcryptHash(String hash) {
    return hash != null
        && (hash.startsWith(CommonConstants.BCRYPT_PREFIX_2A)
            || hash.startsWith(CommonConstants.BCRYPT_PREFIX_2B)
            || hash.startsWith(CommonConstants.BCRYPT_PREFIX_2Y));
  }
}

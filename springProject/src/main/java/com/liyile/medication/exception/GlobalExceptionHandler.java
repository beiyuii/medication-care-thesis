package com.liyile.medication.exception;

import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>统一拦截并处理控制层抛出的异常，规范化返回结构。</p>
 *
 * @author Liyile
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  /** 日志记录器 */
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 处理参数校验异常（@Valid）。
   *
   * @param ex 方法参数校验异常
   * @return 统一失败响应，包含字段错误详情
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    LOGGER.warn("参数校验失败: {}", errors);
    ApiResponse<Map<String, String>> response =
        ApiResponse.failure(ErrorCode.UNPROCESSABLE, "请求参数不合法");
    response.setData(errors);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
  }

  /**
   * 处理约束违反异常（如 @NotBlank 在非 Bean 校验场景）。
   *
   * @param ex 约束违反异常
   * @return 统一失败响应
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
    LOGGER.warn("约束校验失败: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ApiResponse.failure(ErrorCode.UNPROCESSABLE, "请求参数不合法"));
  }

  /**
   * 处理 JWT 密钥过弱异常（服务器配置问题）。
   *
   * @param ex 弱密钥异常
   * @return 服务器错误响应，提示联系管理员
   */
  @ExceptionHandler(WeakKeyException.class)
  public ResponseEntity<ApiResponse<Void>> handleWeakKey(WeakKeyException ex) {
    LOGGER.error("JWT 密钥过弱导致 token 生成失败，请检查配置: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.failure(ErrorCode.SERVER_ERROR, "服务器配置错误：JWT 密钥过弱，请联系管理员"));
  }

  /**
   * 处理 JWT 解析相关异常（无效、过期、签名错误等）。
   *
   * @param ex JWT 解析异常
   * @return 未认证响应
   */
  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleJwt(JwtException ex) {
    LOGGER.warn("JWT 处理异常: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED, "未认证或令牌无效"));
  }

  /**
   * 处理非法参数异常。
   *
   * @param ex 非法参数异常
   * @return 参数错误响应
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    LOGGER.warn("非法参数: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ApiResponse.failure(ErrorCode.UNPROCESSABLE, "请求参数不合法"));
  }

  /**
   * 处理权限拒绝异常。
   *
   * @param ex 权限拒绝异常
   * @return 无权限响应
   */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
    LOGGER.warn("权限拒绝: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.failure(ErrorCode.FORBIDDEN, "无权限访问"));
  }

  /**
   * 处理所有未捕获的通用异常。
   *
   * @param ex 通用异常
   * @param request HTTP请求对象
   * @return 服务器错误响应
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(
      Exception ex, HttpServletRequest request) {
    LOGGER.error("服务器内部异常", ex);
    
    // 构建错误响应（符合前端期望格式）
    Map<String, Object> error = new HashMap<>();
    error.put("error", ex.getClass().getSimpleName());
    error.put("detail", ex.getMessage() != null ? ex.getMessage() : "服务器内部错误");
    error.put("traceId", MDC.get("traceId"));
    error.put("timestamp", Instant.now().toString());
    error.put("path", request.getRequestURI());
    
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}

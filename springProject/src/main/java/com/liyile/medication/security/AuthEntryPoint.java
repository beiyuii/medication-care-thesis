package com.liyile.medication.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 未认证入口处理器。
 * <p>当请求未认证时返回统一错误响应。</p>
 *
 * @author Liyile
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    ApiResponse<Void> body = ApiResponse.failure(ErrorCode.UNAUTHORIZED, "未认证或令牌无效");
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
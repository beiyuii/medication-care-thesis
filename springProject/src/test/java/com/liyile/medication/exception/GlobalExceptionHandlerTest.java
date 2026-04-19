package com.liyile.medication.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.AuthorizationDeniedException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("JWT 异常应返回 401")
  void shouldReturnUnauthorizedStatusForJwtException() {
    ResponseEntity<?> response = handler.handleJwt(new JwtException("bad token"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  @DisplayName("权限异常应返回 403")
  void shouldReturnForbiddenStatusForAuthorizationDenied() {
    ResponseEntity<?> response =
        handler.handleAuthorizationDenied(
            new AuthorizationDeniedException(
                "forbidden", Mockito.mock(AuthorizationResult.class)));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
  }

  @Test
  @DisplayName("通用异常应返回 500 与错误详情")
  void shouldReturnServerErrorStatusForUnhandledException() {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/api/schedules");

    ResponseEntity<Map<String, Object>> response =
        handler.handleException(new RuntimeException("boom"), request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("boom", response.getBody().get("detail"));
    assertEquals("/api/schedules", response.getBody().get("path"));
  }
}

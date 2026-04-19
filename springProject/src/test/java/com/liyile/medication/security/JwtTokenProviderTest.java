package com.liyile.medication.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

  @Test
  @DisplayName("开发默认 JWT 密钥应能稳定生成并校验 token")
  void shouldGenerateTokenWithDevelopmentDefaultSecret() {
    JwtTokenProvider provider = new JwtTokenProvider();
    ReflectionTestUtils.setField(provider, "jwtSecret", "change-this-in-production");
    ReflectionTestUtils.setField(provider, "expirationDays", 1);

    String token =
        assertDoesNotThrow(() -> provider.generateToken("elder1", "elder"));

    assertTrue(provider.validateToken(token));
  }
}

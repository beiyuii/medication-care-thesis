package com.liyile.medication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌生成与校验组件。
 *
 * @author Liyile
 */
@Component
public class JwtTokenProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
  private static final String DEFAULT_PLACEHOLDER_SECRET = "change-this-in-production";
  private static final String DEV_FALLBACK_SECRET =
      "bWVkaWNhdGlvbi1kZXYtdG9rZW4tc2VjcmV0LWZvci1sb2NhbC10ZXN0aW5nLTIwMjY=";

  /** JWT 密钥（强制配置，建议使用 Base64 编码的高熵密钥） */
  @Value("${app.jwt.secret}")
  private String jwtSecret;

  /** 令牌有效天数（默认 1 天） */
  @Value("${app.jwt.expiration-days:1}")
  private int expirationDays;

  /**
   * 根据用户名生成 JWT 令牌。
   *
   * @param username 用户名
   * @param role 角色
   * @return JWT 字符串
   */
  public String generateToken(String username, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + Duration.ofDays(expirationDays).toMillis());

    return Jwts.builder()
        .setSubject(username)
        .addClaims(java.util.Map.of("role", role))
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * 验证令牌是否合法。
   *
   * @param token JWT 字符串
   * @return 是否合法
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 从令牌中获取用户名。
   *
   * @param token JWT 字符串
   * @return 用户名
   */
  public String getUsername(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    return claims.getSubject();
  }

  /** 从令牌中获取角色。 */
  public String getRole(String token) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    Object role = claims.get("role");
    return role == null ? null : role.toString();
  }

  /** 获取签名密钥。 */
  private Key getSigningKey() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new io.jsonwebtoken.security.WeakKeyException("JWT 密钥未配置或为空");
    }
    String effectiveSecret = jwtSecret;
    if (DEFAULT_PLACEHOLDER_SECRET.equals(jwtSecret)) {
      LOGGER.warn("检测到占位 JWT 密钥，自动回退到仅用于开发环境的默认密钥。");
      effectiveSecret = DEV_FALLBACK_SECRET;
    }
    try {
      byte[] keyBytes = Decoders.BASE64.decode(effectiveSecret);
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (IllegalArgumentException ex) {
      // 非 Base64 时优先使用原始字节；长度不足则派生 SHA-256 摘要保证开发环境可用。
      byte[] rawBytes = effectiveSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      if (rawBytes.length >= 32) {
        return Keys.hmacShaKeyFor(rawBytes);
      }
      byte[] hashed = sha256(rawBytes);
      LOGGER.warn("JWT 密钥长度不足 256 bit，已派生 SHA-256 摘要作为签名密钥。");
      return Keys.hmacShaKeyFor(hashed);
    }
  }

  private byte[] sha256(byte[] rawBytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(rawBytes);
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException("当前运行环境不支持 SHA-256", ex);
    }
  }
}

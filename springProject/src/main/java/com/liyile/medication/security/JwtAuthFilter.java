package com.liyile.medication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.liyile.medication.common.CommonConstants;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器。
 * <p>解析 Authorization 头并设置认证上下文。</p>
 *
 * @author Liyile
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  /** JWT 令牌工具 */
  private final JwtTokenProvider jwtTokenProvider;

  /** 构造方法注入依赖。 */
  public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(header) && header.startsWith(CommonConstants.BEARER_PREFIX)) {
      String token = header.substring(CommonConstants.BEARER_PREFIX.length());
      if (jwtTokenProvider.validateToken(token)) {
        String username = jwtTokenProvider.getUsername(token);
        String role = jwtTokenProvider.getRole(token);
        
        // 确保角色名称不为空，并转换为大写以匹配 @PreAuthorize 中的角色名称
        if (role != null && !role.isEmpty()) {
          // Spring Security 的 hasRole() 会自动添加 ROLE_ 前缀
          // 所以我们需要确保角色名称是大写的，例如 "ELDER" 会变成 "ROLE_ELDER"
          String roleUpper = role.toUpperCase();
          GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleUpper);
          
          UserDetails userDetails = User.withUsername(username)
              .password("")
              .authorities(Collections.singletonList(authority))
              .build();
          
          UsernamePasswordAuthenticationToken auth =
              new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }
    filterChain.doFilter(request, response);
  }
}
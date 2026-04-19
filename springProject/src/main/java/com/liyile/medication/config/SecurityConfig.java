package com.liyile.medication.config;

import com.liyile.medication.security.AuthEntryPoint;
import com.liyile.medication.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liyile.medication.common.ApiResponse;
import com.liyile.medication.common.ErrorCode;
import java.util.Arrays;

/**
 * 安全配置类。
 *
 * @author Liyile
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /** JWT 认证过滤器 */
  private final JwtAuthFilter jwtAuthFilter;
  /** 未认证入口处理器 */
  private final AuthEntryPoint authEntryPoint;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthEntryPoint authEntryPoint) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.authEntryPoint = authEntryPoint;
  }

  /** 密码编码器（BCrypt）。 */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 认证管理器。 */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  /** CORS 配置源。 */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // 允许的源（包括前端开发服务器）
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    // 允许的HTTP方法
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    // 允许的请求头
    configuration.setAllowedHeaders(Arrays.asList("*"));
    // 允许携带凭证（cookies、authorization headers等）
    configuration.setAllowCredentials(true);
    // 暴露的响应头
    configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id"));
    // 预检请求的缓存时间（秒）
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /** 安全过滤链配置。 */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) -> {
      response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json;charset=UTF-8");
      ApiResponse<Void> body = ApiResponse.failure(ErrorCode.FORBIDDEN, "无权限访问");
      response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    };

    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(eh -> eh.authenticationEntryPoint(authEntryPoint).accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**", "/health", "/api/status/ping", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**", "/uploads/**", "/logs/**").permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}

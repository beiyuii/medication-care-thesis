package com.liyile.medication.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TraceId过滤器。
 * <p>为每个请求生成唯一的traceId，用于链路追踪和错误排查。</p>
 *
 * @author Liyile
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // 生成traceId
    String traceId = UUID.randomUUID().toString();
    
    // 存储到MDC中，供日志和响应使用
    MDC.put("traceId", traceId);
    
    // 在响应头中添加traceId
    response.setHeader("X-Trace-Id", traceId);
    
    try {
      filterChain.doFilter(request, response);
    } finally {
      // 请求结束后清理MDC
      MDC.clear();
    }
  }
}


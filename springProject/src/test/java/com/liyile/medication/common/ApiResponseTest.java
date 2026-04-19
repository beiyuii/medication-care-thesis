package com.liyile.medication.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ApiResponse 单元测试。
 * <p>验证统一响应结构的成功与失败构造方法。</p>
 */
public class ApiResponseTest {

  /** 测试成功响应的构造。 */
  @Test
  @DisplayName("success(T) 应返回 code=200, message=success, data=payload")
  void successResponseShouldContainExpectedFields() {
    String payload = "ok";
    ApiResponse<String> resp = ApiResponse.success(payload);
    assertEquals(ErrorCode.SUCCESS, resp.getCode(), "成功响应 code 应为 200");
    assertEquals("success", resp.getMessage(), "成功响应 message 应为 success");
    assertEquals(payload, resp.getData(), "成功响应 data 应为传入 payload");
  }

  /** 测试失败响应的构造。 */
  @Test
  @DisplayName("failure(code,msg) 应返回给定 code 与 message, data=null")
  void failureResponseShouldContainExpectedFields() {
    int code = 401;
    String message = "unauthorized";
    ApiResponse<String> resp = ApiResponse.failure(code, message);
    assertEquals(code, resp.getCode(), "失败响应 code 应为传入 code");
    assertEquals(message, resp.getMessage(), "失败响应 message 应为传入 message");
    assertNull(resp.getData(), "失败响应 data 应为 null");
  }
}
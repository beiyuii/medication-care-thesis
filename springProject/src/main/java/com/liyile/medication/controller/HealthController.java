package com.liyile.medication.controller;

import com.liyile.medication.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器。
 * <p>提供系统健康检查接口，用于监控服务状态。</p>
 *
 * @author Liyile
 */
@Tag(name = "系统健康检查", description = "系统健康状态检查接口，无需鉴权")
@RestController
public class HealthController {
  /**
   * 健康检查接口。
   * <p>返回系统健康状态，用于监控和心跳检测。该接口无需鉴权，可被任何客户端访问。</p>
   *
   * @return 健康状态字符串，正常返回"OK"
   */
  @Operation(
      summary = "系统健康检查",
      description = "返回系统健康状态，用于监控和心跳检测。该接口无需鉴权，可被任何客户端访问")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "系统正常，返回OK")
  @GetMapping("/health")
  public ApiResponse<String> health() {
    return ApiResponse.success("OK");
  }
}
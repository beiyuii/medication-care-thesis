package com.liyile.medication.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.slf4j.MDC;

/**
 * 统一响应结构。
 * <p>用于规范接口返回的数据格式。</p>
 *
 * @author Liyile
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {
  /** 响应码（200 表示成功，其它表示错误） */
  @JsonProperty("code")
  private int code;

  /** 响应信息（错误描述或提示信息） */
  @JsonProperty("message")
  private String message;

  /** 具体数据载体 */
  @JsonProperty("data")
  private T data;

  /** 请求链路ID（用于错误追踪） */
  @JsonProperty("traceId")
  private String traceId;

  /** 响应时间戳（ISO8601格式） */
  @JsonProperty("timestamp")
  private String timestamp;

  /** 无参构造方法。 */
  public ApiResponse() {}

  /** 全参构造方法。 */
  public ApiResponse(int code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.traceId = MDC.get("traceId");
    this.timestamp = Instant.now().toString();
  }

  /** 获取响应码。 */
  public int getCode() {
    return code;
  }

  /** 设置响应码。 */
  public void setCode(int code) {
    this.code = code;
  }

  /** 获取响应信息。 */
  public String getMessage() {
    return message;
  }

  /** 设置响应信息。 */
  public void setMessage(String message) {
    this.message = message;
  }

  /** 获取数据载体。 */
  public T getData() {
    return data;
  }

  /** 设置数据载体。 */
  public void setData(T data) {
    this.data = data;
  }

  /** 获取请求链路ID。 */
  public String getTraceId() {
    return traceId;
  }

  /** 设置请求链路ID。 */
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  /** 获取响应时间戳。 */
  public String getTimestamp() {
    return timestamp;
  }

  /** 设置响应时间戳。 */
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /** 成功响应构造方法。 */
  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>(ErrorCode.SUCCESS, "success", data);
    response.traceId = MDC.get("traceId");
    response.timestamp = Instant.now().toString();
    return response;
  }

  /** 失败响应构造方法。 */
  public static <T> ApiResponse<T> failure(int code, String message) {
    ApiResponse<T> response = new ApiResponse<>(code, message, null);
    response.traceId = MDC.get("traceId");
    response.timestamp = Instant.now().toString();
    return response;
  }
}
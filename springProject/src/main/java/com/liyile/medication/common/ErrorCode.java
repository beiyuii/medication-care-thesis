package com.liyile.medication.common;

/**
 * 错误码枚举类。
 * <p>统一管理系统中的错误码和成功码。</p>
 *
 * @author Liyile
 */
public final class ErrorCode {
  /** 成功响应码 */
  public static final int SUCCESS = 200;
  /** 未认证错误码 */
  public static final int UNAUTHORIZED = 401;
  /** 无权限错误码 */
  public static final int FORBIDDEN = 403;
  /** 资源未找到错误码 */
  public static final int NOT_FOUND = 404;
  /** 参数错误码 */
  public static final int UNPROCESSABLE = 422;
  /** 服务器异常错误码 */
  public static final int SERVER_ERROR = 500;

  private ErrorCode() {}
}
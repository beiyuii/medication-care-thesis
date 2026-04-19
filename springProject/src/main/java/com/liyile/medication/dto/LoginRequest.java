package com.liyile.medication.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数。
 *
 * @author Liyile
 */
public class LoginRequest {
  /** 用户名 */
  @NotBlank private String username;
  /** 密码 */
  @NotBlank private String password;

  /** 获取用户名。 */
  public String getUsername() {
    return username;
  }

  /** 设置用户名。 */
  public void setUsername(String username) {
    this.username = username;
  }

  /** 获取密码。 */
  public String getPassword() {
    return password;
  }

  /** 设置密码。 */
  public void setPassword(String password) {
    this.password = password;
  }
}
package com.liyile.medication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注册请求参数对象。
 * <p>包含用户名、登录密码与角色。</p>
 *
 * @author Liyile
 */
public class RegisterRequest {
  /** 用户名 */
  @NotBlank(message = "用户名不能为空")
  private String username;

  /** 登录密码 */
  @NotBlank(message = "密码不能为空")
  private String password;

  /** 角色（elder/caregiver/child） */
  @NotBlank(message = "角色不能为空")
  @Pattern(regexp = "elder|caregiver|child", message = "角色必须为 elder/caregiver/child")
  private String role;

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

  /** 获取角色。 */
  public String getRole() {
    return role;
  }

  /** 设置角色。 */
  public void setRole(String role) {
    this.role = role;
  }
}

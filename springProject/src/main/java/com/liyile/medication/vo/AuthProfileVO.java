package com.liyile.medication.vo;

import java.util.List;

/**
 * 认证后的用户信息视图对象。
 *
 * @author Liyile
 */
public class AuthProfileVO {
  /** 用户ID */
  private Long userId;
  /** 用户名 */
  private String username;
  /** 角色 */
  private String role;
  /** 用户姓名（真实姓名） */
  private String name;
  /** 关联的患者列表（护工/子女用） */
  private List<PatientSummaryVO> patients;

  /** 无参构造方法。 */
  public AuthProfileVO() {}

  /** 全参构造方法。 */
  public AuthProfileVO(Long userId, String username, String role, String name, List<PatientSummaryVO> patients) {
    this.userId = userId;
    this.username = username;
    this.role = role;
    this.name = name;
    this.patients = patients;
  }

  /** 兼容旧版本的构造方法。 */
  public AuthProfileVO(String username, String role) {
    this.username = username;
    this.role = role;
    this.name = username;
  }

  /** 获取用户名。 */
  public String getUsername() {
    return username;
  }

  /** 设置用户名。 */
  public void setUsername(String username) {
    this.username = username;
  }

  /** 获取角色。 */
  public String getRole() {
    return role;
  }

  /** 设置角色。 */
  public void setRole(String role) {
    this.role = role;
  }

  /** 获取用户ID。 */
  public Long getUserId() {
    return userId;
  }

  /** 设置用户ID。 */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /** 获取用户姓名。 */
  public String getName() {
    return name;
  }

  /** 设置用户姓名。 */
  public void setName(String name) {
    this.name = name;
  }

  /** 获取关联的患者列表。 */
  public List<PatientSummaryVO> getPatients() {
    return patients;
  }

  /** 设置关联的患者列表。 */
  public void setPatients(List<PatientSummaryVO> patients) {
    this.patients = patients;
  }

  /**
   * toString 方法。
   *
   * @return 字符串表示
   */
  @Override
  public String toString() {
    return "AuthProfileVO{" + "userId=" + userId + ", username='" + username + '\'' + ", role='" + role + '\'' + ", name='" + name + '\'' + '}';
  }
}
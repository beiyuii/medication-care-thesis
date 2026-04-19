package com.liyile.medication.vo;

import java.util.List;

/**
 * 用户信息VO。
 * <p>用于返回当前登录用户的个人信息，包含用户ID、角色、姓名和关联的患者列表。</p>
 *
 * @author Liyile
 */
public class ProfileVO {
  /** 用户ID */
  private Long userId;

  /** 角色（elder/caregiver/child） */
  private String role;

  /** 用户姓名 */
  private String name;

  /** 关联的患者列表（护工/子女用） */
  private List<PatientSummaryVO> patients;

  /** 无参构造方法。 */
  public ProfileVO() {}

  /** 全参构造方法。 */
  public ProfileVO(Long userId, String role, String name, List<PatientSummaryVO> patients) {
    this.userId = userId;
    this.role = role;
    this.name = name;
    this.patients = patients;
  }

  /** 获取用户ID。 */
  public Long getUserId() {
    return userId;
  }

  /** 设置用户ID。 */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /** 获取角色。 */
  public String getRole() {
    return role;
  }

  /** 设置角色。 */
  public void setRole(String role) {
    this.role = role;
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
}


package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户实体。
 * <p>对应数据表 users。</p>
 *
 * @author Liyile
 */
@TableName("users")
public class User {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户名 */
  private String username;

  /** 密码哈希（或明文，仅开发阶段允许） */
  private String pwdHash;

  /** 角色（elder/caregiver/child） */
  private String role;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取用户名。 */
  public String getUsername() {
    return username;
  }

  /** 设置用户名。 */
  public void setUsername(String username) {
    this.username = username;
  }

  /** 获取密码哈希。 */
  public String getPwdHash() {
    return pwdHash;
  }

  /** 设置密码哈希。 */
  public void setPwdHash(String pwdHash) {
    this.pwdHash = pwdHash;
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
package com.liyile.medication.vo;

import java.util.List;
import java.util.Map;

/**
 * 角色权限矩阵VO。
 * <p>用于返回当前用户的角色和权限信息。</p>
 *
 * @author Liyile
 */
public class RolePermissionVO {
  /** 角色（elder/caregiver/child） */
  private String role;

  /** 权限映射（资源 -> 权限列表） */
  private Map<String, List<String>> permissions;

  /** 无参构造方法。 */
  public RolePermissionVO() {}

  /** 全参构造方法。 */
  public RolePermissionVO(String role, Map<String, List<String>> permissions) {
    this.role = role;
    this.permissions = permissions;
  }

  /** 获取角色。 */
  public String getRole() {
    return role;
  }

  /** 设置角色。 */
  public void setRole(String role) {
    this.role = role;
  }

  /** 获取权限映射。 */
  public Map<String, List<String>> getPermissions() {
    return permissions;
  }

  /** 设置权限映射。 */
  public void setPermissions(Map<String, List<String>> permissions) {
    this.permissions = permissions;
  }
}


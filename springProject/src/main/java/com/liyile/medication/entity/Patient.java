package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 被照护人实体。
 * <p>对应数据表 patients。</p>
 *
 * @author Liyile
 */
@TableName("patients")
public class Patient {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 关联的老年人用户 ID */
  private Long elderUserId;

  /** 姓名 */
  private String name;

  /** 年龄 */
  private Integer age;

  /** 联系电话 */
  private String phone;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取关联的老年人用户 ID。 */
  public Long getElderUserId() {
    return elderUserId;
  }

  /** 设置关联的老年人用户 ID。 */
  public void setElderUserId(Long elderUserId) {
    this.elderUserId = elderUserId;
  }

  /** 获取姓名。 */
  public String getName() {
    return name;
  }

  /** 设置姓名。 */
  public void setName(String name) {
    this.name = name;
  }

  /** 获取年龄。 */
  public Integer getAge() {
    return age;
  }

  /** 设置年龄。 */
  public void setAge(Integer age) {
    this.age = age;
  }

  /** 获取联系电话。 */
  public String getPhone() {
    return phone;
  }

  /** 设置联系电话。 */
  public void setPhone(String phone) {
    this.phone = phone;
  }
}
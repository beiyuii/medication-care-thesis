package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户患者关联实体。
 * <p>对应数据表 user_patient_relation，用于记录护工/子女与患者的关联关系。</p>
 *
 * @author Liyile
 */
@TableName("user_patient_relation")
public class UserPatientRelation {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 护工/子女用户 ID */
  private Long userId;

  /** 患者 ID */
  private Long patientId;

  /** 关联类型（caregiver/child） */
  private String relationType;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取用户 ID。 */
  public Long getUserId() {
    return userId;
  }

  /** 设置用户 ID。 */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /** 获取患者 ID。 */
  public Long getPatientId() {
    return patientId;
  }

  /** 设置患者 ID。 */
  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  /** 获取关联类型。 */
  public String getRelationType() {
    return relationType;
  }

  /** 设置关联类型。 */
  public void setRelationType(String relationType) {
    this.relationType = relationType;
  }
}


package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 异常与告警实体。
 * <p>对应数据表 alerts。</p>
 *
 * @author Liyile
 */
@TableName("alerts")
public class Alert {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 患者 ID */
  private Long patientId;

  /** 告警标题 */
  private String title;

  /** 告警描述 */
  private String description;

  /** 严重程度（high/medium/low） */
  private String severity;

  /** 告警类型（字符串） */
  private String type;

  /** 时间戳 */
  private String ts;

  /** 状态 */
  private String status;

  /** 处理时间 */
  private java.sql.Timestamp resolvedAt;

  /** 处理备注 */
  private String actionNote;

  /** 关联提醒实例 ID */
  private Long reminderInstanceId;

  /** 关联检测任务 ID */
  private Long detectionJobId;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取患者 ID。 */
  public Long getPatientId() {
    return patientId;
  }

  /** 设置患者 ID。 */
  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  /** 获取告警标题。 */
  public String getTitle() {
    return title;
  }

  /** 设置告警标题。 */
  public void setTitle(String title) {
    this.title = title;
  }

  /** 获取告警描述。 */
  public String getDescription() {
    return description;
  }

  /** 设置告警描述。 */
  public void setDescription(String description) {
    this.description = description;
  }

  /** 获取严重程度。 */
  public String getSeverity() {
    return severity;
  }

  /** 设置严重程度。 */
  public void setSeverity(String severity) {
    this.severity = severity;
  }

  /** 获取告警类型。 */
  public String getType() {
    return type;
  }

  /** 设置告警类型。 */
  public void setType(String type) {
    this.type = type;
  }

  /** 获取时间戳。 */
  public String getTs() {
    return ts;
  }

  /** 设置时间戳。 */
  public void setTs(String ts) {
    this.ts = ts;
  }

  /** 获取状态。 */
  public String getStatus() {
    return status;
  }

  /** 设置状态。 */
  public void setStatus(String status) {
    this.status = status;
  }

  /** 获取处理时间。 */
  public java.sql.Timestamp getResolvedAt() {
    return resolvedAt;
  }

  /** 设置处理时间。 */
  public void setResolvedAt(java.sql.Timestamp resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  /** 获取处理备注。 */
  public String getActionNote() {
    return actionNote;
  }

  /** 设置处理备注。 */
  public void setActionNote(String actionNote) {
    this.actionNote = actionNote;
  }

  /** 获取关联提醒实例 ID。 */
  public Long getReminderInstanceId() {
    return reminderInstanceId;
  }

  /** 设置关联提醒实例 ID。 */
  public void setReminderInstanceId(Long reminderInstanceId) {
    this.reminderInstanceId = reminderInstanceId;
  }

  /** 获取关联检测任务 ID。 */
  public Long getDetectionJobId() {
    return detectionJobId;
  }

  /** 设置关联检测任务 ID。 */
  public void setDetectionJobId(Long detectionJobId) {
    this.detectionJobId = detectionJobId;
  }
}

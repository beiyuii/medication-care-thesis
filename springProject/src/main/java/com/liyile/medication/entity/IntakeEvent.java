package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 服药事件实体。
 * <p>对应数据表 intake_events。</p>
 *
 * @author Liyile
 */
@TableName("intake_events")
public class IntakeEvent {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 患者 ID */
  private Long patientId;

  /** 计划 ID */
  private Long scheduleId;

  /** 事件时间戳（ISO 字符串或 long） */
  private String ts;

  /** 事件状态（suspected/confirmed/abnormal） */
  private String status;

  /** 确认人 */
  private String confirmedBy;

  /** 确认时间 */
  private java.sql.Timestamp confirmedAt;

  /** 动作描述（如 hand_to_mouth） */
  private String action;

  /** 目标检测 JSON 字符串 */
  private String targetsJson;

  /** 图片 URL */
  private String imgUrl;

  /** 检测原始录像 URL（通常为 /uploads/videos/ 下的相对路径） */
  private String videoUrl;

  /** 关联提醒实例 ID */
  private Long reminderInstanceId;

  /** 关联检测任务 ID */
  private Long detectionJobId;

  /** 非持久化药品名称。 */
  @TableField(exist = false)
  private String medicineName;

  /** 非持久化计划名称。 */
  @TableField(exist = false)
  private String scheduleName;

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

  /** 获取计划 ID。 */
  public Long getScheduleId() {
    return scheduleId;
  }

  /** 设置计划 ID。 */
  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  /** 获取时间戳。 */
  public String getTs() {
    return ts;
  }

  /** 设置时间戳。 */
  public void setTs(String ts) {
    this.ts = ts;
  }

  /** 获取事件状态。 */
  public String getStatus() {
    return status;
  }

  /** 设置事件状态。 */
  public void setStatus(String status) {
    this.status = status;
  }

  /** 获取确认人。 */
  public String getConfirmedBy() {
    return confirmedBy;
  }

  /** 设置确认人。 */
  public void setConfirmedBy(String confirmedBy) {
    this.confirmedBy = confirmedBy;
  }

  /** 获取确认时间。 */
  public java.sql.Timestamp getConfirmedAt() {
    return confirmedAt;
  }

  /** 设置确认时间。 */
  public void setConfirmedAt(java.sql.Timestamp confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

  /** 获取动作描述。 */
  public String getAction() {
    return action;
  }

  /** 设置动作描述。 */
  public void setAction(String action) {
    this.action = action;
  }

  /** 获取目标检测 JSON。 */
  public String getTargetsJson() {
    return targetsJson;
  }

  /** 设置目标检测 JSON。 */
  public void setTargetsJson(String targetsJson) {
    this.targetsJson = targetsJson;
  }

  /** 获取图片 URL。 */
  public String getImgUrl() {
    return imgUrl;
  }

  /** 设置图片 URL。 */
  public void setImgUrl(String imgUrl) {
    this.imgUrl = imgUrl;
  }

  /** 获取检测原始录像 URL。 */
  public String getVideoUrl() {
    return videoUrl;
  }

  /** 设置检测原始录像 URL。 */
  public void setVideoUrl(String videoUrl) {
    this.videoUrl = videoUrl;
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

  public String getMedicineName() {
    return medicineName;
  }

  public void setMedicineName(String medicineName) {
    this.medicineName = medicineName;
  }

  public String getScheduleName() {
    return scheduleName;
  }

  public void setScheduleName(String scheduleName) {
    this.scheduleName = scheduleName;
  }
}

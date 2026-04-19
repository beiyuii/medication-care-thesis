package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 每日提醒实例实体。
 *
 * <p>由用药计划按天实例化生成，承载当天一次具体的提醒执行状态。</p>
 */
@TableName("reminder_instances")
public class ReminderInstance {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long patientId;
  private Long scheduleId;
  private Date scheduledDate;
  private Timestamp windowStartAt;
  private Timestamp windowEndAt;
  private String status;
  private Timestamp confirmedAt;
  private Long lastEventId;
  private Long lastDetectionJobId;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public Date getScheduledDate() {
    return scheduledDate;
  }

  public void setScheduledDate(Date scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public Timestamp getWindowStartAt() {
    return windowStartAt;
  }

  public void setWindowStartAt(Timestamp windowStartAt) {
    this.windowStartAt = windowStartAt;
  }

  public Timestamp getWindowEndAt() {
    return windowEndAt;
  }

  public void setWindowEndAt(Timestamp windowEndAt) {
    this.windowEndAt = windowEndAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Timestamp getConfirmedAt() {
    return confirmedAt;
  }

  public void setConfirmedAt(Timestamp confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

  public Long getLastEventId() {
    return lastEventId;
  }

  public void setLastEventId(Long lastEventId) {
    this.lastEventId = lastEventId;
  }

  public Long getLastDetectionJobId() {
    return lastDetectionJobId;
  }

  public void setLastDetectionJobId(Long lastDetectionJobId) {
    this.lastDetectionJobId = lastDetectionJobId;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
  }
}

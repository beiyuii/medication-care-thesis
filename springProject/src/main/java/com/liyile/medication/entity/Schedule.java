package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用药计划实体。
 * <p>对应数据表 schedules。</p>
 *
 * @author Liyile
 */
@TableName("schedules")
public class Schedule {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 患者 ID */
  private Long patientId;

  /** 药品名称 */
  private String medicineName;

  /** 药品类型（PILL/BLISTER/BOTTLE/BOX） */
  private String type;

  /** 剂量描述 */
  private String dose;

  /** 频次（如每天、每周） */
  private String freq;

  /** 时间窗开始（HH:mm） */
  private String winStart;

  /** 时间窗结束（HH:mm） */
  private String winEnd;

  /** 周期描述（如 1-30 天） */
  private String period;

  /** 状态（启用/停用） */
  private String status;

  /** 下次提醒时间（非持久化字段）。 */
  @TableField(exist = false)
  private String nextIntake;

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

  /** 获取药品名称。 */
  public String getMedicineName() {
    return medicineName;
  }

  /** 设置药品名称。 */
  public void setMedicineName(String medicineName) {
    this.medicineName = medicineName;
  }

  /** 获取药品类型。 */
  public String getType() {
    return type;
  }

  /** 设置药品类型。 */
  public void setType(String type) {
    this.type = type;
  }

  /** 获取剂量描述。 */
  public String getDose() {
    return dose;
  }

  /** 设置剂量描述。 */
  public void setDose(String dose) {
    this.dose = dose;
  }

  /** 获取频次。 */
  public String getFreq() {
    return freq;
  }

  /** 设置频次。 */
  public void setFreq(String freq) {
    this.freq = freq;
  }

  /** 获取时间窗开始。 */
  public String getWinStart() {
    return winStart;
  }

  /** 设置时间窗开始。 */
  public void setWinStart(String winStart) {
    this.winStart = winStart;
  }

  /** 获取时间窗结束。 */
  public String getWinEnd() {
    return winEnd;
  }

  /** 设置时间窗结束。 */
  public void setWinEnd(String winEnd) {
    this.winEnd = winEnd;
  }

  /** 获取周期描述。 */
  public String getPeriod() {
    return period;
  }

  /** 设置周期描述。 */
  public void setPeriod(String period) {
    this.period = period;
  }

  /** 获取状态。 */
  public String getStatus() {
    return status;
  }

  /** 设置状态。 */
  public void setStatus(String status) {
    this.status = status;
  }

  /** 获取下次提醒时间。 */
  public String getNextIntake() {
    return nextIntake;
  }

  /** 设置下次提醒时间。 */
  public void setNextIntake(String nextIntake) {
    this.nextIntake = nextIntake;
  }
}

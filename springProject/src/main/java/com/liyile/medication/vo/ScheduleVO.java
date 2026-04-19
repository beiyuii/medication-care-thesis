package com.liyile.medication.vo;

import com.liyile.medication.entity.Schedule;

/**
 * 用药计划VO。
 * <p>用于前端展示用药计划信息，统一字段命名。</p>
 *
 * @author Liyile
 */
public class ScheduleVO {
  /** 计划ID */
  private Long id;

  /** 患者ID */
  private Long patientId;

  /** 药品名称 */
  private String medicineName;

  /** 药品类型（PILL/BLISTER/BOTTLE/BOX） */
  private String type;

  /** 剂量 */
  private String dosage;

  /** 频次 */
  private String frequency;

  /** 时间窗 */
  private WindowVO window;

  /** 周期 */
  private String period;

  /** 状态（active/paused） */
  private String status;

  /** 下次服药时间（ISO8601格式） */
  private String nextIntake;

  /** 无参构造方法。 */
  public ScheduleVO() {}

  /**
   * 从Schedule实体转换为VO。
   *
   * @param schedule 用药计划实体
   * @return ScheduleVO对象
   */
  public static ScheduleVO from(Schedule schedule) {
    ScheduleVO vo = new ScheduleVO();
    vo.setId(schedule.getId());
    vo.setPatientId(schedule.getPatientId());
    vo.setMedicineName(schedule.getMedicineName());
    vo.setType(schedule.getType());
    vo.setDosage(schedule.getDose());
    vo.setFrequency(schedule.getFreq());
    vo.setWindow(new WindowVO(schedule.getWinStart(), schedule.getWinEnd()));
    vo.setPeriod(schedule.getPeriod());
    // 状态转换：enabled -> active, disabled -> paused
    vo.setStatus("enabled".equalsIgnoreCase(schedule.getStatus()) ? "active" : "paused");
    vo.setNextIntake(schedule.getNextIntake());
    return vo;
  }

  /** 获取计划ID。 */
  public Long getId() {
    return id;
  }

  /** 设置计划ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取患者ID。 */
  public Long getPatientId() {
    return patientId;
  }

  /** 设置患者ID。 */
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

  /** 获取剂量。 */
  public String getDosage() {
    return dosage;
  }

  /** 设置剂量。 */
  public void setDosage(String dosage) {
    this.dosage = dosage;
  }

  /** 获取频次。 */
  public String getFrequency() {
    return frequency;
  }

  /** 设置频次。 */
  public void setFrequency(String frequency) {
    this.frequency = frequency;
  }

  /** 获取时间窗。 */
  public WindowVO getWindow() {
    return window;
  }

  /** 设置时间窗。 */
  public void setWindow(WindowVO window) {
    this.window = window;
  }

  /** 获取周期。 */
  public String getPeriod() {
    return period;
  }

  /** 设置周期。 */
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

  /** 获取下次服药时间。 */
  public String getNextIntake() {
    return nextIntake;
  }

  /** 设置下次服药时间。 */
  public void setNextIntake(String nextIntake) {
    this.nextIntake = nextIntake;
  }
}

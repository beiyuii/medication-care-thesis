package com.liyile.medication.vo;

/**
 * 患者摘要VO。
 * <p>用于患者列表展示，包含基本信息、下次服药时间、计划状态、告警数量等。</p>
 *
 * @author Liyile
 */
public class PatientSummaryVO {
  /** 患者ID */
  private Long id;

  /** 患者姓名 */
  private String name;

  /** 下次服药时间（ISO8601格式） */
  private String nextIntakeTime;

  /** 计划状态（active/paused） */
  private String planStatus;

  /** 告警数量 */
  private Integer alertCount;

  /** 无参构造方法。 */
  public PatientSummaryVO() {}

  /** 全参构造方法。 */
  public PatientSummaryVO(Long id, String name, String nextIntakeTime, String planStatus, Integer alertCount) {
    this.id = id;
    this.name = name;
    this.nextIntakeTime = nextIntakeTime;
    this.planStatus = planStatus;
    this.alertCount = alertCount;
  }

  /** 获取患者ID。 */
  public Long getId() {
    return id;
  }

  /** 设置患者ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取患者姓名。 */
  public String getName() {
    return name;
  }

  /** 设置患者姓名。 */
  public void setName(String name) {
    this.name = name;
  }

  /** 获取下次服药时间。 */
  public String getNextIntakeTime() {
    return nextIntakeTime;
  }

  /** 设置下次服药时间。 */
  public void setNextIntakeTime(String nextIntakeTime) {
    this.nextIntakeTime = nextIntakeTime;
  }

  /** 获取计划状态。 */
  public String getPlanStatus() {
    return planStatus;
  }

  /** 设置计划状态。 */
  public void setPlanStatus(String planStatus) {
    this.planStatus = planStatus;
  }

  /** 获取告警数量。 */
  public Integer getAlertCount() {
    return alertCount;
  }

  /** 设置告警数量。 */
  public void setAlertCount(Integer alertCount) {
    this.alertCount = alertCount;
  }
}


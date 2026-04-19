package com.liyile.medication.vo;

/**
 * 统计报表摘要VO。
 * <p>用于前端展示服药统计信息。</p>
 *
 * @author Liyile
 */
public class ReportSummaryVO {
  /** 患者ID */
  private Long patientId;

  /** 时间范围（day/week/month/all） */
  private String range;

  /** 总提醒次数 */
  private Integer totalReminders;

  /** 已确认次数 */
  private Integer confirmedCount;

  /** 确认率（0.0-1.0） */
  private Double confirmRate;

  /** 平均响应时间（秒） */
  private Long avgResponseTime;

  /** 异常次数 */
  private Integer abnormalCount;

  /** 漏服次数 */
  private Integer missedCount;

  /** 无参构造方法。 */
  public ReportSummaryVO() {}

  /** 获取患者ID。 */
  public Long getPatientId() {
    return patientId;
  }

  /** 设置患者ID。 */
  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  /** 获取时间范围。 */
  public String getRange() {
    return range;
  }

  /** 设置时间范围。 */
  public void setRange(String range) {
    this.range = range;
  }

  /** 获取总提醒次数。 */
  public Integer getTotalReminders() {
    return totalReminders;
  }

  /** 设置总提醒次数。 */
  public void setTotalReminders(Integer totalReminders) {
    this.totalReminders = totalReminders;
  }

  /** 获取已确认次数。 */
  public Integer getConfirmedCount() {
    return confirmedCount;
  }

  /** 设置已确认次数。 */
  public void setConfirmedCount(Integer confirmedCount) {
    this.confirmedCount = confirmedCount;
  }

  /** 获取确认率。 */
  public Double getConfirmRate() {
    return confirmRate;
  }

  /** 设置确认率。 */
  public void setConfirmRate(Double confirmRate) {
    this.confirmRate = confirmRate;
  }

  /** 获取平均响应时间。 */
  public Long getAvgResponseTime() {
    return avgResponseTime;
  }

  /** 设置平均响应时间。 */
  public void setAvgResponseTime(Long avgResponseTime) {
    this.avgResponseTime = avgResponseTime;
  }

  /** 获取异常次数。 */
  public Integer getAbnormalCount() {
    return abnormalCount;
  }

  /** 设置异常次数。 */
  public void setAbnormalCount(Integer abnormalCount) {
    this.abnormalCount = abnormalCount;
  }

  /** 获取漏服次数。 */
  public Integer getMissedCount() {
    return missedCount;
  }

  /** 设置漏服次数。 */
  public void setMissedCount(Integer missedCount) {
    this.missedCount = missedCount;
  }
}


package com.liyile.medication.dto;

/**
 * 确认事件DTO。
 * <p>用于手动确认服药事件的请求参数。</p>
 *
 * @author Liyile
 */
public class ConfirmEventDTO {
  /** 确认人 */
  private String confirmedBy;

  /** 确认时间（ISO8601格式） */
  private String confirmTime;

  /** 无参构造方法。 */
  public ConfirmEventDTO() {}

  /** 获取确认人。 */
  public String getConfirmedBy() {
    return confirmedBy;
  }

  /** 设置确认人。 */
  public void setConfirmedBy(String confirmedBy) {
    this.confirmedBy = confirmedBy;
  }

  /** 获取确认时间。 */
  public String getConfirmTime() {
    return confirmTime;
  }

  /** 设置确认时间。 */
  public void setConfirmTime(String confirmTime) {
    this.confirmTime = confirmTime;
  }
}


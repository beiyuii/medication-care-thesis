package com.liyile.medication.dto;

/**
 * 处理告警DTO。
 * <p>用于标记告警已处理的请求参数。</p>
 *
 * @author Liyile
 */
public class ResolveAlertDTO {
  /** 处理备注 */
  private String actionNote;

  /** 无参构造方法。 */
  public ResolveAlertDTO() {}

  /** 获取处理备注。 */
  public String getActionNote() {
    return actionNote;
  }

  /** 设置处理备注。 */
  public void setActionNote(String actionNote) {
    this.actionNote = actionNote;
  }
}


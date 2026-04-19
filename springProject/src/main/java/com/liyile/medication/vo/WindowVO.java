package com.liyile.medication.vo;

/**
 * 时间窗VO。
 * <p>用于前端展示用药计划的时间窗信息。</p>
 *
 * @author Liyile
 */
public class WindowVO {
  /** 时间窗开始（HH:mm） */
  private String start;

  /** 时间窗结束（HH:mm） */
  private String end;

  /** 无参构造方法。 */
  public WindowVO() {}

  /** 全参构造方法。 */
  public WindowVO(String start, String end) {
    this.start = start;
    this.end = end;
  }

  /** 获取时间窗开始。 */
  public String getStart() {
    return start;
  }

  /** 设置时间窗开始。 */
  public void setStart(String start) {
    this.start = start;
  }

  /** 获取时间窗结束。 */
  public String getEnd() {
    return end;
  }

  /** 设置时间窗结束。 */
  public void setEnd(String end) {
    this.end = end;
  }
}


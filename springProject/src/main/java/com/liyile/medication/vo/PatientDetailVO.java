package com.liyile.medication.vo;

import java.util.List;

/**
 * 患者详情VO。
 * <p>用于前端展示患者详细信息，包含基本信息、关联计划摘要、最近告警等。</p>
 *
 * @author Liyile
 */
public class PatientDetailVO {
  /** 患者ID */
  private Long id;

  /** 患者姓名 */
  private String name;

  /** 年龄 */
  private Integer age;

  /** 联系电话 */
  private String phone;

  /** 关联的用药计划（最近5条） */
  private List<ScheduleVO> schedules;

  /** 最近告警（最近5条） */
  private List<AlertSummary> recentAlerts;

  /** 无参构造方法。 */
  public PatientDetailVO() {}

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

  /** 获取年龄。 */
  public Integer getAge() {
    return age;
  }

  /** 设置年龄。 */
  public void setAge(Integer age) {
    this.age = age;
  }

  /** 获取联系电话。 */
  public String getPhone() {
    return phone;
  }

  /** 设置联系电话。 */
  public void setPhone(String phone) {
    this.phone = phone;
  }

  /** 获取关联的用药计划。 */
  public List<ScheduleVO> getSchedules() {
    return schedules;
  }

  /** 设置关联的用药计划。 */
  public void setSchedules(List<ScheduleVO> schedules) {
    this.schedules = schedules;
  }

  /** 获取最近告警。 */
  public List<AlertSummary> getRecentAlerts() {
    return recentAlerts;
  }

  /** 设置最近告警。 */
  public void setRecentAlerts(List<AlertSummary> recentAlerts) {
    this.recentAlerts = recentAlerts;
  }

  /**
   * 告警摘要内部类。
   */
  public static class AlertSummary {
    /** 告警ID */
    private Long id;

    /** 告警标题 */
    private String title;

    /** 发生时间 */
    private String occurredAt;

    /** 无参构造方法。 */
    public AlertSummary() {}

    /** 获取告警ID。 */
    public Long getId() {
      return id;
    }

    /** 设置告警ID。 */
    public void setId(Long id) {
      this.id = id;
    }

    /** 获取告警标题。 */
    public String getTitle() {
      return title;
    }

    /** 设置告警标题。 */
    public void setTitle(String title) {
      this.title = title;
    }

    /** 获取发生时间。 */
    public String getOccurredAt() {
      return occurredAt;
    }

    /** 设置发生时间。 */
    public void setOccurredAt(String occurredAt) {
      this.occurredAt = occurredAt;
    }
  }
}


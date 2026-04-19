package com.liyile.medication.vo;

import com.liyile.medication.entity.Alert;
import java.util.List;

public class ElderDashboardVO {
  private ReminderInstanceVO nextReminder;
  private List<ReminderInstanceVO> todayInstances;
  private List<Alert> activeAlerts;
  private int completionRate;

  public ReminderInstanceVO getNextReminder() {
    return nextReminder;
  }

  public void setNextReminder(ReminderInstanceVO nextReminder) {
    this.nextReminder = nextReminder;
  }

  public List<ReminderInstanceVO> getTodayInstances() {
    return todayInstances;
  }

  public void setTodayInstances(List<ReminderInstanceVO> todayInstances) {
    this.todayInstances = todayInstances;
  }

  public List<Alert> getActiveAlerts() {
    return activeAlerts;
  }

  public void setActiveAlerts(List<Alert> activeAlerts) {
    this.activeAlerts = activeAlerts;
  }

  public int getCompletionRate() {
    return completionRate;
  }

  public void setCompletionRate(int completionRate) {
    this.completionRate = completionRate;
  }
}

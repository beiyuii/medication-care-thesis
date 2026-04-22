package com.liyile.medication.vo;

import com.liyile.medication.entity.Alert;
import com.liyile.medication.entity.IntakeEvent;
import java.util.List;

public class CaregiverDashboardVO {
  private List<PatientSummaryVO> patients;
  private PatientSummaryVO activePatient;
  private List<IntakeEvent> recentEvents;
  private List<ReminderInstanceVO> pendingReviewInstances;
  private List<Alert> activeAlerts;
  private Integer completionRate;

  public List<PatientSummaryVO> getPatients() {
    return patients;
  }

  public void setPatients(List<PatientSummaryVO> patients) {
    this.patients = patients;
  }

  public PatientSummaryVO getActivePatient() {
    return activePatient;
  }

  public void setActivePatient(PatientSummaryVO activePatient) {
    this.activePatient = activePatient;
  }

  public List<IntakeEvent> getRecentEvents() {
    return recentEvents;
  }

  public void setRecentEvents(List<IntakeEvent> recentEvents) {
    this.recentEvents = recentEvents;
  }

  public List<ReminderInstanceVO> getPendingReviewInstances() {
    return pendingReviewInstances;
  }

  public void setPendingReviewInstances(List<ReminderInstanceVO> pendingReviewInstances) {
    this.pendingReviewInstances = pendingReviewInstances;
  }

  public List<Alert> getActiveAlerts() {
    return activeAlerts;
  }

  public void setActiveAlerts(List<Alert> activeAlerts) {
    this.activeAlerts = activeAlerts;
  }

  public Integer getCompletionRate() {
    return completionRate;
  }

  public void setCompletionRate(Integer completionRate) {
    this.completionRate = completionRate;
  }
}

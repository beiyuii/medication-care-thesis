package com.liyile.medication.vo;

import java.util.List;

public class ReminderInstanceVO {
  private Long id;
  private Long patientId;
  private Long scheduleId;
  private String scheduledDate;
  private String windowStartAt;
  private String windowEndAt;
  private String status;
  private String confirmedAt;
  private Long detectionJobId;
  private Long lastEventId;
  private String medicineName;
  private String dose;
  private String frequency;
  private List<String> activeAlertTitles;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public String getScheduledDate() {
    return scheduledDate;
  }

  public void setScheduledDate(String scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public String getWindowStartAt() {
    return windowStartAt;
  }

  public void setWindowStartAt(String windowStartAt) {
    this.windowStartAt = windowStartAt;
  }

  public String getWindowEndAt() {
    return windowEndAt;
  }

  public void setWindowEndAt(String windowEndAt) {
    this.windowEndAt = windowEndAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getConfirmedAt() {
    return confirmedAt;
  }

  public void setConfirmedAt(String confirmedAt) {
    this.confirmedAt = confirmedAt;
  }

  public Long getDetectionJobId() {
    return detectionJobId;
  }

  public void setDetectionJobId(Long detectionJobId) {
    this.detectionJobId = detectionJobId;
  }

  public Long getLastEventId() {
    return lastEventId;
  }

  public void setLastEventId(Long lastEventId) {
    this.lastEventId = lastEventId;
  }

  public String getMedicineName() {
    return medicineName;
  }

  public void setMedicineName(String medicineName) {
    this.medicineName = medicineName;
  }

  public String getDose() {
    return dose;
  }

  public void setDose(String dose) {
    this.dose = dose;
  }

  public String getFrequency() {
    return frequency;
  }

  public void setFrequency(String frequency) {
    this.frequency = frequency;
  }

  public List<String> getActiveAlertTitles() {
    return activeAlertTitles;
  }

  public void setActiveAlertTitles(List<String> activeAlertTitles) {
    this.activeAlertTitles = activeAlertTitles;
  }
}

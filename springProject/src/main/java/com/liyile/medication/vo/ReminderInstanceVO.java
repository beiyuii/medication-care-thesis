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
  private String reviewStatus;
  private String detectionStatus;
  private Long parentInstanceId;
  private Integer retryCount;
  private String reviewDeadline;
  private Integer lateMinutes;
  private String reviewedBy;
  private String reviewedAt;
  private String reviewReason;
  private String confirmedAt;
  private Long detectionJobId;
  private Double targetConfidence;
  private Double actionConfidence;
  private Double finalConfidence;
  private String detectionReasonCode;
  private String detectionReasonText;
  private String detectionRiskTag;
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

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getDetectionStatus() {
    return detectionStatus;
  }

  public void setDetectionStatus(String detectionStatus) {
    this.detectionStatus = detectionStatus;
  }

  public Long getParentInstanceId() {
    return parentInstanceId;
  }

  public void setParentInstanceId(Long parentInstanceId) {
    this.parentInstanceId = parentInstanceId;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public String getReviewDeadline() {
    return reviewDeadline;
  }

  public void setReviewDeadline(String reviewDeadline) {
    this.reviewDeadline = reviewDeadline;
  }

  public Integer getLateMinutes() {
    return lateMinutes;
  }

  public void setLateMinutes(Integer lateMinutes) {
    this.lateMinutes = lateMinutes;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(String reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public String getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(String reviewedAt) {
    this.reviewedAt = reviewedAt;
  }

  public String getReviewReason() {
    return reviewReason;
  }

  public void setReviewReason(String reviewReason) {
    this.reviewReason = reviewReason;
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

  public Double getTargetConfidence() {
    return targetConfidence;
  }

  public void setTargetConfidence(Double targetConfidence) {
    this.targetConfidence = targetConfidence;
  }

  public Double getActionConfidence() {
    return actionConfidence;
  }

  public void setActionConfidence(Double actionConfidence) {
    this.actionConfidence = actionConfidence;
  }

  public Double getFinalConfidence() {
    return finalConfidence;
  }

  public void setFinalConfidence(Double finalConfidence) {
    this.finalConfidence = finalConfidence;
  }

  public String getDetectionReasonCode() {
    return detectionReasonCode;
  }

  public void setDetectionReasonCode(String detectionReasonCode) {
    this.detectionReasonCode = detectionReasonCode;
  }

  public String getDetectionReasonText() {
    return detectionReasonText;
  }

  public void setDetectionReasonText(String detectionReasonText) {
    this.detectionReasonText = detectionReasonText;
  }

  public String getDetectionRiskTag() {
    return detectionRiskTag;
  }

  public void setDetectionRiskTag(String detectionRiskTag) {
    this.detectionRiskTag = detectionRiskTag;
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

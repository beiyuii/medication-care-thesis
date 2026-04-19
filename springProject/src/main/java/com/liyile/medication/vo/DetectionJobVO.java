package com.liyile.medication.vo;

public class DetectionJobVO {
  private Long id;
  private Long patientId;
  private Long scheduleId;
  private Long reminderInstanceId;
  private String status;
  private String resultStatus;
  private Double confidence;
  private Boolean actionDetected;
  private String targetsJson;
  private Integer latencyMs;
  private String errorCode;
  private String errorMessage;
  private String traceId;
  private String startedAt;
  private String completedAt;

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

  public Long getReminderInstanceId() {
    return reminderInstanceId;
  }

  public void setReminderInstanceId(Long reminderInstanceId) {
    this.reminderInstanceId = reminderInstanceId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResultStatus() {
    return resultStatus;
  }

  public void setResultStatus(String resultStatus) {
    this.resultStatus = resultStatus;
  }

  public Double getConfidence() {
    return confidence;
  }

  public void setConfidence(Double confidence) {
    this.confidence = confidence;
  }

  public Boolean getActionDetected() {
    return actionDetected;
  }

  public void setActionDetected(Boolean actionDetected) {
    this.actionDetected = actionDetected;
  }

  public String getTargetsJson() {
    return targetsJson;
  }

  public void setTargetsJson(String targetsJson) {
    this.targetsJson = targetsJson;
  }

  public Integer getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(Integer latencyMs) {
    this.latencyMs = latencyMs;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(String completedAt) {
    this.completedAt = completedAt;
  }
}

package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;

/**
 * 检测任务实体。
 *
 * <p>用于承载前端上传视频后，由 Spring 编排并调用 Flask 执行的异步检测任务。</p>
 */
@TableName("detection_jobs")
public class DetectionJob {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long patientId;
  private Long scheduleId;
  private Long reminderInstanceId;
  private String inputType;
  private String sourceFilename;
  private String status;
  private String resultStatus;
  private Double confidence;
  private Boolean actionDetected;
  private String targetsJson;
  private Integer latencyMs;
  private String errorCode;
  private String errorMessage;
  private String traceId;
  private Timestamp startedAt;
  private Timestamp completedAt;
  private Timestamp createdAt;
  private Timestamp updatedAt;

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

  public String getInputType() {
    return inputType;
  }

  public void setInputType(String inputType) {
    this.inputType = inputType;
  }

  public String getSourceFilename() {
    return sourceFilename;
  }

  public void setSourceFilename(String sourceFilename) {
    this.sourceFilename = sourceFilename;
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

  public Timestamp getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Timestamp startedAt) {
    this.startedAt = startedAt;
  }

  public Timestamp getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Timestamp completedAt) {
    this.completedAt = completedAt;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
  }
}

package com.liyile.medication.dto;

/**
 * 创建检测任务的表单字段请求。
 */
public class CreateDetectionJobRequest {
  private Long patientId;
  private Long reminderInstanceId;
  private String cameraId;
  private String modelVersion;
  private Integer samplingRate;
  private Integer maxFrames;

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getReminderInstanceId() {
    return reminderInstanceId;
  }

  public void setReminderInstanceId(Long reminderInstanceId) {
    this.reminderInstanceId = reminderInstanceId;
  }

  public String getCameraId() {
    return cameraId;
  }

  public void setCameraId(String cameraId) {
    this.cameraId = cameraId;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  public Integer getSamplingRate() {
    return samplingRate;
  }

  public void setSamplingRate(Integer samplingRate) {
    this.samplingRate = samplingRate;
  }

  public Integer getMaxFrames() {
    return maxFrames;
  }

  public void setMaxFrames(Integer maxFrames) {
    this.maxFrames = maxFrames;
  }
}

package com.liyile.medication.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户设置实体。
 * <p>对应数据表 settings。</p>
 *
 * @author Liyile
 */
@TableName("settings")
public class Settings {
  /** 主键 ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户 ID */
  private Long userId;

  /** 提醒：启用语音 */
  private Boolean reminderEnableVoice;

  /** 提醒：提前分钟数 */
  private Integer reminderAdvanceMinutes;

  /** 提醒：音量（0-100） */
  private Integer reminderVolume;

  /** 检测：自动启动 */
  private Boolean detectionAutoStart;

  /** 检测：低光增强 */
  private Boolean detectionLowLightEnhance;

  /** 检测：回退模式（WebGPU/WebGL/WASM） */
  private String detectionFallbackMode;

  /** 隐私：摄像头权限 */
  private Boolean privacyCameraPermission;

  /** 隐私：上传同意 */
  private Boolean privacyUploadConsent;

  /** 隐私：分享给护工 */
  private Boolean privacyShareToCaregiver;

  /** 获取主键 ID。 */
  public Long getId() {
    return id;
  }

  /** 设置主键 ID。 */
  public void setId(Long id) {
    this.id = id;
  }

  /** 获取用户 ID。 */
  public Long getUserId() {
    return userId;
  }

  /** 设置用户 ID。 */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /** 获取提醒：启用语音。 */
  public Boolean getReminderEnableVoice() {
    return reminderEnableVoice;
  }

  /** 设置提醒：启用语音。 */
  public void setReminderEnableVoice(Boolean reminderEnableVoice) {
    this.reminderEnableVoice = reminderEnableVoice;
  }

  /** 获取提醒：提前分钟数。 */
  public Integer getReminderAdvanceMinutes() {
    return reminderAdvanceMinutes;
  }

  /** 设置提醒：提前分钟数。 */
  public void setReminderAdvanceMinutes(Integer reminderAdvanceMinutes) {
    this.reminderAdvanceMinutes = reminderAdvanceMinutes;
  }

  /** 获取提醒：音量。 */
  public Integer getReminderVolume() {
    return reminderVolume;
  }

  /** 设置提醒：音量。 */
  public void setReminderVolume(Integer reminderVolume) {
    this.reminderVolume = reminderVolume;
  }

  /** 获取检测：自动启动。 */
  public Boolean getDetectionAutoStart() {
    return detectionAutoStart;
  }

  /** 设置检测：自动启动。 */
  public void setDetectionAutoStart(Boolean detectionAutoStart) {
    this.detectionAutoStart = detectionAutoStart;
  }

  /** 获取检测：低光增强。 */
  public Boolean getDetectionLowLightEnhance() {
    return detectionLowLightEnhance;
  }

  /** 设置检测：低光增强。 */
  public void setDetectionLowLightEnhance(Boolean detectionLowLightEnhance) {
    this.detectionLowLightEnhance = detectionLowLightEnhance;
  }

  /** 获取检测：回退模式。 */
  public String getDetectionFallbackMode() {
    return detectionFallbackMode;
  }

  /** 设置检测：回退模式。 */
  public void setDetectionFallbackMode(String detectionFallbackMode) {
    this.detectionFallbackMode = detectionFallbackMode;
  }

  /** 获取隐私：摄像头权限。 */
  public Boolean getPrivacyCameraPermission() {
    return privacyCameraPermission;
  }

  /** 设置隐私：摄像头权限。 */
  public void setPrivacyCameraPermission(Boolean privacyCameraPermission) {
    this.privacyCameraPermission = privacyCameraPermission;
  }

  /** 获取隐私：上传同意。 */
  public Boolean getPrivacyUploadConsent() {
    return privacyUploadConsent;
  }

  /** 设置隐私：上传同意。 */
  public void setPrivacyUploadConsent(Boolean privacyUploadConsent) {
    this.privacyUploadConsent = privacyUploadConsent;
  }

  /** 获取隐私：分享给护工。 */
  public Boolean getPrivacyShareToCaregiver() {
    return privacyShareToCaregiver;
  }

  /** 设置隐私：分享给护工。 */
  public void setPrivacyShareToCaregiver(Boolean privacyShareToCaregiver) {
    this.privacyShareToCaregiver = privacyShareToCaregiver;
  }
}


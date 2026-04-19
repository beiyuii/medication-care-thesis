package com.liyile.medication.vo;

import com.liyile.medication.entity.Settings;

/**
 * 用户设置VO。
 * <p>用于前端展示和更新用户设置，包含提醒、检测、隐私三个模块的设置。</p>
 *
 * @author Liyile
 */
public class SettingsVO {
  /** 提醒设置 */
  private ReminderSettings reminder;

  /** 检测设置 */
  private DetectionSettings detection;

  /** 隐私设置 */
  private PrivacySettings privacy;

  /** 无参构造方法。 */
  public SettingsVO() {}

  /** 全参构造方法。 */
  public SettingsVO(ReminderSettings reminder, DetectionSettings detection, PrivacySettings privacy) {
    this.reminder = reminder;
    this.detection = detection;
    this.privacy = privacy;
  }

  /**
   * 从Settings实体转换为VO。
   *
   * @param settings 设置实体
   * @return SettingsVO对象
   */
  public static SettingsVO from(Settings settings) {
    if (settings == null) {
      return defaultSettings();
    }
    
    ReminderSettings reminder = new ReminderSettings(
        settings.getReminderEnableVoice(),
        settings.getReminderAdvanceMinutes(),
        settings.getReminderVolume());
    
    DetectionSettings detection = new DetectionSettings(
        settings.getDetectionAutoStart(),
        settings.getDetectionLowLightEnhance(),
        settings.getDetectionFallbackMode());
    
    Boolean cameraPerm = settings.getPrivacyCameraPermission();
    Boolean uploadConsent = settings.getPrivacyUploadConsent();
    Boolean shareCare = settings.getPrivacyShareToCaregiver();
    PrivacySettings privacy =
        new PrivacySettings(
            cameraPerm == null ? Boolean.TRUE : cameraPerm,
            uploadConsent == null ? Boolean.FALSE : uploadConsent,
            shareCare == null ? Boolean.FALSE : shareCare);
    
    return new SettingsVO(reminder, detection, privacy);
  }

  /**
   * 返回默认设置。
   *
   * @return 默认设置VO
   */
  public static SettingsVO defaultSettings() {
    return new SettingsVO(
        new ReminderSettings(true, 5, 80),
        new DetectionSettings(true, false, "WASM"),
        new PrivacySettings(true, false, false));
  }

  /** 获取提醒设置。 */
  public ReminderSettings getReminder() {
    return reminder;
  }

  /** 设置提醒设置。 */
  public void setReminder(ReminderSettings reminder) {
    this.reminder = reminder;
  }

  /** 获取检测设置。 */
  public DetectionSettings getDetection() {
    return detection;
  }

  /** 设置检测设置。 */
  public void setDetection(DetectionSettings detection) {
    this.detection = detection;
  }

  /** 获取隐私设置。 */
  public PrivacySettings getPrivacy() {
    return privacy;
  }

  /** 设置隐私设置。 */
  public void setPrivacy(PrivacySettings privacy) {
    this.privacy = privacy;
  }

  /**
   * 提醒设置内部类。
   */
  public static class ReminderSettings {
    /** 启用语音 */
    private Boolean enableVoice;

    /** 提前分钟数 */
    private Integer advanceMinutes;

    /** 音量（0-100） */
    private Integer volume;

    /** 无参构造方法。 */
    public ReminderSettings() {}

    /** 全参构造方法。 */
    public ReminderSettings(Boolean enableVoice, Integer advanceMinutes, Integer volume) {
      this.enableVoice = enableVoice;
      this.advanceMinutes = advanceMinutes;
      this.volume = volume;
    }

    /** 获取启用语音。 */
    public Boolean getEnableVoice() {
      return enableVoice;
    }

    /** 设置启用语音。 */
    public void setEnableVoice(Boolean enableVoice) {
      this.enableVoice = enableVoice;
    }

    /** 获取提前分钟数。 */
    public Integer getAdvanceMinutes() {
      return advanceMinutes;
    }

    /** 设置提前分钟数。 */
    public void setAdvanceMinutes(Integer advanceMinutes) {
      this.advanceMinutes = advanceMinutes;
    }

    /** 获取音量。 */
    public Integer getVolume() {
      return volume;
    }

    /** 设置音量。 */
    public void setVolume(Integer volume) {
      this.volume = volume;
    }
  }

  /**
   * 检测设置内部类。
   */
  public static class DetectionSettings {
    /** 自动启动 */
    private Boolean autoStart;

    /** 低光增强 */
    private Boolean lowLightEnhance;

    /** 回退模式 */
    private String fallbackMode;

    /** 无参构造方法。 */
    public DetectionSettings() {}

    /** 全参构造方法。 */
    public DetectionSettings(Boolean autoStart, Boolean lowLightEnhance, String fallbackMode) {
      this.autoStart = autoStart;
      this.lowLightEnhance = lowLightEnhance;
      this.fallbackMode = fallbackMode;
    }

    /** 获取自动启动。 */
    public Boolean getAutoStart() {
      return autoStart;
    }

    /** 设置自动启动。 */
    public void setAutoStart(Boolean autoStart) {
      this.autoStart = autoStart;
    }

    /** 获取低光增强。 */
    public Boolean getLowLightEnhance() {
      return lowLightEnhance;
    }

    /** 设置低光增强。 */
    public void setLowLightEnhance(Boolean lowLightEnhance) {
      this.lowLightEnhance = lowLightEnhance;
    }

    /** 获取回退模式。 */
    public String getFallbackMode() {
      return fallbackMode;
    }

    /** 设置回退模式。 */
    public void setFallbackMode(String fallbackMode) {
      this.fallbackMode = fallbackMode;
    }
  }

  /**
   * 隐私设置内部类。
   */
  public static class PrivacySettings {
    /** 摄像头权限 */
    private Boolean cameraPermission;

    /** 上传同意 */
    private Boolean uploadConsent;

    /** 分享给护工 */
    private Boolean shareToCaregiver;

    /** 无参构造方法。 */
    public PrivacySettings() {}

    /** 全参构造方法。 */
    public PrivacySettings(Boolean cameraPermission, Boolean uploadConsent, Boolean shareToCaregiver) {
      this.cameraPermission = cameraPermission;
      this.uploadConsent = uploadConsent;
      this.shareToCaregiver = shareToCaregiver;
    }

    /** 获取摄像头权限。 */
    public Boolean getCameraPermission() {
      return cameraPermission;
    }

    /** 设置摄像头权限。 */
    public void setCameraPermission(Boolean cameraPermission) {
      this.cameraPermission = cameraPermission;
    }

    /** 获取上传同意。 */
    public Boolean getUploadConsent() {
      return uploadConsent;
    }

    /** 设置上传同意。 */
    public void setUploadConsent(Boolean uploadConsent) {
      this.uploadConsent = uploadConsent;
    }

    /** 获取分享给护工。 */
    public Boolean getShareToCaregiver() {
      return shareToCaregiver;
    }

    /** 设置分享给护工。 */
    public void setShareToCaregiver(Boolean shareToCaregiver) {
      this.shareToCaregiver = shareToCaregiver;
    }
  }
}


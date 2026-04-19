/**
 * ReminderSetting 描述提醒相关配置。
 */
export interface ReminderSetting {
  /** enableVoice 是否开启语音。 */
  enableVoice: boolean
  /** advanceMinutes 提醒提前分钟数。 */
  advanceMinutes: number
  /** volume 语音音量（0-100）。 */
  volume: number
}

/**
 * DetectionSetting 描述检测模块配置。
 */
export interface DetectionSetting {
  /** autoStart 是否在时间窗自动开启检测。 */
  autoStart: boolean
  /** lowLightEnhance 是否开启低光增强。 */
  lowLightEnhance: boolean
  /** fallbackMode 检测回退模式。 */
  fallbackMode: 'webgpu' | 'webgl' | 'wasm'
}

/**
 * PrivacySetting 描述隐私与权限偏好。
 */
export interface PrivacySetting {
  /** cameraPermission 摄像头权限是否授予。 */
  cameraPermission: boolean
  /** uploadConsent 是否允许上传关键帧。 */
  uploadConsent: boolean
  /** shareToCaregiver 是否允许护工/子女查看日志。 */
  shareToCaregiver: boolean
}

/**
 * UserSetting 聚合所有配置。
 */
export interface UserSetting {
  /** reminder 提醒配置。 */
  reminder: ReminderSetting
  /** detection 检测配置。 */
  detection: DetectionSetting
  /** privacy 隐私配置。 */
  privacy: PrivacySetting
}

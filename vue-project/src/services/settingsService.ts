import http from '@/lib/http'
import type { UserSetting, DetectionSetting } from '@/types/settings'

/**
 * ApiUserSetting 后端返回的设置数据格式
 */
interface ApiUserSetting {
  reminder: {
    enableVoice: boolean
    advanceMinutes: number
    volume: number
  }
  detection: {
    autoStart: boolean
    lowLightEnhance: boolean
    fallbackMode: string // 后端可能返回大写格式（如 "WASM"）
  }
  privacy: {
    cameraPermission: boolean
    uploadConsent: boolean
    shareToCaregiver: boolean
  }
}

/**
 * normalizeFallbackMode 将后端返回的回退模式格式化为前端格式
 */
function normalizeFallbackMode(mode: string): DetectionSetting['fallbackMode'] {
  const normalized = mode.toLowerCase()
  if (normalized === 'webgpu' || normalized === 'webgl' || normalized === 'wasm') {
    return normalized as DetectionSetting['fallbackMode']
  }
  // 默认返回 wasm
  return 'wasm'
}

/**
 * mapApiSettingToLocal 将后端返回的设置数据映射为前端使用的格式
 */
function mapApiSettingToLocal(apiSetting: ApiUserSetting): UserSetting {
  return {
    reminder: {
      enableVoice: apiSetting.reminder.enableVoice,
      advanceMinutes: apiSetting.reminder.advanceMinutes,
      volume: apiSetting.reminder.volume,
    },
    detection: {
      autoStart: apiSetting.detection.autoStart,
      lowLightEnhance: apiSetting.detection.lowLightEnhance,
      fallbackMode: normalizeFallbackMode(apiSetting.detection.fallbackMode),
    },
    privacy: {
      cameraPermission: apiSetting.privacy.cameraPermission,
      uploadConsent: apiSetting.privacy.uploadConsent,
      shareToCaregiver: apiSetting.privacy.shareToCaregiver,
    },
  }
}

/**
 * mapLocalSettingToApi 将前端的设置数据映射为后端请求的格式
 */
function mapLocalSettingToApi(localSetting: UserSetting): ApiUserSetting {
  return {
    reminder: {
      enableVoice: localSetting.reminder.enableVoice,
      advanceMinutes: localSetting.reminder.advanceMinutes,
      volume: localSetting.reminder.volume,
    },
    detection: {
      autoStart: localSetting.detection.autoStart,
      lowLightEnhance: localSetting.detection.lowLightEnhance,
      fallbackMode: localSetting.detection.fallbackMode.toUpperCase(), // 转换为大写
    },
    privacy: {
      cameraPermission: localSetting.privacy.cameraPermission,
      uploadConsent: localSetting.privacy.uploadConsent,
      shareToCaregiver: localSetting.privacy.shareToCaregiver,
    },
  }
}

/**
 * fetchUserSettings 获取用户设置
 * @returns 返回用户设置对象
 */
export async function fetchUserSettings(): Promise<UserSetting> {
  const apiSetting: ApiUserSetting = await http.get('/settings')
  return mapApiSettingToLocal(apiSetting)
}

/**
 * updateUserSettings 更新用户设置
 * @param payload 需要更新的设置对象（部分更新）
 * @returns 返回更新后的完整设置对象
 */
export async function updateUserSettings(payload: UserSetting): Promise<UserSetting> {
  const apiPayload = mapLocalSettingToApi(payload)
  const apiSetting: ApiUserSetting = await http.put('/settings', apiPayload)
  return mapApiSettingToLocal(apiSetting)
}

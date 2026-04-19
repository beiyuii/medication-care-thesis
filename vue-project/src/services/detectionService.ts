import axios, { AxiosHeaders, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { appConfig } from '@/config/env'
import { useAuthStore } from '@/stores/auth'
import pinia from '@/stores'

export interface DetectionPredictPayload {
  patientId: string
  scheduleId: string
  timestamp: string
  frameB64?: string
  frameUrl?: string
  cameraId?: string
  modelVersion?: string
}

export interface DetectionPredictResult {
  status: 'suspected' | 'confirmed' | 'abnormal'
  confidence: number
  actionDetected: boolean
  targetDetected?: boolean // 是否检测到目标（从 targets 数组推导）
  targets: Array<{ label: string; score: number; bbox: [number, number, number, number] }>
  latencyMs: number
  traceId: string
  message?: string // 检测结果消息
}

/**
 * 视频上传响应接口
 */
export interface VideoUploadResponse {
  success: boolean
  filename: string
  path: string
  fullPath: string
  size: number
  traceId: string
}

/**
 * 视频检测请求载荷接口
 */
export interface VideoDetectionPayload {
  patientId: string
  scheduleId: string
  timestamp: string
  videoFile: Blob
  mimeType?: string
  cameraId?: string
  modelVersion?: string
  samplingRate?: number
  maxFrames?: number
}

/**
 * 视频检测目标接口
 */
export interface VideoDetectionTarget {
  label: 'PILL' | 'BLISTER' | 'BOTTLE' | 'BOX'
  score: number
  bbox: [number, number, number, number]
  firstDetectedFrame: number
  lastDetectedFrame: number
  detectionCount: number
}

/**
 * 动作时间线接口
 */
export interface ActionTimeline {
  startFrame: number
  endFrame: number
  confidence: number
}

/**
 * 视频元数据接口
 */
export interface VideoMetadata {
  duration: number
  fps: number
  totalFrames: number
  processedFrames: number
}

/**
 * 视频检测响应接口
 */
export interface VideoDetectionResult {
  status: 'suspected' | 'confirmed' | 'abnormal'
  confidence: number
  actionDetected: boolean
  targets: VideoDetectionTarget[]
  actionTimeline: ActionTimeline[]
  videoMetadata: VideoMetadata
  latencyMs: number
  traceId: string
}

/**
 * detectionHttp 为算法检测服务的独立 HTTP 实例
 * 算法服务接口不经过业务后端的 /api 前缀
 */
const detectionHttp: AxiosInstance = axios.create({
  baseURL: appConfig.detectionBaseUrl,
  timeout: 30000, // 检测接口可能需要更长的超时时间
})

// 视频上传和检测接口需要更长的超时时间（视频处理可能需要较长时间）
const videoDetectionHttp: AxiosInstance = axios.create({
  baseURL: appConfig.detectionBaseUrl,
  timeout: 120000, // 120秒超时，视频处理可能需要较长时间
})

// 添加请求拦截器，设置认证头
const setupAuthInterceptor = (httpInstance: AxiosInstance) => {
  httpInstance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore(pinia)
    if (authStore.token) {
      const headers = AxiosHeaders.from(config.headers)
      headers.set('Authorization', `Bearer ${authStore.token}`)
      config.headers = headers
      
      if (import.meta.env.DEV) {
        console.log('[Detection HTTP Request]', config.method?.toUpperCase(), config.url, 'Token:', authStore.token.substring(0, 20) + '...')
      }
    }
    return config
  })
}

setupAuthInterceptor(detectionHttp)
setupAuthInterceptor(videoDetectionHttp)

// 添加响应拦截器，处理算法服务的响应格式
const setupResponseInterceptor = (httpInstance: AxiosInstance) => {
  httpInstance.interceptors.response.use(
    (response) => {
      // 算法服务可能直接返回数据，也可能有包装格式
      // 如果响应有 data 字段且是对象，提取 data；否则直接返回 response.data
      if (response.data?.data && typeof response.data.data === 'object') {
        return response.data.data
      }
      return response.data
    },
    (error) => {
      // 处理错误响应
      const errorData = error.response?.data || {}
      const errorMsg =
        errorData.message ||
        errorData.msg ||
        errorData.error ||
        errorData.detail ||
        error.message ||
        '检测服务异常，请稍后重试'
      
      console.error('[Detection HTTP Error]', {
        url: error.config?.url,
        status: error.response?.status,
        message: errorMsg,
        data: errorData,
      })
      
      return Promise.reject({
        status: error.response?.status ?? 0,
        message: errorMsg,
        traceId: errorData.traceId,
        data: errorData,
      })
    },
  )
}

setupResponseInterceptor(detectionHttp)
setupResponseInterceptor(videoDetectionHttp)

export async function checkHealth() {
  return detectionHttp.get<unknown, unknown>('/health')
}

export async function checkReady() {
  return detectionHttp.get<unknown, unknown>('/ready')
}

export async function predictDetection(payload: DetectionPredictPayload) {
  return detectionHttp.post<DetectionPredictResult, DetectionPredictResult, DetectionPredictPayload>(
    '/v1/detections/predict',
    payload,
  )
}

/**
 * 上传视频到 Flask 的 video 目录
 * @param payload 上传参数
 * @returns 上传响应
 */
export async function uploadVideo(payload: {
  videoFile: Blob
  patientId?: string
  scheduleId?: string
  customFilename?: string
  mimeType?: string
}): Promise<VideoUploadResponse> {
  const formData = new FormData()
  
  // 根据 MIME 类型确定文件扩展名
  let filename = 'recording.mp4' // 默认 mp4
  if (payload.mimeType) {
    if (payload.mimeType.includes('mp4')) {
      filename = 'recording.mp4'
    } else if (payload.mimeType.includes('webm')) {
      filename = 'recording.webm'
    } else {
      // 根据 Blob 类型推断
      filename = payload.videoFile.type.includes('mp4') ? 'recording.mp4' : 'recording.webm'
    }
  } else if (payload.videoFile.type) {
    // 如果没有提供 mimeType，从 Blob 类型推断
    filename = payload.videoFile.type.includes('mp4') ? 'recording.mp4' : 'recording.webm'
  }
  
  formData.append('videoFile', payload.videoFile, filename)

  if (payload.patientId) {
    formData.append('patientId', payload.patientId)
  }
  if (payload.scheduleId) {
    formData.append('scheduleId', payload.scheduleId)
  }
  if (payload.customFilename) {
    formData.append('customFilename', payload.customFilename)
  }

  // 注意：不要手动设置 Content-Type，让浏览器自动设置（包含 boundary）
  return videoDetectionHttp.post<VideoUploadResponse, VideoUploadResponse, FormData>(
    '/v1/videos/upload',
    formData,
  )
}

/**
 * 视频检测接口
 * @param payload 视频检测请求载荷
 * @returns 视频检测结果
 */
export async function predictVideoDetection(
  payload: VideoDetectionPayload,
): Promise<VideoDetectionResult> {
  const formData = new FormData()
  formData.append('patientId', payload.patientId)
  formData.append('scheduleId', payload.scheduleId)
  formData.append('timestamp', payload.timestamp)
  
  // 根据 MIME 类型确定文件扩展名
  let filename = 'recording.mp4' // 默认 mp4
  if (payload.mimeType) {
    if (payload.mimeType.includes('mp4')) {
      filename = 'recording.mp4'
    } else if (payload.mimeType.includes('webm')) {
      filename = 'recording.webm'
    } else {
      // 根据 Blob 类型推断
      filename = payload.videoFile.type.includes('mp4') ? 'recording.mp4' : 'recording.webm'
    }
  } else if (payload.videoFile.type) {
    // 如果没有提供 mimeType，从 Blob 类型推断
    filename = payload.videoFile.type.includes('mp4') ? 'recording.mp4' : 'recording.webm'
  }
  
  formData.append('videoFile', payload.videoFile, filename)

  if (payload.cameraId) {
    formData.append('cameraId', payload.cameraId)
  }
  if (payload.modelVersion) {
    formData.append('modelVersion', payload.modelVersion)
  }
  if (payload.samplingRate !== undefined) {
    formData.append('samplingRate', String(payload.samplingRate))
  }
  if (payload.maxFrames !== undefined) {
    formData.append('maxFrames', String(payload.maxFrames))
  }

  // 注意：不要手动设置 Content-Type，让浏览器自动设置（包含 boundary）
  return videoDetectionHttp.post<VideoDetectionResult, VideoDetectionResult, FormData>(
    '/v1/detections/video/predict',
    formData,
  )
}

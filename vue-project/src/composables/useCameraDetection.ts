import { nextTick, onUnmounted, ref, type Ref } from 'vue'
import { predictDetection, type DetectionPredictResult } from '@/services/detectionService'
import type { PrivacySetting } from '@/types/settings'

/**
 * 摄像头错误类型枚举
 */
const CAMERA_ERROR_TYPE = {
  /** 权限被拒绝 */
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  /** 设备未找到 */
  NOT_FOUND: 'NOT_FOUND',
  /** 约束条件无法满足 */
  OVERCONSTRAINED: 'OVERCONSTRAINED',
  /** 其他错误 */
  UNKNOWN: 'UNKNOWN',
} as const

type CameraErrorType = (typeof CAMERA_ERROR_TYPE)[keyof typeof CAMERA_ERROR_TYPE]

/**
 * 获取错误类型
 * @param error 错误对象
 * @returns 错误类型
 */
function getCameraErrorType(error: unknown): CameraErrorType {
  if (error instanceof DOMException) {
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      return CAMERA_ERROR_TYPE.PERMISSION_DENIED
    }
    if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
      return CAMERA_ERROR_TYPE.NOT_FOUND
    }
    if (error.name === 'OverconstrainedError' || error.name === 'ConstraintNotSatisfiedError') {
      return CAMERA_ERROR_TYPE.OVERCONSTRAINED
    }
  }
  return CAMERA_ERROR_TYPE.UNKNOWN
}

/**
 * 获取错误提示信息
 * @param errorType 错误类型
 * @returns 错误提示信息
 */
function getErrorMessage(errorType: CameraErrorType): string {
  switch (errorType) {
    case CAMERA_ERROR_TYPE.PERMISSION_DENIED:
      return '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
    case CAMERA_ERROR_TYPE.NOT_FOUND:
      return '未检测到可用的摄像头设备，请检查设备连接'
    case CAMERA_ERROR_TYPE.OVERCONSTRAINED:
      return '摄像头不支持请求的分辨率，将尝试使用默认设置'
    default:
      return '摄像头启动失败，请检查设备是否正常工作'
  }
}

/**
 * 检查摄像头权限状态
 * @returns 权限状态信息
 */
async function checkCameraPermission(): Promise<{ state: PermissionState | null; supported: boolean }> {
  if (!navigator.permissions || !navigator.permissions.query) {
    return { state: null, supported: false }
  }
  try {
    const result = await navigator.permissions.query({ name: 'camera' as PermissionName })
    return { state: result.state, supported: true }
  } catch {
    return { state: null, supported: false }
  }
}

/**
 * useCameraDetection 管理摄像头权限、视频流与占位推理逻辑。
 * @param userPrivacySetting 用户隐私设置的响应式引用，用于检查摄像头权限配置
 */
export function useCameraDetection(userPrivacySetting?: Ref<PrivacySetting | null>) {
  const videoElement = ref<HTMLVideoElement | null>(null)
  const canvasElement = ref<HTMLCanvasElement | null>(null)
  const mediaStream = ref<MediaStream | null>(null)
  const isStreaming = ref(false)
  const permissionGranted = ref<boolean | null>(null)
  const errorMessage = ref<string | null>(null)
  const detectionResult = ref<DetectionPredictResult | null>(null)
  const isDetecting = ref(false)

  // 视频录制相关状态
  const mediaRecorder = ref<MediaRecorder | null>(null)
  const isRecording = ref(false)
  const recordingDuration = ref(0)
  const videoBlob = ref<Blob | null>(null)
  const recordingChunks = ref<Blob[]>([])
  const recordingDurationInterval = ref<ReturnType<typeof setInterval> | null>(null)
  const MAX_RECORDING_DURATION = 60000 // 最大录制时长60秒
  const recordingMimeType = ref<string>('') // 记录当前录制的 MIME 类型

  /**
   * 启动摄像头
   */
  const startCamera = async () => {
    if (isStreaming.value) return
    errorMessage.value = null

    // 首先检查用户设置中的摄像头权限（响应式读取最新值）
    const privacySetting = userPrivacySetting?.value
    if (privacySetting && privacySetting.cameraPermission === false) {
      permissionGranted.value = false
      errorMessage.value = '摄像头权限未开启，请在设置中心开启摄像头权限'
      return
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      permissionGranted.value = false
      errorMessage.value = '当前浏览器不支持摄像头 API，请使用 HTTPS 或本机地址访问'
      return
    }

    // 检查浏览器权限状态
    const permissionStatus = await checkCameraPermission()
    if (permissionStatus.supported && permissionStatus.state === 'denied') {
      permissionGranted.value = false
      errorMessage.value = '摄像头权限已被拒绝，请在浏览器设置中重新授权'
      return
    }

    try {
      // 先尝试请求高分辨率
      try {
        mediaStream.value = await navigator.mediaDevices.getUserMedia({
          video: { width: 1280, height: 720 },
          audio: false,
        })
      } catch (highResError) {
        const errorType = getCameraErrorType(highResError)
        // 如果是约束错误，尝试使用默认设置
        if (errorType === CAMERA_ERROR_TYPE.OVERCONSTRAINED) {
          mediaStream.value = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false,
          })
        } else {
          throw highResError
        }
      }

      const stream = mediaStream.value
      if (!stream) {
        throw new Error('未获取到媒体流')
      }

      /**
       * attachStreamToVideo 将 MediaStream 绑定到 video 元素并尝试播放。
       *
       * @returns 是否绑定并播放成功
       */
      const attachStreamToVideo = async (): Promise<boolean> => {
        const el = videoElement.value
        if (!el) {
          return false
        }
        el.srcObject = stream
        try {
          await el.play()
        } catch (playErr) {
          console.error('视频画面播放失败:', playErr)
          return false
        }
        return true
      }

      await nextTick()
      let attached = await attachStreamToVideo()
      if (!attached) {
        await nextTick()
        attached = await attachStreamToVideo()
      }
      if (!attached) {
        stream.getTracks().forEach(track => track.stop())
        mediaStream.value = null
        permissionGranted.value = false
        isStreaming.value = false
        errorMessage.value = '无法显示摄像头画面，请刷新页面后重试'
        return
      }

      permissionGranted.value = true
      isStreaming.value = true
    } catch (error) {
      console.error('摄像头启动失败:', error)
      const errorType = getCameraErrorType(error)
      permissionGranted.value = false
      isStreaming.value = false
      errorMessage.value = getErrorMessage(errorType)
    }
  }

  const stopCamera = () => {
    // 如果正在录制，先停止录制
    if (isRecording.value) {
      stopRecording()
    }
    mediaStream.value?.getTracks().forEach(track => track.stop())
    mediaStream.value = null
    isStreaming.value = false
  }

  /**
   * 开始录制视频
   */
  const startRecording = async (): Promise<boolean> => {
    if (!mediaStream.value || isRecording.value) {
      return false
    }

    try {
      // 清空之前的录制数据
      recordingChunks.value = []
      videoBlob.value = null
      recordingDuration.value = 0
      recordingMimeType.value = ''

      // 获取支持的 MIME 类型（优先选择 mp4 格式）
      const mimeTypes = [
        'video/mp4;codecs=avc1.42E01E', // H.264 Baseline Profile
        'video/mp4;codecs=avc1.4D001E', // H.264 Main Profile
        'video/mp4;codecs=avc1.640028', // H.264 High Profile
        'video/mp4', // 通用 mp4
        'video/webm;codecs=vp8', // 降级选项
        'video/webm;codecs=vp9',
        'video/webm',
      ]

      let selectedMimeType = ''
      for (const mimeType of mimeTypes) {
        if (MediaRecorder.isTypeSupported(mimeType)) {
          selectedMimeType = mimeType
          break
        }
      }

      if (!selectedMimeType) {
        errorMessage.value = '当前浏览器不支持视频录制'
        return false
      }

      // 记录选中的 MIME 类型
      recordingMimeType.value = selectedMimeType

      // 创建 MediaRecorder
      const recorder = new MediaRecorder(mediaStream.value, {
        mimeType: selectedMimeType,
        videoBitsPerSecond: 2500000, // 2.5Mbps，中等质量
      })

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          recordingChunks.value.push(event.data)
        }
      }

      recorder.onstop = () => {
        // 合并所有数据块
        if (recordingChunks.value.length > 0) {
          videoBlob.value = new Blob(recordingChunks.value, {
            type: selectedMimeType,
          })
          console.log('录制完成，视频大小:', videoBlob.value.size, 'bytes')
        }
        // 清理定时器
        if (recordingDurationInterval.value) {
          clearInterval(recordingDurationInterval.value)
          recordingDurationInterval.value = null
        }
      }

      recorder.onerror = (event) => {
        console.error('录制错误:', event)
        errorMessage.value = '录制过程中发生错误'
        stopRecording()
      }

      mediaRecorder.value = recorder
      recorder.start(1000) // 每秒收集一次数据
      isRecording.value = true

      // 开始计时
      const startTime = Date.now()
      recordingDurationInterval.value = setInterval(() => {
        const elapsed = Date.now() - startTime
        recordingDuration.value = Math.floor(elapsed / 1000) // 秒

        // 检查是否超过最大录制时长
        if (elapsed >= MAX_RECORDING_DURATION) {
          stopRecording()
        }
      }, 1000)

      console.log('开始录制视频，MIME类型:', selectedMimeType)
      return true
    } catch (error) {
      console.error('开始录制失败:', error)
      errorMessage.value = '开始录制失败，请重试'
      return false
    }
  }

  /**
   * 停止录制视频
   * @returns Promise<Blob | null> 返回视频 Blob，如果失败则返回 null
   */
  const stopRecording = async (): Promise<Blob | null> => {
    if (!isRecording.value || !mediaRecorder.value) {
      return null
    }

    try {
      // 停止录制
      if (mediaRecorder.value.state === 'recording') {
        mediaRecorder.value.stop()
      }

      // 清理定时器
      if (recordingDurationInterval.value) {
        clearInterval(recordingDurationInterval.value)
        recordingDurationInterval.value = null
      }

      isRecording.value = false
      mediaRecorder.value = null

      // 等待视频 Blob 生成（MediaRecorder 的 onstop 事件是异步的）
      return new Promise((resolve) => {
        // 设置超时，最多等待 3 秒
        const timeout = setTimeout(() => {
          console.warn('等待视频 Blob 生成超时')
          resolve(videoBlob.value)
        }, 3000)

        // 如果已经有 videoBlob，立即返回
        if (videoBlob.value) {
          clearTimeout(timeout)
          resolve(videoBlob.value)
          return
        }

        // 监听 recorder 的 stop 事件
        const checkInterval = setInterval(() => {
          if (videoBlob.value) {
            clearTimeout(timeout)
            clearInterval(checkInterval)
            resolve(videoBlob.value)
          }
        }, 100)
      })
    } catch (error) {
      console.error('停止录制失败:', error)
      errorMessage.value = '停止录制失败'
      isRecording.value = false
      return null
    }
  }

  /**
   * 从视频 Blob 中提取关键帧
   * @param videoBlob 视频 Blob 对象
   * @param timeOffset 时间偏移（秒），默认提取中间帧
   * @returns Promise<Blob | null> 返回关键帧图片 Blob，失败返回 null
   */
  const extractKeyFrameFromVideo = async (
    videoBlob: Blob,
    timeOffset?: number,
  ): Promise<Blob | null> => {
    return new Promise((resolve) => {
      const video = document.createElement('video')
      const canvas = document.createElement('canvas')
      const ctx = canvas.getContext('2d')

      if (!ctx) {
        console.error('无法获取 Canvas 上下文')
        resolve(null)
        return
      }

      let videoUrl: string | null = null
      let timeoutId: ReturnType<typeof setTimeout> | null = null

      // 设置超时（10秒）
      timeoutId = setTimeout(() => {
        console.error('提取关键帧超时')
        if (videoUrl) {
          URL.revokeObjectURL(videoUrl)
        }
        resolve(null)
      }, 10000)

      const cleanup = () => {
        if (timeoutId) {
          clearTimeout(timeoutId)
          timeoutId = null
        }
        if (videoUrl) {
          URL.revokeObjectURL(videoUrl)
          videoUrl = null
        }
        video.src = ''
      }

      video.preload = 'metadata'
      video.muted = true
      video.playsInline = true

      video.onloadedmetadata = () => {
        try {
          // 确定提取的时间点
          const duration = video.duration
          if (!duration || !isFinite(duration)) {
            console.error('视频时长无效')
            cleanup()
            resolve(null)
            return
          }

          const targetTime = timeOffset !== undefined ? timeOffset : duration / 2
          video.currentTime = Math.min(Math.max(0, targetTime), duration - 0.1)
        } catch (error) {
          console.error('设置视频时间失败:', error)
          cleanup()
          resolve(null)
        }
      }

      video.onseeked = () => {
        try {
          // 设置画布尺寸
          if (video.videoWidth === 0 || video.videoHeight === 0) {
            console.error('视频尺寸无效')
            cleanup()
            resolve(null)
            return
          }

          canvas.width = video.videoWidth
          canvas.height = video.videoHeight

          // 绘制视频帧到画布
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

          // 转换为 Blob
          canvas.toBlob(
            (blob) => {
              if (timeoutId) {
                clearTimeout(timeoutId)
                timeoutId = null
              }
              if (videoUrl) {
                URL.revokeObjectURL(videoUrl)
                videoUrl = null
              }
              resolve(blob)
            },
            'image/jpeg',
            0.85,
          )
        } catch (error) {
          console.error('提取关键帧失败:', error)
          cleanup()
          resolve(null)
        }
      }

      video.onerror = (error) => {
        console.error('视频加载失败:', error)
        cleanup()
        resolve(null)
      }

      try {
        // 创建视频 URL
        videoUrl = URL.createObjectURL(videoBlob)
        video.src = videoUrl
      } catch (error) {
        console.error('创建视频 URL 失败:', error)
        cleanup()
        resolve(null)
      }
    })
  }

  const captureFrame = async () => {
    if (!videoElement.value || !canvasElement.value) return null
    const canvas = canvasElement.value
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    canvas.width = videoElement.value.videoWidth
    canvas.height = videoElement.value.videoHeight
    ctx.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
    return createImageBitmap(canvas)
  }

  const frameToBase64 = async (frame: ImageBitmap) => {
    const canvas = document.createElement('canvas')
    canvas.width = frame.width
    canvas.height = frame.height
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    ctx.drawImage(frame, 0, 0)
    return canvas.toDataURL('image/jpeg').split(',')[1]
  }

  const captureFrameAsBlob = async (): Promise<Blob | null> => {
    if (!videoElement.value || !canvasElement.value) return null
    const canvas = canvasElement.value
    const ctx = canvas.getContext('2d')
    if (!ctx) return null
    canvas.width = videoElement.value.videoWidth
    canvas.height = videoElement.value.videoHeight
    ctx.drawImage(videoElement.value, 0, 0, canvas.width, canvas.height)
    
    return new Promise((resolve) => {
      canvas.toBlob((blob) => resolve(blob), 'image/jpeg', 0.85)
    })
  }

  /**
   * 运行检测
   * @param patientId 患者ID
   * @param scheduleId 计划ID
   * @param onProgress 进度回调函数，用于显示检测状态
   */
  const runDetection = async (
    patientId: string,
    scheduleId: string,
    onProgress?: (message: string) => void,
  ) => {
    if (!isStreaming.value || isDetecting.value) return
    isDetecting.value = true
    onProgress?.('正在截取画面...')
    
    const frame = await captureFrame()
    if (!frame) {
      isDetecting.value = false
      onProgress?.('截取画面失败')
      return
    }
    
    try {
      onProgress?.('正在编码图片...')
      const frameB64 = await frameToBase64(frame)
      if (!frameB64) {
        errorMessage.value = '截帧失败'
        onProgress?.('图片编码失败')
        isDetecting.value = false
        return
      }
      
      onProgress?.('正在调用检测接口...')
      console.log('开始检测:', { patientId, scheduleId, timestamp: new Date().toISOString() })
      
      const result = await predictDetection({
        patientId,
        scheduleId,
        timestamp: new Date().toISOString(),
        frameB64,
        cameraId: 'web-cam',
        modelVersion: 'web-yolo-v1',
      })
      
      // 添加 targetDetected 字段，基于 targets 数组判断
      const resultWithTargetDetected = {
        ...result,
        targetDetected: result.targets && result.targets.length > 0,
        message: result.status === 'confirmed' 
          ? '已检测到药品和服药动作' 
          : result.status === 'suspected'
            ? '疑似检测到药品或动作'
            : '未检测到',
      }
      
      detectionResult.value = resultWithTargetDetected
      
      console.log('检测完成:', {
        status: result.status,
        targetDetected: resultWithTargetDetected.targetDetected,
        actionDetected: result.actionDetected,
        targetsCount: result.targets?.length ?? 0,
        confidence: result.confidence,
        traceId: result.traceId,
      })
      
      onProgress?.(
        resultWithTargetDetected.targetDetected && result.actionDetected
          ? '检测完成：已确认'
          : resultWithTargetDetected.targetDetected || result.actionDetected
            ? '检测完成：疑似'
            : '检测完成：未检测到',
      )
    } catch (error) {
      console.error('检测失败:', error)
      const errorMsg = (error as { message?: string }).message ?? '检测服务暂不可用'
      errorMessage.value = errorMsg
      onProgress?.(`检测失败: ${errorMsg}`)
    } finally {
      frame.close()
      isDetecting.value = false
    }
  }

  /**
   * 获取权限引导信息
   * @returns 权限引导信息
   */
  const getPermissionGuide = (): string => {
    // 如果用户设置中权限未开启，提示去设置中心（响应式读取最新值）
    const privacySetting = userPrivacySetting?.value
    if (privacySetting && privacySetting.cameraPermission === false) {
      return '请在设置中心开启摄像头权限'
    }
    
    const userAgent = navigator.userAgent.toLowerCase()
    if (userAgent.includes('chrome')) {
      return 'Chrome: 点击地址栏左侧的锁图标 → 摄像头 → 允许'
    }
    if (userAgent.includes('firefox')) {
      return 'Firefox: 点击地址栏左侧的锁图标 → 权限 → 摄像头 → 允许'
    }
    if (userAgent.includes('safari') && !userAgent.includes('chrome')) {
      return 'Safari: Safari → 偏好设置 → 网站 → 摄像头 → 允许'
    }
    if (userAgent.includes('edge')) {
      return 'Edge: 点击地址栏左侧的锁图标 → 摄像头 → 允许'
    }
    return '请在浏览器设置中允许此网站访问摄像头'
  }

  onUnmounted(() => {
    // 清理录制定时器
    if (recordingDurationInterval.value) {
      clearInterval(recordingDurationInterval.value)
      recordingDurationInterval.value = null
    }
    // 停止录制
    if (isRecording.value) {
      stopRecording()
    }
    stopCamera()
  })

  return {
    videoElement,
    canvasElement,
    isStreaming,
    permissionGranted,
    errorMessage,
    detectionResult,
    isDetecting,
    startCamera,
    stopCamera,
    runDetection,
    captureFrameAsBlob,
    getPermissionGuide,
    // 视频录制相关
    isRecording,
    recordingDuration,
    videoBlob,
    recordingMimeType,
    startRecording,
    stopRecording,
    extractKeyFrameFromVideo,
  }
}

import http from '@/lib/http'
import { appConfig } from '@/config/env'

/**
 * LogImage 日志图片数据格式
 */
export interface LogImage {
  /** id 图片ID */
  id: number
  /** eventId 关联的事件ID */
  eventId: number
  /** url 图片访问URL */
  url: string
  /** ts 上传时间戳 */
  ts: string
}

/**
 * UploadImageResponse 上传图片的响应格式
 */
interface UploadImageResponse {
  url: string
}

/**
 * uploadLogImage 上传日志图片
 * @param file 图片文件（File 对象）
 * @param eventId 关联的服药事件ID
 * @returns 返回图片访问URL
 */
export async function uploadLogImage(file: File, eventId: number): Promise<string> {
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    throw new Error('不支持的图片格式，仅支持 JPG、PNG、WEBP')
  }

  // 验证文件大小（2MB = 2 * 1024 * 1024 bytes）
  const maxSize = 2 * 1024 * 1024
  if (file.size > maxSize) {
    throw new Error('图片大小不能超过 2MB')
  }

  // 创建 FormData
  const formData = new FormData()
  formData.append('file', file)
  formData.append('eventId', String(eventId))

  // 发送请求
  const response: UploadImageResponse = await http.post('/logs/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return response.url
}

/**
 * fetchLogImages 查询事件关联的图片列表
 * @param eventId 服药事件ID
 * @returns 返回图片列表
 */
export async function fetchLogImages(eventId: number): Promise<LogImage[]> {
  const images: LogImage[] = await http.get('/logs/images', {
    params: { eventId },
  })

  return images
}

/**
 * resolveImageUrl 解析图片完整URL
 * 如果 URL 是相对路径，则拼接基础URL
 * @param url 图片URL（可能是相对路径或完整URL）
 * @returns 返回完整的图片URL
 */
export function resolveImageUrl(url: string): string {
  if (!url) return ''
  
  // 如果已经是完整URL（以 http:// 或 https:// 开头），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // 如果是相对路径，拼接基础URL
  // 注意：这里需要根据实际的后端配置来拼接
  // 如果后端返回的是 /uploads/xxx.jpg，可能需要拼接服务器地址
  // 如果后端返回的是完整路径，则直接使用
  const baseUrl = appConfig.uploadBaseUrl.replace('/logs', '')
  return `${baseUrl}${url.startsWith('/') ? '' : '/'}${url}`
}


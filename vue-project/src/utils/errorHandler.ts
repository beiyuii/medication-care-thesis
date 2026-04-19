/**
 * ApiError 表示后端返回的错误对象
 */
export interface ApiError {
  /** status HTTP 状态码 */
  status?: number
  /** code 业务错误码 */
  code?: number
  /** message 错误消息 */
  message?: string
  /** data 错误数据 */
  data?: {
    message?: string
    msg?: string
    error?: string
    detail?: string
  }
  /** traceId 请求追踪 ID */
  traceId?: string
  /** timestamp 错误时间戳 */
  timestamp?: string
}

/**
 * extractErrorMessage 从错误对象中提取用户友好的错误消息
 * @param error 错误对象，可能是 ApiError、Error 或其他类型
 * @param defaultMessage 默认错误消息
 * @returns 提取的错误消息
 */
export function extractErrorMessage(error: unknown, defaultMessage = '操作失败，请稍后重试'): string {
  if (!error) {
    return defaultMessage
  }

  // 如果是 ApiError 类型
  const apiError = error as ApiError
  
  // 针对特定状态码提供更友好的提示
  if (apiError.status === 403) {
    // 403 权限错误，优先使用后端返回的消息
    return apiError.message || '您没有权限执行此操作，请联系管理员'
  }
  
  if (apiError.status === 401) {
    // 401 未授权错误，优先使用后端返回的消息（如"用户名或密码错误"）
    // 如果没有消息，则使用默认提示
    return apiError.message || '登录已过期，请重新登录'
  }
  
  if (apiError.status === 404) {
    return '请求的资源不存在'
  }
  
  if (apiError.status === 500) {
    if (apiError.message) {
      return apiError.traceId
        ? `${apiError.message}（traceId: ${apiError.traceId}）`
        : apiError.message
    }
    return '服务器内部错误，请稍后重试'
  }
  
  // 优先使用 error.message（已由拦截器处理）
  if (apiError.message) {
    return apiError.message
  }
  
  // 其次使用 error.data.message 或 error.data.msg
  if (apiError.data) {
    if (apiError.data.message) {
      return apiError.data.message
    }
    if (apiError.data.msg) {
      return apiError.data.msg
    }
    if (apiError.data.error) {
      return apiError.data.error
    }
    if (apiError.data.detail) {
      return apiError.data.detail
    }
  }
  
  // 如果是标准 Error 对象
  if (error instanceof Error) {
    return error.message || defaultMessage
  }
  
  // 如果是字符串
  if (typeof error === 'string') {
    return error
  }
  
  return defaultMessage
}

/**
 * logError 在开发环境下输出详细错误信息
 * @param error 错误对象
 * @param context 错误上下文描述
 */
export function logError(error: unknown, context = '请求失败'): void {
  if (import.meta.env.DEV) {
    const apiError = error as ApiError
    console.error(`${context}详情:`, {
      message: extractErrorMessage(error),
      status: apiError.status,
      code: apiError.code,
      traceId: apiError.traceId,
      timestamp: apiError.timestamp,
      originalError: error,
    })
  }
}

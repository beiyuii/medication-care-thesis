import axios, {
  AxiosHeaders,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import axiosRetry from 'axios-retry'
import { appConfig } from '@/config/env'
import { useAuthStore } from '@/stores/auth'
import pinia from '@/stores'
import { isAuthFailureStatus, normalizeHttpError } from './httpError'

/**
 * http 为全局 Axios 实例，集中处理鉴权头与错误提示。
 */
const http: AxiosInstance = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: 15000,
})

axiosRetry(http, {
  retries: 3,
  retryDelay: axiosRetry.exponentialDelay,
  shouldResetTimeout: true,
  retryCondition: error => {
    return axiosRetry.isNetworkError(error) || axiosRetry.isRetryableError(error)
  },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const authStore = useAuthStore(pinia)
  if (authStore.token) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${authStore.token}`)
    config.headers = headers
    
    if (import.meta.env.DEV) {
      console.log('[HTTP Request]', config.method?.toUpperCase(), config.url, 'Token:', authStore.token.substring(0, 20) + '...')
    }
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse) => {
    // 后端统一响应格式：{ code, msg, data, traceId, timestamp }
    // 成功响应：code === 200，提取 data 字段返回
    if (response.data?.code === 200) {
      return response.data.data
    }
    // 如果 code 不是 200，当作错误处理
    return Promise.reject(
      normalizeHttpError({
        response: {
          status: response.status,
          data: response.data,
        },
      }),
    )
  },
  error => {
    const authStore = useAuthStore(pinia)
    const requestUrl = error.config?.url || ''
    
    // 401 未授权，清除会话（clearSession 会自动跳转到登录页）
    // 但是登录/注册接口返回 401 时不应该清除会话（用户还没有登录）
    if (isAuthFailureStatus(error.response?.status)) {
      const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register')
      if (!isAuthEndpoint) {
        // 只有非登录/注册接口的 401 错误才清除会话
        authStore.clearSession()
      }
    }

    return Promise.reject(normalizeHttpError(error))
  },
)

export default http

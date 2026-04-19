import http from '@/lib/http'
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  UserProfileResponse,
} from '@/types/auth'

/**
 * login 用户登录接口
 * @param payload 登录请求参数，包含 username 和 password
 * @returns 返回登录响应数据，包含 token、role、userId、displayName
 */
export async function login(payload: LoginRequest): Promise<LoginResponse> {
  return http.post('/auth/login', payload)
}

/**
 * register 用户注册接口
 * @param payload 注册请求参数，包含 username、password、role
 * @returns 返回注册响应数据，包含 token、role、userId、displayName
 */
export async function register(payload: RegisterRequest): Promise<LoginResponse> {
  return http.post('/auth/register', payload)
}

/**
 * getProfile 获取当前登录用户信息
 * @returns 返回用户信息，包含 userId、username、role、name、patients
 */
export async function getProfile(): Promise<UserProfileResponse> {
  return http.get('/auth/profile')
}


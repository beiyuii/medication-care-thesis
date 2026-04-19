/**
 * LoginRequest 登录请求参数
 */
export interface LoginRequest {
  /** username 为登录账户名 */
  username: string
  /** password 为密码 */
  password: string
}

/**
 * RegisterRequest 注册请求参数
 */
export interface RegisterRequest {
  /** username 为注册账户名 */
  username: string
  /** password 为密码 */
  password: string
  /** role 为用户角色 */
  role: 'elder' | 'caregiver' | 'child'
}

/**
 * ApiResponse 后端统一响应格式
 */
export interface ApiResponse<T> {
  /** code 为状态码，200 表示成功 */
  code: number
  /** msg 为响应消息 */
  msg: string
  /** data 为业务数据 */
  data: T
  /** traceId 为请求链路 ID */
  traceId: string
  /** timestamp 为响应时间戳 */
  timestamp: string
}

/**
 * LoginResponse 登录响应数据
 */
export interface LoginResponse {
  /** token 为 JWT 令牌 */
  token: string
  /** role 为用户角色 */
  role: string
  /** userId 为用户 ID */
  userId: number
  /** displayName 为显示名称 */
  displayName: string
}

/**
 * PatientSummary 患者摘要信息
 */
export interface PatientSummary {
  /** id 为患者 ID */
  id: number
  /** name 为患者姓名 */
  name: string
  /** nextIntakeTime 为下次服药时间 */
  nextIntakeTime: string
  /** planStatus 为计划状态 */
  planStatus: string
  /** alertCount 为告警数量 */
  alertCount: number
}

/**
 * UserProfileResponse 用户信息响应数据
 */
export interface UserProfileResponse {
  /** userId 为用户 ID */
  userId: number
  /** username 为用户名 */
  username: string
  /** role 为角色 */
  role: string
  /** name 为姓名 */
  name: string
  /** patients 为关联的患者列表 */
  patients: PatientSummary[]
}


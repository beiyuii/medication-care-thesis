import http from '@/lib/http'

/**
 * PingResponse 联通性测试响应格式
 */
export interface PingResponse {
  timestamp: string
  version: string
}

/**
 * RolePermissionMatrix 角色权限矩阵
 */
export interface RolePermissionMatrix {
  role: string
  permissions: string[]
}

/**
 * ping 联通性测试
 * @returns 返回系统健康状态和版本信息
 */
export async function ping(): Promise<PingResponse> {
  return http.get('/status/ping')
}

/**
 * getRolePermissions 获取角色权限矩阵
 * @returns 返回当前角色的权限列表
 */
export async function getRolePermissions(): Promise<RolePermissionMatrix> {
  return http.get('/status/role')
}


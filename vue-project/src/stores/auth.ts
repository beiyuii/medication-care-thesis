import { defineStore } from 'pinia'
import { storage } from '@/utils/storage'

export type UserRole = 'elder' | 'caregiver' | 'child'

/**
 * UserProfile 表示当前登录用户的基础信息。
 */
export interface UserProfile {
  /** id 为用户唯一标识，供接口传参使用。 */
  id: string
  /** name 展示在界面顶部，帮助识别当前身份。 */
  name: string
  /** role 标记角色类型，控制权限。 */
  role: UserRole
}

/**
 * AuthState 持久化认证与角色状态。
 */
interface AuthState {
  /** token 保存后端签发的 JWT，用于接口鉴权。 */
  token: string | null
  /** user 保存完整的用户 Profile，便于展示。 */
  user: UserProfile | null
  /** role 表示当前激活角色，用于前端控制渲染与权限。 */
  role: UserRole | null
}

const ROLE_LABEL_MAP: Record<UserRole, string> = {
  elder: '老年人',
  caregiver: '护工',
  child: '子女',
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => {
    // 安全地解析用户信息
    const parseUser = (): UserProfile | null => {
      try {
        const userStr = storage.getItem('auth_user')
        return userStr ? JSON.parse(userStr) : null
      } catch (error) {
        console.warn('Failed to parse user from localStorage:', error)
        // 如果解析失败，清除无效数据
        storage.removeItem('auth_user')
        return null
      }
    }

    return {
      token: storage.getItem('auth_token') || null,
      user: parseUser(),
      role: (storage.getItem('auth_role') as UserRole) || null,
    }
  },
  getters: {
    isAuthenticated: state => Boolean(state.token),
    displayRole: state => (state.role ? ROLE_LABEL_MAP[state.role] : '访客'),
    displayName: state => state.user?.name ?? '访客',
  },
  actions: {
    /**
     * setSession 在登录成功后写入 token 与用户信息。
     */
    setSession(payload: { token: string; user: UserProfile }) {
      this.token = payload.token
      this.user = payload.user
      this.role = payload.user.role

      // 持久化到 localStorage（带异常处理）
      try {
        storage.setItem('auth_token', payload.token)
        storage.setItem('auth_user', JSON.stringify(payload.user))
        storage.setItem('auth_role', payload.user.role)
      } catch (error) {
        console.warn('Failed to persist session to localStorage:', error)
        // 即使持久化失败，内存中的状态仍然有效
      }
    },
    /**
     * clearSession 清除所有会话信息，常用于登出或 token 失效。
     */
    clearSession() {
      this.token = null
      this.user = null
      this.role = null

      // 清除 localStorage（带异常处理）
      try {
        storage.removeItem('auth_token')
        storage.removeItem('auth_user')
        storage.removeItem('auth_role')
      } catch (error) {
        console.warn('Failed to clear session from localStorage:', error)
      }

      // 清除数据后跳转到登录页
      if (typeof window !== 'undefined') {
        const currentPath = window.location.pathname
        // 如果当前不在登录页，则跳转到登录页
        if (currentPath !== '/login') {
          window.location.href = '/login'
        }
      }
    },
    /**
     * switchRole 允许在多角色之间切换，用于护工/子女等账号。
     */
    switchRole(role: UserRole) {
      this.role = role
      // 持久化角色到 localStorage
      try {
        storage.setItem('auth_role', role)
      } catch (error) {
        console.warn('Failed to persist role to localStorage:', error)
      }
    },
  },
})

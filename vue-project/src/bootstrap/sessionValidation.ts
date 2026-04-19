import type { UserProfileResponse } from '@/types/auth'
import type { UserRole } from '@/stores/auth'
import { isAuthFailureStatus } from '@/lib/httpError'

type SessionStore = {
  isAuthenticated: boolean
  token: string | null
  clearSession: () => void
  setSession: (payload: { token: string; user: { id: string; name: string; role: UserRole } }) => void
}

type GetProfileFn = () => Promise<UserProfileResponse>

/**
 * validateStoredSession 在应用启动时验证持久化 token，并避免将业务异常误判为登录失效。
 */
export async function validateStoredSession(
  authStore: SessionStore,
  getProfile: GetProfileFn,
): Promise<void> {
  if (!authStore.isAuthenticated || !authStore.token) {
    return
  }

  try {
    const profile = await getProfile()
    authStore.setSession({
      token: authStore.token,
      user: {
        id: String(profile.userId),
        name: profile.name,
        role: profile.role as UserRole,
      },
    })
  } catch (error) {
    const status = (error as { status?: number }).status
    if (isAuthFailureStatus(status)) {
      console.warn('Token validation failed, clearing session:', error)
      authStore.clearSession()
      return
    }
    console.warn('Profile loading failed but session is kept:', error)
  }
}

import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore, type UserRole } from '@/stores/auth'

const roleHomeMap: Record<UserRole, string> = {
  elder: '/elder/home',
  caregiver: '/caregiver/home',
  child: '/child/home',
}

/**
 * useAuthSession 提供身份判断与快捷跳转工具。
 */
export function useAuthSession() {
  const router = useRouter()
  const authStore = useAuthStore()

  const isElder = computed(() => authStore.role === 'elder')

  const resolveHomeRoute = (role?: UserRole) => roleHomeMap[role ?? authStore.role ?? 'elder']

  const goHome = (role?: UserRole) => router.push(resolveHomeRoute(role))

  return {
    authStore,
    isElder,
    resolveHomeRoute,
    goHome,
  }
}

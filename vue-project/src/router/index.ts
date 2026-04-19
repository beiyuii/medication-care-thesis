import { createRouter, createWebHistory } from 'vue-router'
import CoreLayout from '@/layouts/CoreLayout.vue'
import { useAuthStore, type UserRole } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    role?: 'elder' | 'caregiver' | 'child'
    publicOnly?: boolean
    allowRoles?: UserRole[]
  }
}
const roleHomeMap: Record<UserRole, string> = {
  elder: '/elder/home',
  caregiver: '/caregiver/home',
  child: '/child/home',
}

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0, left: 0 }
  },
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { publicOnly: true },
    },
    {
      path: '/',
      component: CoreLayout,
      children: [
        {
          path: '',
          redirect: '/elder/home',
        },
        {
          path: 'elder/home',
          name: 'elder-home',
          component: () => import('@/views/ElderDashboardView.vue'),
          meta: { requiresAuth: true, role: 'elder' },
        },
        {
          path: 'caregiver/home',
          name: 'caregiver-home',
          component: () => import('@/views/CaregiverDashboardView.vue'),
          meta: { requiresAuth: true, role: 'caregiver' },
        },
        {
          path: 'child/home',
          name: 'child-home',
          component: () => import('@/views/ChildDashboardView.vue'),
          meta: { requiresAuth: true, role: 'child' },
        },
        {
          path: 'plans',
          name: 'plan-board',
          component: () => import('@/views/PlanBoardView.vue'),
          meta: { requiresAuth: true, role: 'elder' },
        },
        {
          path: 'detection',
          name: 'detection-room',
          component: () => import('@/views/DetectionRoomView.vue'),
          meta: { requiresAuth: true, role: 'elder' },
        },
        {
          path: 'history',
          name: 'history-center',
          component: () => import('@/views/HistoryCenterView.vue'),
          meta: { requiresAuth: true, allowRoles: ['elder', 'caregiver', 'child'] },
        },
        {
          path: 'alerts',
          name: 'alert-center',
          component: () => import('@/views/AlertCenterView.vue'),
          meta: { requiresAuth: true, allowRoles: ['elder', 'caregiver', 'child'] },
        },
        {
          path: 'settings',
          name: 'settings-center',
          component: () => import('@/views/SettingsCenterView.vue'),
          meta: { requiresAuth: true, role: 'elder' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { publicOnly: true },
    },
  ],
})

router.beforeEach(to => {
  const authStore = useAuthStore()
  if (to.meta.publicOnly && authStore.isAuthenticated) {
    return roleHomeMap[authStore.role ?? 'elder']
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return '/login'
  }
  if (to.meta.role && authStore.role && to.meta.role !== authStore.role) {
    // 确保 role 存在，否则跳转到默认首页
    return roleHomeMap[authStore.role] ?? '/elder/home'
  }
  if (to.meta.allowRoles && authStore.role && !to.meta.allowRoles.includes(authStore.role)) {
    // 确保 role 存在，否则跳转到默认首页
    return roleHomeMap[authStore.role] ?? '/elder/home'
  }
  return true
})

export default router

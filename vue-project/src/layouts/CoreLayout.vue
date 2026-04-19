<script setup lang="ts">
import type { Component } from 'vue'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  AlertCircleOutline,
  CameraOutline,
  ClipboardOutline,
  GridOutline,
  HomeOutline,
  LogOutOutline,
  MenuOutline,
  SettingsOutline,
  SparklesOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import { useAuthStore, type UserRole } from '@/stores/auth'

interface NavItem {
  label: string
  path: string
  icon: Component
}

interface NavSection {
  label: string
  items: NavItem[]
}

const SIDER_WIDTH = 280

const navConfig: Record<UserRole, NavItem[]> = {
  elder: [
    { label: '首页', path: '/elder/home', icon: HomeOutline },
    { label: '用药计划', path: '/plans', icon: ClipboardOutline },
    { label: '摄像头检测', path: '/detection', icon: CameraOutline },
    { label: '历史与统计', path: '/history', icon: StatsChartOutline },
    { label: '异常与告警', path: '/alerts', icon: AlertCircleOutline },
    { label: '设置', path: '/settings', icon: SettingsOutline },
  ],
  caregiver: [
    { label: '首页', path: '/caregiver/home', icon: HomeOutline },
    { label: '历史与统计', path: '/history', icon: StatsChartOutline },
    { label: '异常与告警', path: '/alerts', icon: AlertCircleOutline },
  ],
  child: [
    { label: '首页', path: '/child/home', icon: HomeOutline },
    { label: '历史与统计', path: '/history', icon: StatsChartOutline },
    { label: '异常与告警', path: '/alerts', icon: AlertCircleOutline },
  ],
}

const homeRouteMap: Record<UserRole, string> = {
  elder: '/elder/home',
  caregiver: '/caregiver/home',
  child: '/child/home',
}

const pageMetaMap: Record<string, { eyebrow: string; title: string; description: string }> = {
  'elder-home': {
    eyebrow: 'Elder workspace',
    title: '今日提醒工作台',
    description: '围绕下一次提醒、今日计划和异常反馈完成整个服药流程。',
  },
  'caregiver-home': {
    eyebrow: 'Caregiver workspace',
    title: '照护概览工作台',
    description: '快速查看患者今日状态、异常任务和最近服药记录。',
  },
  'child-home': {
    eyebrow: 'Family workspace',
    title: '家人关怀概览',
    description: '用更轻的视图持续关注家人的提醒执行与异常情况。',
  },
  'plan-board': {
    eyebrow: 'Plan management',
    title: '用药计划管理',
    description: '维护药品、剂量和时间窗，让每日提醒实例按计划稳定生成。',
  },
  'detection-room': {
    eyebrow: 'Detection workflow',
    title: '服药检测流程',
    description: '在单任务页面内完成摄像头采集、结果等待和人工确认。',
  },
  'history-center': {
    eyebrow: 'History & reports',
    title: '历史与统计',
    description: '追溯提醒实例、服药事件和整体完成情况。',
  },
  'alert-center': {
    eyebrow: 'Alert center',
    title: '异常与告警',
    description: '查看已落库的异常告警，并追踪对应提醒实例与处理状态。',
  },
  'settings-center': {
    eyebrow: 'Settings',
    title: '权限与设置',
    description: '管理摄像头权限、隐私选项和系统偏好。',
  },
}

const roleSummaryMap: Record<UserRole, { label: string; accent: string; detail: string }> = {
  elder: {
    label: '老人端',
    accent: 'bg-primary/12 text-primary ring-primary/15',
    detail: '今天的提醒、检测和确认会聚合在一个工作台内。',
  },
  caregiver: {
    label: '护工端',
    accent: 'bg-info/12 text-info ring-info/15',
    detail: '以患者概览和异常任务为主，强调跨患者信息密度。',
  },
  child: {
    label: '子女端',
    accent: 'bg-accent/16 text-amber-700 ring-accent/20',
    detail: '以一对一关怀视图为主，优先显示提醒与异常状态。',
  },
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isMobile = ref(false)
const showMobileNav = ref(false)
const headerOffset = computed(() => (isMobile.value ? 76 : 88))

watch(
  () => authStore.isAuthenticated,
  isAuthenticated => {
    if (!isAuthenticated && route.path !== '/login') {
      router.replace('/login')
    }
  },
  { immediate: true },
)

const updateMobileState = () => {
  isMobile.value = window.matchMedia('(max-width: 1024px)').matches
  if (!isMobile.value) {
    showMobileNav.value = false
  }
}

onMounted(() => {
  updateMobileState()
  window.addEventListener('resize', updateMobileState)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateMobileState)
})

const isActive = (path: string) =>
  path === '/elder/home' || path === '/caregiver/home' || path === '/child/home'
    ? route.path === path
    : route.path.startsWith(path)

const userBadge = computed(() => {
  if (!authStore.isAuthenticated || !authStore.user) {
    return ''
  }
  return `${authStore.displayRole} · ${authStore.displayName}`
})

const roleSummary = computed(() => roleSummaryMap[authStore.role ?? 'elder'])

const navSections = computed<NavSection[]>(() => {
  const role = authStore.role ?? 'elder'
  const items = navConfig[role] ?? navConfig.elder
  if (role === 'elder') {
    return [
      { label: '今日执行', items: items.slice(0, 3) },
      { label: '记录与设置', items: items.slice(3) },
    ]
  }
  return [
    { label: '工作台', items: items.slice(0, 1) },
    { label: '记录中心', items: items.slice(1) },
  ]
})

const dropdownOptions = [
  { label: '个人信息', key: 'profile' },
  { label: '重新登录', key: 'relogin' },
  { label: '退出登录', key: 'logout' },
]

const currentPageMeta = computed(() => {
  const routeName = String(route.name ?? '')
  return (
    pageMetaMap[routeName] ?? {
      eyebrow: 'Workspace',
      title: '健康辅助系统',
      description: '保持计划、检测与记录之间的链路清晰稳定。',
    }
  )
})

const todayLabel = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(new Date()),
)

const handleAccountAction = async (key: string | number) => {
  if (key === 'logout' || key === 'relogin') {
    authStore.clearSession()
    await router.push('/login')
    return
  }
  if (key === 'profile') {
    await router.push('/settings')
    return
  }
  const role = authStore.role ?? 'elder'
  await router.push(homeRouteMap[role])
}

const toggleMobileNav = () => {
  if (isMobile.value) {
    showMobileNav.value = !showMobileNav.value
  }
}

const handleNavClick = () => {
  if (isMobile.value) {
    showMobileNav.value = false
  }
}
</script>

<template>
  <div class="min-h-[100dvh] bg-paper-glow text-text">
    <header
      class="fixed inset-x-0 top-0 z-40 border-b border-white/70 bg-[#fbf8f1]/82 backdrop-blur-xl"
    >
      <div
        class="mx-auto flex h-[76px] max-w-[1600px] items-center justify-between gap-4 px-4 md:h-[88px] md:px-6"
      >
        <div class="flex items-center gap-4">
          <NButton text class="lg:hidden" @click="toggleMobileNav">
            <NIcon :component="MenuOutline" size="22" />
          </NButton>
          <RouterLink to="/" class="flex items-center gap-3">
            <div class="flex h-12 w-12 items-center justify-center rounded-[18px] bg-primary text-white shadow-soft">
              <NIcon :component="SparklesOutline" size="22" />
            </div>
            <div class="hidden md:block">
              <p class="text-sm font-semibold uppercase tracking-[0.22em] text-primary/70">Medication care</p>
              <h1 class="text-lg font-semibold text-text">用药提醒与管理</h1>
            </div>
          </RouterLink>
        </div>

        <div class="hidden items-center gap-3 lg:flex">
          <div class="rounded-pill border border-white/80 bg-white/75 px-4 py-2 text-sm text-muted shadow-card">
            {{ todayLabel }}
          </div>
          <div class="rounded-pill px-4 py-2 text-sm font-semibold ring-1 ring-inset" :class="roleSummary.accent">
            {{ roleSummary.label }}
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="hidden rounded-pill border border-white/80 bg-white/75 px-4 py-2 text-sm text-muted shadow-card md:block">
            {{ userBadge }}
          </div>
          <NDropdown trigger="click" :options="dropdownOptions" @select="handleAccountAction">
            <NButton quaternary class="rounded-pill border border-white/70 bg-white/80 px-4">
              <template #icon>
                <NIcon :component="LogOutOutline" />
              </template>
              账户
            </NButton>
          </NDropdown>
        </div>
      </div>
    </header>

    <div class="flex" :style="{ paddingTop: `${headerOffset}px` }">
      <aside
        class="fixed bottom-0 left-0 z-20 hidden overflow-hidden border-r border-white/70 bg-[#faf6ee]/85 lg:block"
        :style="{ top: `${headerOffset}px`, width: `${SIDER_WIDTH}px` }"
      >
        <div class="flex h-full flex-col px-5 py-6">
          <div class="rounded-[28px] border border-white/80 bg-white/70 p-5 shadow-card">
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary/12 text-primary">
                <NIcon :component="GridOutline" size="18" />
              </div>
              <div>
                <p class="text-sm font-semibold text-text">{{ roleSummary.label }}</p>
                <p class="text-xs leading-5 text-muted">{{ roleSummary.detail }}</p>
              </div>
            </div>
          </div>

          <nav class="surface-scroll mt-6 flex-1 space-y-6 overflow-y-auto pr-1">
            <section v-for="section in navSections" :key="section.label" class="space-y-2">
              <p class="px-3 text-xs font-semibold uppercase tracking-[0.2em] text-muted/80">
                {{ section.label }}
              </p>
              <RouterLink
                v-for="item in section.items"
                :key="item.path"
                :to="item.path"
                class="group flex items-center gap-3 rounded-[22px] px-4 py-3 transition-all duration-200"
                :class="
                  isActive(item.path)
                    ? 'bg-primary text-white shadow-soft'
                    : 'border border-transparent text-text hover:border-white/80 hover:bg-white/80 hover:shadow-card'
                "
              >
                <div
                  class="flex h-10 w-10 items-center justify-center rounded-2xl transition"
                  :class="isActive(item.path) ? 'bg-white/18 text-white' : 'bg-primary/8 text-primary'"
                >
                  <NIcon size="18" :component="item.icon" />
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-semibold">{{ item.label }}</p>
                  <p class="text-xs opacity-75">
                    {{ isActive(item.path) ? '当前页面' : '进入查看' }}
                  </p>
                </div>
              </RouterLink>
            </section>
          </nav>
        </div>
      </aside>

      <transition name="fade">
        <div
          v-if="showMobileNav && isMobile"
          class="fixed inset-0 z-50 bg-black/28 lg:hidden"
          @click.self="toggleMobileNav"
        >
          <div class="surface-scroll h-full w-[82%] max-w-[320px] overflow-y-auto bg-[#faf6ee] p-5 shadow-float">
            <div class="rounded-[28px] border border-white/80 bg-white/80 p-5 shadow-card">
              <p class="text-sm font-semibold text-text">{{ roleSummary.label }}</p>
              <p class="mt-1 text-sm leading-6 text-muted">{{ roleSummary.detail }}</p>
            </div>
            <nav class="mt-6 space-y-6">
              <section v-for="section in navSections" :key="section.label" class="space-y-2">
                <p class="px-3 text-xs font-semibold uppercase tracking-[0.2em] text-muted/80">
                  {{ section.label }}
                </p>
                <RouterLink
                  v-for="item in section.items"
                  :key="item.path"
                  :to="item.path"
                  class="flex items-center gap-3 rounded-[22px] px-4 py-3 transition-all"
                  :class="
                    isActive(item.path)
                      ? 'bg-primary text-white shadow-soft'
                      : 'bg-white/75 text-text shadow-card'
                  "
                  @click="handleNavClick"
                >
                  <div
                    class="flex h-10 w-10 items-center justify-center rounded-2xl"
                    :class="isActive(item.path) ? 'bg-white/18 text-white' : 'bg-primary/8 text-primary'"
                  >
                    <NIcon size="18" :component="item.icon" />
                  </div>
                  <span class="text-sm font-semibold">{{ item.label }}</span>
                </RouterLink>
              </section>
            </nav>
          </div>
        </div>
      </transition>

      <main
        class="min-h-[calc(100dvh-76px)] min-w-0 flex-1 px-4 pb-8 pt-5 md:min-h-[calc(100dvh-88px)] md:px-6 md:pb-10 md:pt-8"
        :style="{ marginLeft: isMobile ? '0px' : `${SIDER_WIDTH}px` }"
      >
        <div class="mx-auto max-w-[1380px] space-y-5 md:space-y-6">
          <section
            class="overflow-hidden rounded-[28px] border border-white/80 bg-white/72 px-5 py-4 shadow-card backdrop-blur md:rounded-[30px] md:px-8 md:py-5"
          >
            <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div class="space-y-2">
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-primary/70">
                  {{ currentPageMeta.eyebrow }}
                </p>
                <h2 class="text-2xl font-semibold tracking-tight text-text md:text-3xl">
                  {{ currentPageMeta.title }}
                </h2>
                <p class="max-w-3xl text-sm leading-7 text-muted md:text-base">
                  {{ currentPageMeta.description }}
                </p>
              </div>
              <div class="flex flex-wrap gap-3">
                <div class="rounded-[20px] border border-white/85 bg-[#f8f5ee] px-4 py-3 shadow-card">
                  <p class="text-xs font-semibold uppercase tracking-[0.16em] text-muted/80">当前角色</p>
                  <p class="mt-1 text-sm font-semibold text-text">{{ roleSummary.label }}</p>
                </div>
                <div class="rounded-[20px] border border-white/85 bg-[#f8f5ee] px-4 py-3 shadow-card">
                  <p class="text-xs font-semibold uppercase tracking-[0.16em] text-muted/80">工作提示</p>
                  <p class="mt-1 text-sm font-semibold text-text">聚焦一个主任务，减少操作分叉</p>
                </div>
              </div>
            </div>
          </section>

          <RouterView v-slot="{ Component }">
            <transition name="page-swap" mode="out-in">
              <component :is="Component" />
            </transition>
          </RouterView>
        </div>
      </main>
    </div>
  </div>
</template>

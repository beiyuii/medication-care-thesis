<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  AlertCircleOutline,
  CheckmarkCircleOutline,
  MedicalOutline,
  NotificationsOutline,
  SparklesOutline,
  TimeOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import StatusBar from '@/components/ui/StatusBar.vue'
import {
  fetchElderDashboard,
  type DashboardAlert,
  type ReminderInstanceItem,
} from '@/services/dashboardService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'

interface TodayPlanCard {
  id: string
  medicine: string
  dosage: string
  windowTime: string
  status: 'pending' | 'confirmed' | 'abnormal'
}

const router = useRouter()
const message = useMessage()

const todayPlans = ref<TodayPlanCard[]>([])
const loading = ref(false)
const nextReminder = ref<{ time: string; medicine: string; windowEnd: string } | null>(null)
const unresolvedAlerts = ref<DashboardAlert[]>([])
const completionRate = ref(0)

const formatClock = (value?: string | null): string => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatDosage = (instance: ReminderInstanceItem): string => {
  if (instance.dose && instance.frequency) {
    return `${instance.dose} · ${instance.frequency}`
  }
  return instance.dose || instance.frequency || '--'
}

const mapPlanStatus = (
  status: ReminderInstanceItem['status'],
): 'pending' | 'confirmed' | 'abnormal' => {
  if (status === 'confirmed' || status === 'resolved') {
    return 'confirmed'
  }
  if (status === 'abnormal' || status === 'missed') {
    return 'abnormal'
  }
  return 'pending'
}

const loadTodayPlans = async () => {
  loading.value = true
  try {
    const dashboard = await fetchElderDashboard()
    unresolvedAlerts.value = dashboard.activeAlerts.filter(alert => alert.status !== 'resolved')
    completionRate.value = dashboard.completionRate
    todayPlans.value = dashboard.todayInstances
      .map(instance => ({
        id: String(instance.id),
        medicine: instance.medicineName,
        dosage: formatDosage(instance),
        windowTime: `${formatClock(instance.windowStartAt)} - ${formatClock(instance.windowEndAt)}`,
        status: mapPlanStatus(instance.status),
      }))
      .sort((a, b) => {
        const timeA = a.windowTime.split(' - ')[0] || '00:00'
        const timeB = b.windowTime.split(' - ')[0] || '00:00'
        return timeA.localeCompare(timeB)
      })

    nextReminder.value = dashboard.nextReminder
      ? {
          time: formatClock(dashboard.nextReminder.windowStartAt),
          medicine: dashboard.nextReminder.medicineName,
          windowEnd: formatClock(dashboard.nextReminder.windowEndAt),
        }
      : null
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载首页聚合数据失败')
    message.error(errorMsg)
    logError(error, '加载 elder dashboard')
  } finally {
    loading.value = false
  }
}

const shouldShowStatusBar = computed(
  () =>
    unresolvedAlerts.value.length > 0 ||
    todayPlans.value.some(plan => plan.status === 'abnormal'),
)

const latestAlert = computed(() => {
  if (unresolvedAlerts.value.length === 0) return null
  return [...unresolvedAlerts.value].sort((a, b) => {
    const timeA = new Date(a.ts).getTime()
    const timeB = new Date(b.ts).getTime()
    return timeB - timeA
  })[0]
})

const confirmedCount = computed(
  () => todayPlans.value.filter(plan => plan.status === 'confirmed').length,
)

const abnormalCount = computed(
  () => todayPlans.value.filter(plan => plan.status === 'abnormal').length,
)

const pendingCount = computed(
  () => todayPlans.value.filter(plan => plan.status === 'pending').length,
)

const heroDescription = computed(() => {
  if (!nextReminder.value) {
    return '当前没有待执行的提醒实例，你可以先查看计划或回顾历史记录。'
  }
  return `下一次提醒聚焦在 ${nextReminder.value.medicine}，建议在 ${nextReminder.value.windowEnd} 前完成检测并确认。`
})

const latestAlertTone = computed(() => {
  if (!latestAlert.value) return 'info'
  if (latestAlert.value.severity === 'high') return 'error'
  if (latestAlert.value.severity === 'medium') return 'warning'
  return 'info'
})

const handleStartDetection = () => {
  router.push('/detection')
}

const toStatusTone = (status: TodayPlanCard['status']) => {
  if (status === 'confirmed') return 'success'
  if (status === 'abnormal') return 'danger'
  return 'warning'
}

const toStatusLabel = (status: TodayPlanCard['status']) => {
  if (status === 'confirmed') return '已确认'
  if (status === 'abnormal') return '异常'
  return '待执行'
}

onMounted(loadTodayPlans)
onActivated(loadTodayPlans)
</script>

<template>
  <div class="space-y-6">
    <StatusBar
      v-if="shouldShowStatusBar && latestAlert"
      :type="latestAlertTone as 'info' | 'success' | 'warning' | 'error'"
      :title="latestAlert.title"
      message="请优先处理当前异常，避免影响今日提醒节奏。"
    >
      <template #action>
        <RouterLink
          to="/alerts"
          class="text-sm font-semibold text-danger hover:text-danger/80"
        >
          查看异常
        </RouterLink>
      </template>
    </StatusBar>

    <PageHero
      eyebrow="Today focus"
      :title="nextReminder ? `${nextReminder.time} · ${nextReminder.medicine}` : '今天的提醒已经清空'"
      :description="heroDescription"
      tone="brand"
    >
      <template #meta>
        <StatusBadge
          :label="nextReminder ? `请在 ${nextReminder.windowEnd} 前完成` : '当前无待执行提醒'"
          :tone="nextReminder ? 'info' : 'neutral'"
        />
        <StatusBadge :label="`今日完成度 ${completionRate}%`" tone="success" />
        <StatusBadge :label="`异常 ${unresolvedAlerts.length} 条`" :tone="unresolvedAlerts.length > 0 ? 'danger' : 'neutral'" />
      </template>
      <template #actions>
        <NButton type="primary" size="large" @click="handleStartDetection">
          进入检测流程
        </NButton>
        <RouterLink to="/plans">
          <NButton size="large" tertiary>查看用药计划</NButton>
        </RouterLink>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="今日提醒总数"
        :value="todayPlans.length"
        helper="所有提醒实例都按时间顺序汇总在这里。"
        tone="neutral"
        :icon="NotificationsOutline"
      />
      <MetricCard
        label="已确认服药"
        :value="confirmedCount"
        helper="已完成人工确认或系统确认的提醒实例。"
        tone="success"
        :icon="CheckmarkCircleOutline"
      />
      <MetricCard
        label="待处理提醒"
        :value="abnormalCount > 0 ? abnormalCount : pendingCount"
        :helper="abnormalCount > 0 ? '优先处理异常提醒，避免形成连续漏服。' : '待执行提醒会按照时间窗逐步进入检测流程。'"
        :tone="abnormalCount > 0 ? 'danger' : 'warning'"
        :icon="AlertCircleOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.45fr_0.95fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Today timeline
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">今日提醒时间线</h3>
            <p class="mt-2 text-sm leading-7 text-muted">
              每次提醒以时间窗、药品名称和当前状态呈现，尽量让你一眼看到下一步要做什么。
            </p>
          </div>
          <RouterLink to="/history">
            <NButton quaternary>查看历史统计</NButton>
          </RouterLink>
        </div>

        <div class="mt-6 space-y-4">
          <NSkeleton v-if="loading" text :repeat="5" />
          <NEmpty v-else-if="todayPlans.length === 0" description="今日暂无提醒实例" />
          <article
            v-else
            v-for="plan in todayPlans"
            :key="plan.id"
            class="group rounded-[26px] border border-line/70 bg-[#fffcf6] p-5 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/25 hover:shadow-card"
          >
            <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div class="flex items-start gap-4">
                <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                  <NIcon :component="TimeOutline" size="20" />
                </div>
                <div class="space-y-2">
                  <div class="flex flex-wrap items-center gap-3">
                    <h4 class="text-xl font-semibold text-text">{{ plan.medicine }}</h4>
                    <StatusBadge :label="toStatusLabel(plan.status)" :tone="toStatusTone(plan.status)" />
                  </div>
                  <p class="text-sm text-muted">{{ plan.dosage }}</p>
                  <p class="text-sm text-muted">时间窗：{{ plan.windowTime }}</p>
                </div>
              </div>
              <div class="flex shrink-0 flex-wrap gap-3">
                <RouterLink v-if="plan.status === 'pending'" to="/detection">
                  <NButton type="primary">开始检测</NButton>
                </RouterLink>
                <RouterLink v-else-if="plan.status === 'abnormal'" to="/alerts">
                  <NButton type="error">处理异常</NButton>
                </RouterLink>
                <RouterLink v-else to="/history">
                  <NButton tertiary>查看记录</NButton>
                </RouterLink>
              </div>
            </div>
          </article>
        </div>
      </NCard>

      <div class="space-y-6">
        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center gap-3">
            <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <NIcon :component="SparklesOutline" size="22" />
            </div>
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Detection guide
              </p>
              <h3 class="text-xl font-semibold text-text">开始检测前</h3>
            </div>
          </div>
          <ul class="mt-5 space-y-3 text-sm leading-7 text-muted">
            <li>保持桌面整洁，把药品、手部和面部留在同一画面内。</li>
            <li>进入检测页后只保留一个主要动作：开始录制并等待结果。</li>
            <li>如果系统给出疑似结果，直接在结果页完成人工确认即可。</li>
          </ul>
        </NCard>

        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Active alerts
              </p>
              <h3 class="mt-2 text-xl font-semibold text-text">当前异常</h3>
            </div>
            <RouterLink to="/alerts">
              <NButton quaternary>全部查看</NButton>
            </RouterLink>
          </div>

          <div class="mt-5 space-y-3">
            <NEmpty v-if="unresolvedAlerts.length === 0" description="当前没有未处理异常" />
            <article
              v-for="alert in unresolvedAlerts.slice(0, 3)"
              v-else
              :key="alert.id"
              class="rounded-[22px] border border-danger/15 bg-danger/5 p-4"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="space-y-2">
                  <div class="flex items-center gap-3">
                    <NIcon :component="MedicalOutline" size="18" class="text-danger" />
                    <p class="font-semibold text-text">{{ alert.title }}</p>
                  </div>
                  <p class="text-sm leading-6 text-muted">
                    {{ alert.description || '请进入告警中心查看详情并处理。' }}
                  </p>
                </div>
                <StatusBadge
                  :label="alert.severity === 'high' ? '高优先级' : alert.severity === 'medium' ? '中优先级' : '提示'"
                  :tone="alert.severity === 'high' ? 'danger' : alert.severity === 'medium' ? 'warning' : 'info'"
                />
              </div>
            </article>
          </div>
        </NCard>
      </div>
    </section>
  </div>
</template>

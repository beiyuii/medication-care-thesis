<script setup lang="ts">
import { computed, onMounted, ref, watchEffect } from 'vue'
import { RouterLink } from 'vue-router'
import type { SelectOption } from 'naive-ui'
import { useMessage } from 'naive-ui'
import {
  AlertCircleOutline,
  CalendarOutline,
  CallOutline,
  CheckmarkCircleOutline,
  MedicalOutline,
  NotificationsOutline,
  PersonOutline,
} from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { usePatientList } from '@/composables/usePatientList'
import { fetchCaregiverDashboard, type DashboardAlert } from '@/services/dashboardService'
import { getPatientDetail, type PatientDetail } from '@/services/patientService'
import type { HistoryEvent } from '@/types/history'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import { resolveDashboardCompletionRate } from './dashboardCompletion'

const { patients, activePatient, activePatientId, hydratePatients } = usePatientList()
const message = useMessage()

const intakeEvents = ref<HistoryEvent[]>([])
const activeAlerts = ref<DashboardAlert[]>([])
const isLoadingEvents = ref(false)
const currentLoadingPatientId = ref<string | number | null>(null)
const completionRate = ref(0)

const loadIntakeEvents = async () => {
  if (!activePatient.value?.id) {
    intakeEvents.value = []
    activeAlerts.value = []
    currentLoadingPatientId.value = null
    return
  }

  const patientId =
    typeof activePatient.value.id === 'string'
      ? parseInt(activePatient.value.id, 10)
      : Number(activePatient.value.id)

  if (!patientId || Number.isNaN(patientId) || patientId <= 0) {
    intakeEvents.value = []
    activeAlerts.value = []
    currentLoadingPatientId.value = null
    return
  }

  if (currentLoadingPatientId.value === patientId && isLoadingEvents.value) {
    return
  }

  currentLoadingPatientId.value = patientId
  isLoadingEvents.value = true
  try {
    const dashboard = await fetchCaregiverDashboard(patientId)
    if (currentLoadingPatientId.value === patientId) {
      intakeEvents.value = dashboard.recentEvents
      activeAlerts.value = dashboard.activeAlerts
      completionRate.value = resolveDashboardCompletionRate(
        dashboard.completionRate,
        dashboard.recentEvents,
      )
    }
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载服药记录失败')
    message.error(errorMsg)
    logError(error, '加载 caregiver dashboard')
    if (currentLoadingPatientId.value === patientId) {
      intakeEvents.value = []
      activeAlerts.value = []
      completionRate.value = 0
    }
  } finally {
    if (currentLoadingPatientId.value === patientId) {
      isLoadingEvents.value = false
    }
  }
}

onMounted(async () => {
  await hydratePatients()
  await loadIntakeEvents()
})

watchEffect(() => {
  if (activePatient.value?.id) {
    loadIntakeEvents()
  } else {
    intakeEvents.value = []
    activeAlerts.value = []
  }
})

const patientOptions = computed<SelectOption[]>(() =>
  patients.value.map(patient => ({ label: patient.name, value: patient.id })),
)

const formatIntakeTime = (timeStr: string | undefined): string => {
  if (!timeStr) return '--:--'
  try {
    const date = new Date(timeStr)
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  } catch {
    return timeStr
  }
}

const formatDateTime = (dateStr: string): string => {
  try {
    return new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return dateStr
  }
}

const planStatusText = computed(() => {
  if (!activePatient.value) return '未选择'
  if (activePatient.value.planStatus === 'on_track' || activePatient.value.planStatus === 'active') {
    return '进行中'
  }
  if (activePatient.value.planStatus === 'delayed') {
    return '稍有延迟'
  }
  return '已暂停'
})

const showDetailModal = ref(false)
const patientDetail = ref<PatientDetail | null>(null)
const isLoadingDetail = ref(false)

const handleViewDetail = async () => {
  if (!activePatient.value) {
    message.warning('请先选择患者')
    return
  }

  const rawId = activePatient.value.id
  const patientId = typeof rawId === 'string' ? parseInt(rawId, 10) : Number(rawId)
  if (!patientId || Number.isNaN(patientId) || patientId <= 0) {
    message.error('患者ID无效，请刷新页面重试')
    return
  }

  showDetailModal.value = true
  isLoadingDetail.value = true
  patientDetail.value = null

  try {
    patientDetail.value = await getPatientDetail(patientId)
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载患者详情失败')
    message.error(errorMsg)
    logError(error, '加载患者详情')
    showDetailModal.value = false
  } finally {
    isLoadingDetail.value = false
  }
}

const heroTitle = computed(() =>
  activePatient.value ? `${activePatient.value.name} 的今日状态` : '选择一位被照护人开始查看',
)

const heroDescription = computed(() => {
  if (!activePatient.value) {
    return '选择患者后，系统会把提醒、异常和最近事件聚合到同一视图内。'
  }
  const nextTime = formatIntakeTime(activePatient.value.nextIntakeTime)
  return `下一次提醒预计在 ${nextTime}，当前未处理异常 ${activePatient.value.alertCount ?? 0} 条。`
})

const latestAlerts = computed(() => activeAlerts.value.slice(0, 3))

const getTimelineTone = (status: string) => {
  if (status === 'confirmed') return 'success'
  if (status === 'suspected') return 'warning'
  if (status === 'abnormal') return 'danger'
  return 'neutral'
}

const getStatusText = (status: string) => {
  if (status === 'confirmed') return '已确认'
  if (status === 'suspected') return '待确认'
  if (status === 'abnormal') return '异常'
  return '未知'
}
</script>

<template>
  <div class="space-y-6">
    <PageHero eyebrow="Care workflow" :title="heroTitle" :description="heroDescription" tone="soft">
      <template #meta>
        <StatusBadge
          :label="activePatient ? `当前患者：${activePatient.name}` : '尚未选择患者'"
          :tone="activePatient ? 'info' : 'neutral'"
        />
        <StatusBadge
          :label="activePatient ? `计划状态：${planStatusText}` : '等待选择患者'"
          :tone="activePatient?.planStatus === 'delayed' ? 'warning' : activePatient ? 'success' : 'neutral'"
        />
      </template>
      <template #actions>
        <div class="min-w-[260px]">
          <NSelect
            v-model:value="activePatientId"
            :options="patientOptions"
            placeholder="请选择患者"
            size="large"
            :disabled="patients.length === 0"
          />
        </div>
        <NButton type="primary" size="large" :disabled="!activePatient" @click="handleViewDetail">
          查看患者详情
        </NButton>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="下一次提醒"
        :value="formatIntakeTime(activePatient?.nextIntakeTime)"
        helper="把接下来的提醒时间固定在首屏，方便快速判断是否临近。"
        tone="brand"
        :icon="NotificationsOutline"
      />
      <MetricCard
        label="今日完成率"
        :value="`${completionRate}%`"
        helper="基于当日提醒实例的真实确认率计算，不再用最近事件数量近似。"
        tone="success"
        :icon="CheckmarkCircleOutline"
      />
      <MetricCard
        label="未处理异常"
        :value="activePatient?.alertCount ?? 0"
        :helper="(activePatient?.alertCount ?? 0) > 0 ? '建议优先查看异常详情，避免提醒持续滞后。' : '当前暂无异常任务。'"
        :tone="(activePatient?.alertCount ?? 0) > 0 ? 'danger' : 'neutral'"
        :icon="AlertCircleOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Recent records
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">最近服药记录</h3>
          </div>
          <RouterLink to="/history">
            <NButton quaternary>查看全部</NButton>
          </RouterLink>
        </div>

        <div class="mt-5 space-y-3">
          <NSkeleton v-if="isLoadingEvents" text :repeat="4" />
          <NEmpty v-else-if="intakeEvents.length === 0" description="暂无服药记录" />
          <article
            v-else
            v-for="event in intakeEvents"
            :key="event.id"
            class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-4"
          >
            <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div class="space-y-2">
                <div class="flex flex-wrap items-center gap-3">
                  <p class="text-lg font-semibold text-text">{{ event.medicineName }}</p>
                  <StatusBadge :label="getStatusText(event.status)" :tone="getTimelineTone(event.status)" />
                </div>
                <p class="text-sm leading-6 text-muted">{{ event.action }}</p>
              </div>
              <p class="text-sm text-muted">{{ event.timestamp }}</p>
            </div>
          </article>
        </div>
      </NCard>

      <div class="space-y-6">
        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Alert tasks
              </p>
              <h3 class="mt-2 text-2xl font-semibold text-text">异常任务</h3>
            </div>
            <RouterLink to="/alerts">
              <NButton quaternary>前往告警中心</NButton>
            </RouterLink>
          </div>

          <div class="mt-5 space-y-3">
            <NEmpty v-if="latestAlerts.length === 0" description="暂无异常任务" />
            <article
              v-for="alert in latestAlerts"
              v-else
              :key="alert.id"
              class="rounded-[24px] border border-danger/15 bg-danger/5 p-4"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="space-y-2">
                  <p class="font-semibold text-text">{{ alert.title }}</p>
                  <p class="text-sm leading-6 text-muted">
                    {{ alert.description || '请查看异常详情并决定是否需要人工跟进。' }}
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

        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
            Patient snapshot
          </p>
          <h3 class="mt-2 text-2xl font-semibold text-text">患者摘要</h3>
          <div class="mt-5 space-y-4 rounded-[24px] border border-line/70 bg-[#fffcf6] p-5">
            <div class="flex items-start gap-3">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <NIcon :component="PersonOutline" size="20" />
              </div>
              <div>
                <p class="text-sm text-muted">当前患者</p>
                <p class="text-lg font-semibold text-text">
                  {{ activePatient?.name ?? '未选择患者' }}
                </p>
              </div>
            </div>
            <div class="grid gap-3 md:grid-cols-2">
              <div class="rounded-[20px] bg-white p-4 shadow-card">
                <p class="text-sm text-muted">下一次提醒</p>
                <p class="mt-2 text-lg font-semibold text-text">
                  {{ formatIntakeTime(activePatient?.nextIntakeTime) }}
                </p>
              </div>
              <div class="rounded-[20px] bg-white p-4 shadow-card">
                <p class="text-sm text-muted">计划状态</p>
                <p class="mt-2 text-lg font-semibold text-text">{{ planStatusText }}</p>
              </div>
            </div>
          </div>
        </NCard>
      </div>
    </section>

    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="患者详情"
      style="width: 860px; max-width: 92vw"
      :bordered="false"
      size="large"
    >
      <div v-if="isLoadingDetail" class="flex items-center justify-center py-12">
        <NSpin size="large" />
      </div>
      <div v-else-if="patientDetail" class="space-y-6">
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2">
            <NIcon :component="PersonOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">基础信息</h3>
          </div>
          <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex items-start gap-3">
                <NIcon :component="PersonOutline" size="18" class="mt-1 text-primary" />
                <div>
                  <p class="text-xs text-muted">姓名</p>
                  <p class="mt-1 text-base font-semibold text-text">{{ patientDetail.name }}</p>
                </div>
              </div>
            </div>
            <div class="rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex items-start gap-3">
                <NIcon :component="CalendarOutline" size="18" class="mt-1 text-primary" />
                <div>
                  <p class="text-xs text-muted">年龄</p>
                  <p class="mt-1 text-base font-semibold text-text">{{ patientDetail.age }} 岁</p>
                </div>
              </div>
            </div>
            <div class="rounded-[20px] bg-white p-4 shadow-card md:col-span-2">
              <div class="flex items-start gap-3">
                <NIcon :component="CallOutline" size="18" class="mt-1 text-primary" />
                <div>
                  <p class="text-xs text-muted">联系电话</p>
                  <p class="mt-1 text-base font-semibold text-text">{{ patientDetail.phone }}</p>
                </div>
              </div>
            </div>
          </div>
        </NCard>

        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2">
            <NIcon :component="MedicalOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">用药计划</h3>
          </div>
          <div v-if="patientDetail.schedules.length === 0" class="mt-4">
            <NEmpty description="暂无用药计划" size="small" />
          </div>
          <div v-else class="mt-4 space-y-3">
            <article
              v-for="schedule in patientDetail.schedules"
              :key="schedule.id"
              class="rounded-[22px] bg-white p-4 shadow-card"
            >
              <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <p class="text-base font-semibold text-text">{{ schedule.medicineName }}</p>
                    <StatusBadge :label="schedule.status === 'enabled' ? '启用' : '禁用'" :tone="schedule.status === 'enabled' ? 'success' : 'neutral'" />
                  </div>
                  <p class="mt-2 text-sm text-muted">{{ schedule.dosage }} · {{ schedule.frequency }}</p>
                </div>
                <div class="grid gap-2 text-sm text-muted md:text-right">
                  <p>时间：{{ schedule.window.start }} - {{ schedule.window.end }}</p>
                  <p>周期：{{ schedule.period }}</p>
                  <p>类型：{{ schedule.type }}</p>
                </div>
              </div>
            </article>
          </div>
        </NCard>

        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2">
            <NIcon :component="AlertCircleOutline" size="20" class="text-danger" />
            <h3 class="text-lg font-semibold text-text">最近告警</h3>
          </div>
          <div v-if="patientDetail.recentAlerts.length === 0" class="mt-4">
            <NEmpty description="暂无告警记录" size="small" />
          </div>
          <div v-else class="mt-4 space-y-3">
            <article
              v-for="alert in patientDetail.recentAlerts"
              :key="alert.id"
              class="rounded-[22px] border border-danger/15 bg-danger/5 p-4"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-semibold text-text">{{ alert.title }}</p>
                  <p class="mt-2 text-sm text-muted">
                    发生时间：{{ formatDateTime(alert.occurredAt) }}
                  </p>
                </div>
                <StatusBadge label="已记录" tone="danger" />
              </div>
            </article>
          </div>
        </NCard>
      </div>
    </NModal>
  </div>
</template>

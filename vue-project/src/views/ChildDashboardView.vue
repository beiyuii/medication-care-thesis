<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  AlertCircleOutline,
  CalendarOutline,
  CallOutline,
  MedicalOutline,
  NotificationsOutline,
  PersonOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import {
  fetchCaregiverDashboard,
  type DashboardAlert,
} from '@/services/dashboardService'
import { getPatientDetail, type PatientDetail } from '@/services/patientService'
import type { HistoryEvent } from '@/types/history'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import { resolveDashboardCompletionRate } from './dashboardCompletion'
import { usePatientStore, mapApiPatientToLocal } from '@/stores/patient'

const message = useMessage()
const patientStore = usePatientStore()

const currentPatient = ref<{
  id: string | number
  name: string
  nextIntakeTime?: string
  alertCount?: number
  planStatus?: string
} | null>(null)
const isLoadingPatient = ref(false)
const intakeEvents = ref<HistoryEvent[]>([])
const activeAlerts = ref<DashboardAlert[]>([])
const isLoadingEvents = ref(false)
const completionRate = ref(0)

const loadCurrentPatient = async () => {
  isLoadingPatient.value = true
  isLoadingEvents.value = true
  try {
    const dashboard = await fetchCaregiverDashboard()
    const patient = dashboard.activePatient
    if (patient) {
      // 与护工端一致：写入全局患者上下文，供历史统计 / 检测等页通过 activePatientId 查询
      patientStore.setPatients([mapApiPatientToLocal(patient)])
      patientStore.selectPatient(String(patient.id))
      currentPatient.value = {
        id: patient.id,
        name: patient.name,
        nextIntakeTime: patient.nextIntakeTime,
        alertCount: patient.alertCount,
        planStatus: patient.planStatus,
      }
      intakeEvents.value = dashboard.recentEvents
      activeAlerts.value = dashboard.activeAlerts
      completionRate.value = resolveDashboardCompletionRate(
        dashboard.completionRate,
        dashboard.recentEvents,
      )
    } else {
      patientStore.$patch({ patients: [], activePatientId: null })
      currentPatient.value = null
      intakeEvents.value = []
      activeAlerts.value = []
      completionRate.value = 0
      message.warning('未找到关联的患者信息')
    }
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载患者信息失败')
    message.error(errorMsg)
    logError(error, '加载 child dashboard')
    patientStore.$patch({ patients: [], activePatientId: null })
    currentPatient.value = null
    intakeEvents.value = []
    activeAlerts.value = []
    completionRate.value = 0
  } finally {
    isLoadingPatient.value = false
    isLoadingEvents.value = false
  }
}

onMounted(loadCurrentPatient)

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

const showDetailModal = ref(false)
const patientDetail = ref<PatientDetail | null>(null)
const isLoadingDetail = ref(false)

const handleViewDetail = async () => {
  if (!currentPatient.value) {
    message.warning('未找到患者信息')
    return
  }

  const rawId = currentPatient.value.id
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

const latestAlerts = computed(() => activeAlerts.value.slice(0, 3))

const getStatusText = (status: string): string => {
  if (status === 'confirmed') return '已确认'
  if (status === 'suspected') return '待确认'
  if (status === 'abnormal') return '异常'
  return '未知'
}

const getStatusTone = (status: string) => {
  if (status === 'confirmed') return 'success'
  if (status === 'suspected') return 'warning'
  if (status === 'abnormal') return 'danger'
  return 'neutral'
}
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Family care"
      :title="currentPatient ? `${currentPatient.name} 的提醒概览` : '正在连接家人状态'"
      :description="currentPatient ? `下一次提醒在 ${formatIntakeTime(currentPatient.nextIntakeTime)}，你可以在这里快速查看最新提醒与异常。` : '如果尚未关联家人，页面会显示为空状态。'"
      tone="soft"
    >
      <template #meta>
        <StatusBadge :label="currentPatient ? '一对一关联' : '未找到关联患者'" :tone="currentPatient ? 'info' : 'neutral'" />
        <StatusBadge
          :label="`异常 ${currentPatient?.alertCount ?? 0} 条`"
          :tone="(currentPatient?.alertCount ?? 0) > 0 ? 'danger' : 'neutral'"
        />
      </template>
      <template #actions>
        <NButton type="primary" size="large" :disabled="!currentPatient" @click="handleViewDetail">
          查看家人详情
        </NButton>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="下一次提醒"
        :value="formatIntakeTime(currentPatient?.nextIntakeTime)"
        helper="把最重要的提醒时间放在首屏，方便快速判断是否需要联系家人。"
        tone="brand"
        :icon="NotificationsOutline"
      />
      <MetricCard
        label="今日完成率"
        :value="`${completionRate}%`"
        helper="基于当日提醒实例的真实确认率计算，不再用最近事件数量近似。"
        tone="success"
        :icon="MedicalOutline"
      />
      <MetricCard
        label="待关注异常"
        :value="currentPatient?.alertCount ?? 0"
        :helper="(currentPatient?.alertCount ?? 0) > 0 ? '如有持续异常，建议尽快联系护工或本人确认。' : '当前没有需要额外关注的异常。'"
        :tone="(currentPatient?.alertCount ?? 0) > 0 ? 'danger' : 'neutral'"
        :icon="AlertCircleOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.15fr_0.95fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Recent updates
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
                  <StatusBadge :label="getStatusText(event.status)" :tone="getStatusTone(event.status)" />
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
          <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
            Family summary
          </p>
          <h3 class="mt-2 text-2xl font-semibold text-text">家人状态摘要</h3>
          <div v-if="isLoadingPatient" class="mt-5">
            <NSkeleton text :repeat="3" />
          </div>
          <div v-else class="mt-5 space-y-4 rounded-[24px] border border-line/70 bg-[#fffcf6] p-5">
            <div class="flex items-start gap-3">
              <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <NIcon :component="PersonOutline" size="20" />
              </div>
              <div>
                <p class="text-sm text-muted">关联家人</p>
                <p class="text-lg font-semibold text-text">{{ currentPatient?.name ?? '未关联' }}</p>
              </div>
            </div>
            <div class="grid gap-3 md:grid-cols-2">
              <div class="rounded-[20px] bg-white p-4 shadow-card">
                <p class="text-sm text-muted">下一次提醒</p>
                <p class="mt-2 text-lg font-semibold text-text">{{ formatIntakeTime(currentPatient?.nextIntakeTime) }}</p>
              </div>
              <div class="rounded-[20px] bg-white p-4 shadow-card">
                <p class="text-sm text-muted">计划状态</p>
                <p class="mt-2 text-lg font-semibold text-text">{{ currentPatient?.planStatus ?? '未知' }}</p>
              </div>
            </div>
          </div>
        </NCard>

        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Alerts
              </p>
              <h3 class="mt-2 text-2xl font-semibold text-text">当前异常</h3>
            </div>
            <RouterLink to="/alerts">
              <NButton quaternary>全部查看</NButton>
            </RouterLink>
          </div>
          <div class="mt-5 space-y-3">
            <NEmpty v-if="latestAlerts.length === 0" description="暂无异常提醒" />
            <article
              v-for="alert in latestAlerts"
              v-else
              :key="alert.id"
              class="rounded-[24px] border border-danger/15 bg-danger/5 p-4"
            >
              <div class="space-y-2">
                <div class="flex items-center justify-between gap-3">
                  <p class="font-semibold text-text">{{ alert.title }}</p>
                  <StatusBadge
                    :label="alert.severity === 'high' ? '高优先级' : alert.severity === 'medium' ? '中优先级' : '提示'"
                    :tone="alert.severity === 'high' ? 'danger' : alert.severity === 'medium' ? 'warning' : 'info'"
                  />
                </div>
                <p class="text-sm leading-6 text-muted">{{ alert.description || '请查看详情并确认是否需要联系本人。' }}</p>
              </div>
            </article>
          </div>
        </NCard>
      </div>
    </section>

    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="家人详情"
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
              <p class="font-semibold text-text">{{ alert.title }}</p>
              <p class="mt-2 text-sm text-muted">发生时间：{{ formatDateTime(alert.occurredAt) }}</p>
            </article>
          </div>
        </NCard>
      </div>
    </NModal>
  </div>
</template>

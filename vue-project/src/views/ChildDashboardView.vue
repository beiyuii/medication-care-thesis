<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  AlertCircleOutline,
  CheckmarkCircleOutline,
  NotificationsOutline,
  PersonOutline,
} from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { fetchCaregiverDashboard } from '@/services/dashboardService'
import { usePatientStore, mapApiPatientToLocal } from '@/stores/patient'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import type { HistoryEvent } from '@/types/history'

const patientStore = usePatientStore()

const currentPatient = ref<{
  id: string | number
  name: string
  nextIntakeTime?: string
  alertCount?: number
  planStatus?: string
} | null>(null)
const pendingCount = ref(0)
const reviewTimeline = ref<HistoryEvent[]>([])
const timeoutEvents = ref<HistoryEvent[]>([])

const formatTime = (value?: string | null) => {
  if (!value) return '--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatDateTime = (value?: string | null) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const approvedCount = computed(
  () => reviewTimeline.value.filter(item => item.reviewDecision === 'confirmed').length,
)

const loadDashboard = async () => {
  try {
    const dashboard = await fetchCaregiverDashboard()
    const patient = dashboard.activePatient
    if (!patient) {
      currentPatient.value = null
      reviewTimeline.value = []
      timeoutEvents.value = []
      pendingCount.value = 0
      patientStore.$patch({ patients: [], activePatientId: null })
      return
    }
    patientStore.setPatients([mapApiPatientToLocal(patient)])
    patientStore.selectPatient(String(patient.id))
    currentPatient.value = {
      id: patient.id,
      name: patient.name,
      nextIntakeTime: patient.nextIntakeTime,
      alertCount: patient.alertCount,
      planStatus: patient.planStatus,
    }
    pendingCount.value = dashboard.pendingReviewInstances.length
    reviewTimeline.value = dashboard.recentEvents.filter(event => event.eventType === 'review_decided').slice(0, 8)
    timeoutEvents.value = dashboard.recentEvents.filter(event => event.eventType === 'instance_timeout').slice(0, 4)
  } catch (error: unknown) {
    logError(error, '加载 child dashboard')
    console.error(extractErrorMessage(error, '加载家人端失败'))
  }
}

const statusText = (event: HistoryEvent) => {
  if (event.reviewDecision === 'confirmed') return '护工已确认'
  if (event.reviewDecision === 'rejected') return '护工已驳回'
  if (event.reviewDecision === 'needs_evidence') return '等待补充证据'
  return event.eventType === 'instance_timeout' ? '超时提醒' : '进度更新'
}

const statusTone = (event: HistoryEvent) => {
  if (event.reviewDecision === 'confirmed') return 'success'
  if (event.reviewDecision === 'rejected') return 'danger'
  return 'warning'
}

onMounted(loadDashboard)
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Family overview"
      :title="currentPatient ? `${currentPatient.name} 的今日服药监督` : '尚未关联家人'"
      :description="currentPatient ? `下一次计划时间 ${formatTime(currentPatient.nextIntakeTime)}，你可以在这里查看护工审核结果和超时提醒。` : '如果尚未关联患者，页面会显示为空。'"
      tone="soft"
    >
      <template #meta>
        <StatusBadge :label="currentPatient ? `待审核 ${pendingCount} 条` : '未关联患者'" :tone="pendingCount > 0 ? 'warning' : 'neutral'" />
        <StatusBadge :label="`告警 ${currentPatient?.alertCount ?? 0} 条`" :tone="(currentPatient?.alertCount ?? 0) > 0 ? 'danger' : 'neutral'" />
      </template>
      <template #actions>
        <RouterLink to="/history">
          <NButton type="primary" size="large">查看完整时间线</NButton>
        </RouterLink>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="待审核"
        :value="pendingCount"
        helper="老人已提交但仍等待护工审核的记录。"
        tone="warning"
        :icon="NotificationsOutline"
      />
      <MetricCard
        label="已确认"
        :value="approvedCount"
        helper="今日最近审核结果中的已确认次数。"
        tone="success"
        :icon="CheckmarkCircleOutline"
      />
      <MetricCard
        label="关联家人"
        :value="currentPatient?.name ?? '未关联'"
        helper="家人端只读监督，不参与审核。"
        tone="neutral"
        :icon="PersonOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.15fr_0.95fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Reviewed timeline
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">今日审核结果</h3>
          </div>
          <RouterLink to="/history">
            <NButton quaternary>查看全部</NButton>
          </RouterLink>
        </div>

        <div class="mt-5 space-y-3">
          <NEmpty v-if="reviewTimeline.length === 0" description="暂时还没有审核结果" />
          <article
            v-for="event in reviewTimeline"
            v-else
            :key="event.id"
            class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-4"
          >
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-3">
                <p class="text-lg font-semibold text-text">{{ event.medicineName }}</p>
                <StatusBadge :label="statusText(event)" :tone="statusTone(event)" />
              </div>
              <p class="text-sm text-muted">{{ event.reviewReason || event.action }}</p>
              <p class="text-sm text-muted">
                {{ event.confirmedBy || '护工' }} · {{ formatDateTime(event.confirmedAt || event.timestamp) }}
              </p>
            </div>
          </article>
        </div>
      </NCard>

      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-danger/10 text-danger">
            <NIcon :component="AlertCircleOutline" size="20" />
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Escalations
            </p>
            <h3 class="text-2xl font-semibold text-text">超时与异常提醒</h3>
          </div>
        </div>

        <div class="mt-5 space-y-3">
          <NEmpty v-if="timeoutEvents.length === 0" description="当前没有超时提醒" />
          <article
            v-for="event in timeoutEvents"
            v-else
            :key="event.id"
            class="rounded-[24px] border border-danger/15 bg-danger/5 p-4"
          >
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-3">
                <p class="font-semibold text-text">{{ event.medicineName }}</p>
                <StatusBadge label="超时提醒" tone="danger" />
              </div>
              <p class="text-sm text-muted">{{ event.action }}</p>
              <p class="text-sm text-muted">{{ formatDateTime(event.timestamp) }}</p>
            </div>
          </article>
        </div>
      </NCard>
    </section>
  </div>
</template>

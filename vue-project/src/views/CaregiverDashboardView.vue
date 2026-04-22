<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watchEffect } from 'vue'
import { RouterLink } from 'vue-router'
import type { SelectOption } from 'naive-ui'
import { useMessage } from 'naive-ui'
import {
  AlertCircleOutline,
  CheckmarkCircleOutline,
  MedicalOutline,
  PersonOutline,
} from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { usePatientList } from '@/composables/usePatientList'
import { fetchCaregiverDashboard, type ReminderInstanceItem } from '@/services/dashboardService'
import {
  requestReminderEvidence,
  reviewReminderInstance,
} from '@/services/reminderInstanceService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import { resolveDashboardCompletionRate } from './dashboardCompletion'

const { patients, activePatient, activePatientId, hydratePatients } = usePatientList()
const message = useMessage()

const queue = ref<ReminderInstanceItem[]>([])
const reviewedTimeline = ref<ReminderInstanceItem[]>([])
const isLoading = ref(false)
const completionRate = ref(0)
const actingInstanceId = ref<number | null>(null)
const showRejectModal = ref(false)
const rejectReason = ref('未服')
const rejectNote = ref('')
const selectedRejectInstance = ref<ReminderInstanceItem | null>(null)
let dashboardPollingTimer: number | null = null

const reasonOptions = ['未服', '服错药', '剂量不对', '记录不清晰', '其他']

const patientOptions = computed<SelectOption[]>(() =>
  patients.value.map(patient => ({ label: patient.name, value: patient.id })),
)

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

const reviewLabel = (item: ReminderInstanceItem) => {
  switch (item.reviewStatus) {
    case 'waiting_caregiver':
      return '待审核'
    case 'review_timeout':
      return '已超时待补审'
    case 'waiting_caregiver_late':
      return '迟服待审核'
    case 'abnormal_pending_review':
      return '检测异常待审核'
    case 'evidence_required':
      return '等待补充证据'
    case 'caregiver_confirmed':
      return '已确认'
    case 'caregiver_rejected':
      return '已驳回'
    default:
      return item.reviewStatus
  }
}

const reviewTone = (item: ReminderInstanceItem) => {
  if (item.reviewStatus === 'caregiver_confirmed') return 'success'
  if (item.reviewStatus === 'caregiver_rejected') return 'danger'
  if (item.reviewStatus === 'abnormal_pending_review' || item.reviewStatus === 'review_timeout') return 'warning'
  return 'info'
}

const detectionLabel = (status: ReminderInstanceItem['detectionStatus']) => {
  switch (status) {
    case 'confirmed':
      return '检测通过'
    case 'suspected':
      return '检测疑似'
    case 'abnormal':
      return '检测异常'
    default:
      return '未检测'
  }
}

const evidenceSummary = (item: ReminderInstanceItem) => {
  if (!item.detectionReasonText) return null
  const target = item.targetConfidence != null ? `药品证据 ${(item.targetConfidence * 100).toFixed(1)}%` : null
  const action = item.actionConfidence != null ? `动作证据 ${(item.actionConfidence * 100).toFixed(1)}%` : null
  return [item.detectionReasonText, target, action].filter(Boolean).join(' · ')
}

const loadDashboard = async () => {
  if (!activePatient.value?.id) {
    queue.value = []
    reviewedTimeline.value = []
    return
  }

  isLoading.value = true
  try {
    const patientId = Number(activePatient.value.id)
    const dashboard = await fetchCaregiverDashboard(patientId)
    queue.value = [...dashboard.pendingReviewInstances].sort((a, b) => {
      const timeA = a.reviewDeadline ? new Date(a.reviewDeadline).getTime() : Number.MAX_SAFE_INTEGER
      const timeB = b.reviewDeadline ? new Date(b.reviewDeadline).getTime() : Number.MAX_SAFE_INTEGER
      return timeA - timeB
    })
    reviewedTimeline.value = dashboard.recentEvents
      .filter(event => event.eventType === 'review_decided')
      .slice(0, 6)
      .map(event => ({
        id: Number(event.id),
        patientId: event.patientId ?? patientId,
        scheduleId: event.scheduleId ?? 0,
        scheduledDate: '',
        windowStartAt: event.timestamp,
        windowEndAt: event.timestamp,
        status: event.reviewDecision === 'confirmed' ? 'caregiver_confirmed' : 'caregiver_rejected',
        reviewStatus: event.reviewDecision === 'confirmed' ? 'caregiver_confirmed' : 'caregiver_rejected',
        detectionStatus: event.detectionStatus ?? 'none',
        retryCount: 0,
        medicineName: event.medicineName,
        dose: '',
        frequency: '',
        reviewReason: event.reviewReason ?? event.action,
        reviewedBy: event.confirmedBy,
        reviewedAt: event.confirmedAt,
        activeAlertTitles: [],
      }))
    completionRate.value = resolveDashboardCompletionRate(
      dashboard.completionRate,
      dashboard.recentEvents,
    )
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载护工端首页失败')
    message.error(errorMsg)
    logError(error, '加载 caregiver dashboard')
    queue.value = []
    reviewedTimeline.value = []
  } finally {
    isLoading.value = false
  }
}

const confirmInstance = async (item: ReminderInstanceItem) => {
  actingInstanceId.value = item.id
  try {
    await reviewReminderInstance(item.id, {
      decision: 'confirmed',
      reviewedBy: 'caregiver',
      reviewTime: new Date().toISOString(),
    })
    message.success('已确认本次服药')
    await loadDashboard()
  } catch (error: unknown) {
    message.error(extractErrorMessage(error, '确认失败'))
    logError(error, 'caregiver confirm review')
  } finally {
    actingInstanceId.value = null
  }
}

const requestEvidence = async (item: ReminderInstanceItem) => {
  actingInstanceId.value = item.id
  try {
    await requestReminderEvidence(item.id, {
      requestedBy: 'caregiver',
      requestTime: new Date().toISOString(),
      note: '请补充更清晰的画面或线下核实后再确认',
    })
    message.success('已要求补充证据')
    await loadDashboard()
  } catch (error: unknown) {
    message.error(extractErrorMessage(error, '操作失败'))
    logError(error, 'caregiver request evidence')
  } finally {
    actingInstanceId.value = null
  }
}

const openRejectModal = (item: ReminderInstanceItem) => {
  selectedRejectInstance.value = item
  rejectReason.value = '未服'
  rejectNote.value = ''
  showRejectModal.value = true
}

const submitReject = async () => {
  if (!selectedRejectInstance.value) return
  if (rejectReason.value === '其他' && !rejectNote.value.trim()) {
    message.warning('请选择“其他”时需要填写文字说明')
    return
  }

  actingInstanceId.value = selectedRejectInstance.value.id
  try {
    await reviewReminderInstance(selectedRejectInstance.value.id, {
      decision: 'rejected',
      reviewedBy: 'caregiver',
      reviewTime: new Date().toISOString(),
      reason:
        rejectReason.value === '其他'
          ? `其他：${rejectNote.value.trim()}`
          : rejectReason.value,
    })
    showRejectModal.value = false
    message.success('已驳回并回到重服链路')
    await loadDashboard()
  } catch (error: unknown) {
    message.error(extractErrorMessage(error, '驳回失败'))
    logError(error, 'caregiver reject review')
  } finally {
    actingInstanceId.value = null
  }
}

watchEffect(() => {
  if (activePatient.value?.id) {
    loadDashboard()
  }
})

onMounted(async () => {
  await hydratePatients()
  await loadDashboard()
  dashboardPollingTimer = window.setInterval(() => {
    if (activePatient.value?.id && !isLoading.value && actingInstanceId.value === null) {
      loadDashboard()
    }
  }, 5000)
})

onUnmounted(() => {
  if (dashboardPollingTimer) {
    window.clearInterval(dashboardPollingTimer)
    dashboardPollingTimer = null
  }
})
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Review queue"
      :title="activePatient ? `${activePatient.name} 的待审核队列` : '请选择一位被照护人'"
      :description="activePatient ? '首页优先展示待审核记录，默认按审核截止时间升序排列。' : '选择患者后开始审核今日提交的服药记录。'"
      tone="soft"
    >
      <template #meta>
        <StatusBadge :label="activePatient ? `待审核 ${queue.length} 条` : '未选择患者'" :tone="queue.length > 0 ? 'warning' : 'neutral'" />
        <StatusBadge :label="`今日完成率 ${completionRate}%`" tone="success" />
      </template>
      <template #actions>
        <div class="min-w-[280px]">
          <NSelect
            v-model:value="activePatientId"
            :options="patientOptions"
            placeholder="请选择患者"
            size="large"
            :disabled="patients.length === 0"
          />
        </div>
        <RouterLink to="/history">
          <NButton size="large" tertiary>查看全部记录</NButton>
        </RouterLink>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="待审核记录"
        :value="queue.length"
        helper="按审核截止时间升序排列。"
        tone="warning"
        :icon="AlertCircleOutline"
      />
      <MetricCard
        label="已审核结果"
        :value="reviewedTimeline.length"
        helper="展示最近的审核决定。"
        tone="success"
        :icon="CheckmarkCircleOutline"
      />
      <MetricCard
        label="当前患者"
        :value="activePatient?.name ?? '未选择'"
        helper="先切换患者，再处理审核。"
        tone="neutral"
        :icon="PersonOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.25fr_0.95fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Pending reviews
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">待审核队列</h3>
          </div>
          <StatusBadge :label="`${queue.length} 条待处理`" :tone="queue.length > 0 ? 'warning' : 'neutral'" />
        </div>

        <div class="mt-5 space-y-3">
          <NSkeleton v-if="isLoading" text :repeat="5" />
          <NEmpty v-else-if="queue.length === 0" description="当前没有待审核记录" />
          <article
            v-for="item in queue"
            v-else
            :key="item.id"
            class="rounded-[24px] border p-4"
            :class="
              item.detectionRiskTag === 'possible_fake_intake'
                ? 'border-warning/30 bg-warning/10'
                : 'border-line/70 bg-[#fffcf6]'
            "
          >
            <div class="space-y-3">
              <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div class="space-y-2">
                  <div class="flex flex-wrap items-center gap-3">
                    <p class="text-lg font-semibold text-text">{{ item.medicineName }}</p>
                    <StatusBadge :label="reviewLabel(item)" :tone="reviewTone(item)" />
                    <StatusBadge :label="detectionLabel(item.detectionStatus)" :tone="item.detectionStatus === 'abnormal' ? 'warning' : 'neutral'" />
                    <StatusBadge
                      v-if="item.detectionRiskTag === 'possible_fake_intake'"
                      label="疑似假吃"
                      tone="warning"
                    />
                  </div>
                  <p class="text-sm text-muted">
                    {{ formatTime(item.windowStartAt) }} - {{ formatTime(item.windowEndAt) }} · {{ item.frequency }} · {{ item.dose }}
                  </p>
                  <p class="text-sm text-muted">
                    审核截止：{{ formatDateTime(item.reviewDeadline) }}
                    <span v-if="item.lateMinutes"> · 迟服 {{ item.lateMinutes }} 分钟</span>
                  </p>
                  <p v-if="evidenceSummary(item)" class="text-sm text-muted">
                    {{ evidenceSummary(item) }}
                  </p>
                  <p v-if="item.reviewReason" class="text-sm text-muted">最近备注：{{ item.reviewReason }}</p>
                </div>
                <StatusBadge :label="`第 ${item.retryCount + 1} 次尝试`" tone="neutral" />
              </div>

              <div class="flex flex-wrap gap-3">
                <NButton
                  type="primary"
                  :disabled="item.detectionStatus === 'abnormal'"
                  :loading="actingInstanceId === item.id"
                  @click="confirmInstance(item)"
                >
                  确认已正确服用
                </NButton>
                <NButton
                  v-if="item.detectionStatus === 'abnormal'"
                  type="warning"
                  ghost
                  :loading="actingInstanceId === item.id"
                  @click="requestEvidence(item)"
                >
                  要求补充证据
                </NButton>
                <NButton
                  type="error"
                  ghost
                  :loading="actingInstanceId === item.id"
                  @click="openRejectModal(item)"
                >
                  退回重新服用
                </NButton>
              </div>
              <p
                v-if="item.detectionRiskTag === 'possible_fake_intake'"
                class="text-sm font-medium text-warning"
              >
                建议优先要求补充证据或退回重服，不建议直接放行。
              </p>
            </div>
          </article>
        </div>
      </NCard>

      <div class="space-y-6">
        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center gap-3">
            <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <NIcon :component="MedicalOutline" size="20" />
            </div>
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Rules
              </p>
              <h3 class="text-2xl font-semibold text-text">审核规则</h3>
            </div>
          </div>
          <ul class="mt-5 space-y-3 text-sm leading-7 text-muted">
            <li>检测异常时禁用直接确认，必须先要求补充证据或驳回重服。</li>
            <li>驳回会创建新的重服实例并保留完整重试链。</li>
            <li>重试达到上限后会进入人工介入，不再继续自动重服。</li>
          </ul>
        </NCard>

        <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
                Recent decisions
              </p>
              <h3 class="mt-2 text-2xl font-semibold text-text">最近审核结果</h3>
            </div>
            <RouterLink to="/history">
              <NButton quaternary>历史时间线</NButton>
            </RouterLink>
          </div>
          <div class="mt-5 space-y-3">
            <NEmpty v-if="reviewedTimeline.length === 0" description="暂无审核结果" />
            <article
              v-for="item in reviewedTimeline"
              v-else
              :key="item.id"
              class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-4"
            >
              <div class="space-y-2">
                <div class="flex flex-wrap items-center gap-3">
                  <p class="font-semibold text-text">{{ item.medicineName }}</p>
                  <StatusBadge :label="reviewLabel(item)" :tone="reviewTone(item)" />
                </div>
                <p class="text-sm text-muted">{{ item.reviewReason || '已完成审核' }}</p>
                <p class="text-sm text-muted">
                  {{ item.reviewedBy || '护工' }} · {{ formatDateTime(item.reviewedAt) }}
                </p>
              </div>
            </article>
          </div>
        </NCard>
      </div>
    </section>

    <NModal
      v-model:show="showRejectModal"
      preset="card"
      title="退回重新服用"
      style="width: 520px; max-width: 92vw"
      :bordered="false"
    >
      <div class="space-y-4">
        <NSelect
          v-model:value="rejectReason"
          :options="reasonOptions.map(item => ({ label: item, value: item }))"
          placeholder="请选择驳回原因"
        />
        <NInput
          v-model:value="rejectNote"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          placeholder="如果选择“其他”，这里填写具体说明；其他原因也可补充备注"
        />
        <div class="flex justify-end gap-3">
          <NButton @click="showRejectModal = false">取消</NButton>
          <NButton type="error" :loading="actingInstanceId !== null" @click="submitReject">
            确认驳回
          </NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>

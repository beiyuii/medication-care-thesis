<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  CheckmarkCircleOutline,
  MedicalOutline,
  NotificationsOutline,
  WarningOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { fetchElderDashboard, type ReminderInstanceItem } from '@/services/dashboardService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'

const router = useRouter()
const message = useMessage()

const todayInstances = ref<ReminderInstanceItem[]>([])
const loading = ref(false)

const actionableStatuses = new Set(['caregiver_rejected', 'not_submitted', 'manual_intervention'])

const toClock = (value?: string | null): string => {
  if (!value) return '--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatPlanDose = (item: ReminderInstanceItem) =>
  [item.frequency, item.dose].filter(Boolean).join(' · ') || '--'

const priorityOf = (item: ReminderInstanceItem): number => {
  if (item.reviewStatus === 'caregiver_rejected') return 0
  if (item.reviewStatus === 'not_submitted') return 1
  if (item.reviewStatus === 'manual_intervention') return 2
  return 9
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const dashboard = await fetchElderDashboard()
    todayInstances.value = [...dashboard.todayInstances].sort((a, b) => {
      const priorityDiff = priorityOf(a) - priorityOf(b)
      if (priorityDiff !== 0) return priorityDiff
      return new Date(a.windowStartAt).getTime() - new Date(b.windowStartAt).getTime()
    })
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载老人端首页失败')
    message.error(errorMsg)
    logError(error, '加载 elder dashboard')
  } finally {
    loading.value = false
  }
}

const primaryTask = computed(
  () => todayInstances.value.find(item => actionableStatuses.has(item.reviewStatus)) ?? null,
)

const timelineItems = computed(() =>
  todayInstances.value.filter(item => item.reviewStatus !== 'manual_intervention'),
)

const feedbackItems = computed(() =>
  todayInstances.value.filter(item =>
    ['waiting_caregiver', 'review_timeout', 'caregiver_confirmed', 'abnormal_pending_review', 'evidence_required', 'waiting_caregiver_late'].includes(item.reviewStatus),
  ),
)

const confirmedCount = computed(
  () => todayInstances.value.filter(item => item.reviewStatus === 'caregiver_confirmed').length,
)
const waitingCount = computed(
  () => todayInstances.value.filter(item => ['waiting_caregiver', 'review_timeout', 'waiting_caregiver_late'].includes(item.reviewStatus)).length,
)
const retryCount = computed(
  () => todayInstances.value.filter(item => item.reviewStatus === 'caregiver_rejected').length,
)

const statusText = (status: ReminderInstanceItem['reviewStatus']) => {
  switch (status) {
    case 'not_submitted':
      return '待服药'
    case 'waiting_caregiver':
    case 'review_timeout':
      return '等待护工确认'
    case 'waiting_caregiver_late':
      return '迟服待确认'
    case 'caregiver_rejected':
      return '需重新服药'
    case 'caregiver_confirmed':
      return '已确认完成'
    case 'abnormal_pending_review':
      return '检测异常待审核'
    case 'evidence_required':
      return '等待补充证据'
    case 'manual_intervention':
      return '请联系护工处理'
    case 'missed':
      return '已漏服'
    default:
      return status
  }
}

const statusTone = (status: ReminderInstanceItem['reviewStatus']) => {
  switch (status) {
    case 'caregiver_confirmed':
      return 'success'
    case 'caregiver_rejected':
    case 'manual_intervention':
      return 'danger'
    case 'abnormal_pending_review':
    case 'evidence_required':
    case 'missed':
      return 'warning'
    default:
      return 'info'
  }
}

const primaryButtonLabel = computed(() => {
  if (!primaryTask.value) return '今日暂无主任务'
  switch (primaryTask.value.reviewStatus) {
    case 'not_submitted':
      return '开始服药'
    case 'caregiver_rejected':
      return '重新服药'
    case 'manual_intervention':
      return '请联系护工处理'
    default:
      return '查看详情'
  }
})

const primaryDescription = computed(() => {
  if (!primaryTask.value) return '当前没有需要你立刻处理的任务。'
  if (primaryTask.value.reviewStatus === 'caregiver_rejected') {
    return primaryTask.value.reviewReason || '护工判定这次服药需要重新执行，请按提示重新服药。'
  }
  if (primaryTask.value.reviewStatus === 'manual_intervention') {
    return '这次任务已达到重试上限，请直接联系护工处理。'
  }
  return '请按照当前计划完成服药，提交后等待护工确认。'
})

const handlePrimaryAction = () => {
  if (!primaryTask.value) return
  if (primaryTask.value.reviewStatus === 'manual_intervention') {
    router.push('/history')
    return
  }
  router.push('/detection')
}

onMounted(loadDashboard)
onActivated(loadDashboard)
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Primary task"
      :title="primaryTask ? `${primaryTask.medicineName} · ${statusText(primaryTask.reviewStatus)}` : '今天暂无待处理任务'"
      :description="primaryDescription"
      tone="brand"
    >
      <template #meta>
        <StatusBadge
          :label="primaryTask ? `${toClock(primaryTask.windowStartAt)} - ${toClock(primaryTask.windowEndAt)}` : '当前空闲'"
          :tone="primaryTask ? 'info' : 'neutral'"
        />
        <StatusBadge
          v-if="primaryTask"
          :label="formatPlanDose(primaryTask)"
          tone="neutral"
        />
      </template>
      <template #actions>
        <NButton
          type="primary"
          size="large"
          :disabled="!primaryTask"
          @click="handlePrimaryAction"
        >
          {{ primaryButtonLabel }}
        </NButton>
        <RouterLink to="/history">
          <NButton size="large" tertiary>查看记录</NButton>
        </RouterLink>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="今日计划"
        :value="todayInstances.length"
        helper="按时段拆分后的所有服药任务。"
        tone="neutral"
        :icon="NotificationsOutline"
      />
      <MetricCard
        label="已确认完成"
        :value="confirmedCount"
        helper="护工已经确认通过的次数。"
        tone="success"
        :icon="CheckmarkCircleOutline"
      />
      <MetricCard
        label="需关注"
        :value="retryCount > 0 ? retryCount : waitingCount"
        :helper="retryCount > 0 ? '优先处理被驳回的重服任务。' : '等待护工确认的任务会显示在反馈区。'"
        :tone="retryCount > 0 ? 'danger' : 'warning'"
        :icon="WarningOutline"
      />
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.2fr_0.9fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Today timeline
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">今日计划时间线</h3>
          </div>
          <RouterLink to="/plans">
            <NButton quaternary>查看计划</NButton>
          </RouterLink>
        </div>

        <div class="mt-5 space-y-3">
          <NSkeleton v-if="loading" text :repeat="4" />
          <NEmpty v-else-if="timelineItems.length === 0" description="今日暂无服药任务" />
          <article
            v-for="item in timelineItems"
            v-else
            :key="item.id"
            class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-4"
          >
            <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div class="space-y-2">
                <div class="flex flex-wrap items-center gap-3">
                  <p class="text-lg font-semibold text-text">{{ item.medicineName }}</p>
                  <StatusBadge :label="statusText(item.reviewStatus)" :tone="statusTone(item.reviewStatus)" />
                </div>
                <p class="text-sm text-muted">{{ formatPlanDose(item) }}</p>
                <p class="text-sm text-muted">时间窗：{{ toClock(item.windowStartAt) }} - {{ toClock(item.windowEndAt) }}</p>
              </div>
              <div class="flex items-center gap-3">
                <StatusBadge :label="`第 ${item.retryCount + 1} 次尝试`" tone="neutral" />
                <NButton
                  v-if="item.reviewStatus === 'not_submitted' || item.reviewStatus === 'caregiver_rejected'"
                  type="primary"
                  @click="router.push('/detection')"
                >
                  {{ item.reviewStatus === 'caregiver_rejected' ? '重新服药' : '开始服药' }}
                </NButton>
              </div>
            </div>
          </article>
        </div>
      </NCard>

      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <NIcon :component="MedicalOutline" size="20" />
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Caregiver feedback
            </p>
            <h3 class="text-2xl font-semibold text-text">护工确认反馈</h3>
          </div>
        </div>

        <div class="mt-5 space-y-3">
          <NEmpty v-if="feedbackItems.length === 0" description="暂时没有新的护工反馈" />
          <article
            v-for="item in feedbackItems"
            v-else
            :key="item.id"
            class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-4"
          >
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-3">
                <p class="font-semibold text-text">{{ item.medicineName }}</p>
                <StatusBadge :label="statusText(item.reviewStatus)" :tone="statusTone(item.reviewStatus)" />
              </div>
              <p class="text-sm text-muted">
                {{ item.reviewReason || (item.reviewStatus === 'caregiver_confirmed' ? '护工已确认本次服药完成。' : '正在等待护工处理。') }}
              </p>
              <p class="text-sm text-muted">
                {{ item.reviewedAt ? `处理时间：${toClock(item.reviewedAt)}` : `提交时间窗：${toClock(item.windowStartAt)} - ${toClock(item.windowEndAt)}` }}
              </p>
            </div>
          </article>
        </div>

        <div class="mt-5 rounded-[24px] border border-line/70 bg-[#fffcf6] p-4 text-sm leading-7 text-muted">
          老人端只保留一个主任务。等待护工确认时不再出现额外操作，避免把“等待”误当成“还要继续点按钮”。
        </div>
      </NCard>
    </section>
  </div>
</template>

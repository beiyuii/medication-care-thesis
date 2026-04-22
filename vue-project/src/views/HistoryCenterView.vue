<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { useRouter } from 'vue-router'
import {
  TimeOutline,
  CheckmarkCircleOutline,
  WarningOutline,
  CloseCircleOutline,
  ImageOutline,
} from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { HistoryEvent, HistoryRange, HistorySummary } from '@/types/history'
import { fetchHistoryEvents, fetchHistorySummary } from '@/services/historyService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import { useAuthStore } from '@/stores/auth'
import { usePatientStore } from '@/stores/patient'

const router = useRouter()
const authStore = useAuthStore()
const patientStore = usePatientStore()
const message = useMessage()

/** range 表示当前选择的时间维度。 */
const range = ref<HistoryRange>('day')
/** loading 控制加载状态。 */
const loading = ref(false)
/** summaries 保存统计卡片数据。 */
const summaries = ref<HistorySummary[]>([])
/** events 保存历史事件列表。 */
const events = ref<HistoryEvent[]>([])
// 详情Modal相关状态
const showDetailModal = ref(false)
const selectedEvent = ref<HistoryEvent | null>(null)

/**
 * handleViewDetail 查看事件详情
 */
const handleViewDetail = (event: HistoryEvent) => {
  selectedEvent.value = event
  showDetailModal.value = true
}

/**
 * parseTargetsJson 解析目标检测 JSON（兼容对象或数组格式）。
 */
const parseTargetsJson = (targetsJson?: string): Record<string, number> | null => {
  if (!targetsJson) return null
  try {
    const parsed: unknown = JSON.parse(targetsJson)
    if (Array.isArray(parsed)) {
      const rec: Record<string, number> = {}
      parsed.forEach((item, index) => {
        if (item && typeof item === 'object' && 'label' in item) {
          const row = item as { label?: unknown; score?: unknown }
          const label = String(row.label ?? index)
          const score = Number(row.score ?? 0)
          rec[label] = Number.isFinite(score) ? score : 0
        }
      })
      return Object.keys(rec).length > 0 ? rec : null
    }
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed as Record<string, number>
    }
    return null
  } catch {
    return null
  }
}

/**
 * formatTargets 格式化目标检测结果显示（score 为 0~1 时按百分比展示）。
 */
const formatTargets = (targets: Record<string, number> | null): string => {
  if (!targets) return '无检测结果'
  const entries = Object.entries(targets)
  if (entries.length === 0) return '无检测结果'
  return entries
    .map(([key, value]) => {
      const n = Number(value)
      if (!Number.isFinite(n)) {
        return `${key}: --`
      }
      const pct = n <= 1 && n >= 0 ? n * 100 : n
      return `${key}: ${pct.toFixed(1)}%`
    })
    .join(', ')
}

const getDetailSectionTitle = (event: HistoryEvent): string => {
  switch (event.eventType) {
    case 'detection_completed':
      return '检测结果'
    case 'intake_submitted':
      return '提交流程'
    case 'review_decided':
      return '审核结果'
    case 'instance_timeout':
      return '超时信息'
    case 'retry_created':
      return '重服链路'
    default:
      return '事件信息'
  }
}

const getActionLabel = (event: HistoryEvent): string => {
  const raw = event.rawAction ?? event.action ?? ''
  switch (raw) {
    case 'elder_submitted':
      return '老人已提交本次服药记录，等待护工审核。'
    case 'review_timeout':
      return '护工未在截止时间前完成审核。'
    case 'missed_timeout':
      return '该次提醒在时间窗结束前未完成提交。'
    case 'retry_created':
      return '护工驳回后已创建新的重服实例。'
    case 'caregiver_confirmed':
      return '护工已确认本次服药完成。'
    case 'caregiver_rejected':
      return '护工已驳回本次记录并要求重新服用。'
    case 'caregiver_request_evidence':
      return '护工要求补充证据后再继续审核。'
    default:
      return raw || '暂无补充说明'
  }
}

const getDetailEmptyDescription = (event: HistoryEvent): string => {
  switch (event.eventType) {
    case 'intake_submitted':
      return '这是一条提交事件，本身不包含检测截图或录像。'
    case 'review_decided':
      return '这是一条审核事件，重点查看审核结论和备注。'
    case 'instance_timeout':
      return '这是一条超时事件，本身不包含检测媒体。'
    case 'retry_created':
      return '这是一条重服创建事件，本身不包含检测媒体。'
    default:
      return '暂无图片与录像'
  }
}

const shouldShowDetectionMedia = (event: HistoryEvent): boolean =>
  event.eventType === 'detection_completed'

const shouldShowTargets = (event: HistoryEvent): boolean =>
  event.eventType === 'detection_completed' && Boolean(event.targetsJson)

const shouldShowActionBlock = (event: HistoryEvent): boolean =>
  event.eventType !== 'review_decided' || Boolean(event.rawAction)

/**
 * getStatusInfo 获取状态信息
 */
const getStatusInfo = (event: HistoryEvent) => {
  if (event.eventType === 'review_decided') {
    if (event.reviewDecision === 'confirmed') {
      return { text: '护工已确认', type: 'success', icon: CheckmarkCircleOutline }
    }
    if (event.reviewDecision === 'needs_evidence') {
      return { text: '等待补证', type: 'warning', icon: WarningOutline }
    }
    return { text: '护工已驳回', type: 'error', icon: CloseCircleOutline }
  }
  if (event.eventType === 'instance_timeout') {
    return { text: '超时', type: 'error', icon: CloseCircleOutline }
  }
  if (event.eventType === 'retry_created') {
    return { text: '重服已创建', type: 'warning', icon: WarningOutline }
  }
  if (event.eventType === 'intake_submitted') {
    return { text: '已提交待审核', type: 'warning', icon: WarningOutline }
  }
  if (event.eventType === 'detection_completed') {
    if (event.detectionStatus === 'confirmed') {
      return { text: '检测通过', type: 'success', icon: CheckmarkCircleOutline }
    }
    if (event.detectionStatus === 'suspected') {
      return { text: '检测疑似', type: 'warning', icon: WarningOutline }
    }
    return { text: '检测异常', type: 'error', icon: CloseCircleOutline }
  }
  return { text: '计划生成', type: 'default', icon: TimeOutline }
}

/**
 * formatDateTime 格式化日期时间显示
 */
const formatDateTime = (dateStr: string): string => {
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch {
    return dateStr
  }
}

/**
 * checkPatientAvailable 检查是否有可用的患者数据
 */
const checkPatientAvailable = (): boolean => {
  if (authStore.role === 'elder') {
    // elder角色：使用自己的ID，应该总是可用
    return !!authStore.user?.id
  } else {
    // caregiver/child角色：需要检查是否有选中的患者
    return !!patientStore.activePatientId
  }
}

/**
 * redirectToHome 跳转到首页
 */
const redirectToHome = () => {
  const role = authStore.role
  let homePath = '/elder/home'
  let tipMessage = '请先选择患者后再查看历史记录'
  
  if (role === 'caregiver') {
    homePath = '/caregiver/home'
    tipMessage = '请先选择被照护人后再查看历史记录'
  } else if (role === 'child') {
    homePath = '/child/home'
    tipMessage = '未找到关联的患者信息，请返回首页'
  }
  
  message.warning(tipMessage)
  // 延迟跳转，让用户看到提示信息
  setTimeout(() => {
    router.push(homePath)
  }, 1500)
}

const loadHistory = async () => {
  // 检查是否有可用的患者数据
  if (!checkPatientAvailable()) {
    redirectToHome()
    return
  }

  loading.value = true
  try {
    const [summaryRes, eventRes] = await Promise.all([
      fetchHistorySummary(range.value),
      fetchHistoryEvents(range.value),
    ])
    summaries.value = summaryRes
    events.value = eventRes
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载历史数据失败')
    
    // 检查是否是"无法获取患者ID"的错误
    if (errorMsg.includes('无法获取患者ID') || errorMsg.includes('请先选择患者')) {
      redirectToHome()
      return
    }
    
    message.error(errorMsg)
    logError(error, '加载历史数据')
  } finally {
    loading.value = false
  }
}

watch(range, loadHistory)

// 监听患者变化，如果患者被清空，跳转到首页
watch(
  () => patientStore.activePatientId,
  (newId) => {
    // 只有caregiver和child角色需要监听患者变化
    if (authStore.role !== 'elder' && !newId) {
      redirectToHome()
    }
  }
)

onMounted(() => {
  // 页面加载时检查患者数据
  if (!checkPatientAvailable()) {
    redirectToHome()
    return
  }
  loadHistory()
})

const rangeLabelMap: Record<HistoryRange, string> = {
  day: '今日',
  week: '本周',
  month: '本月',
}
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="History & reports"
      title="历史记录与统计"
      :description="`按 ${rangeLabelMap[range]} 维度查看提醒实例和服药事件，辅助回顾执行质量与人工确认情况。`"
      tone="soft"
    >
      <template #meta>
        <StatusBadge :label="`${rangeLabelMap[range]} 视图`" tone="info" />
        <StatusBadge :label="`事件 ${events.length} 条`" :tone="events.length > 0 ? 'success' : 'neutral'" />
      </template>
      <template #actions>
        <NRadioGroup v-model:value="range" size="small">
          <NRadioButton value="day">今日</NRadioButton>
          <NRadioButton value="week">本周</NRadioButton>
          <NRadioButton value="month">本月</NRadioButton>
        </NRadioGroup>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        v-for="item in summaries"
        :key="item.label"
        :label="item.label"
        :value="`${item.value}${item.unit}`"
        :helper="item.description"
        tone="neutral"
      />
    </section>

    <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
            Event records
          </p>
          <h3 class="mt-2 text-2xl font-semibold text-text">事件记录</h3>
        </div>
        <StatusBadge :label="`共 ${events.length} 条`" tone="info" />
      </div>

      <div class="mt-5 space-y-4">
        <NSkeleton v-if="loading" text :repeat="4" />
        <NEmpty v-else-if="events.length === 0" description="暂无历史记录" />
        <article
          v-else
          v-for="event in events"
          :key="event.id"
          class="rounded-[26px] border border-line/70 bg-[#fffcf6] p-5 shadow-card"
        >
          <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div class="space-y-3">
              <div class="flex flex-wrap items-center gap-3">
                <h4 class="text-xl font-semibold text-text">{{ event.medicineName }}</h4>
                <StatusBadge
                  :label="getStatusInfo(event).text"
                  :tone="
                    getStatusInfo(event).type === 'error'
                      ? 'danger'
                      : getStatusInfo(event).type === 'warning'
                        ? 'warning'
                        : getStatusInfo(event).type === 'success'
                          ? 'success'
                          : 'neutral'
                  "
                />
              </div>
              <p class="text-sm leading-6 text-muted">{{ event.planName }}</p>
              <p class="text-sm leading-6 text-muted">事件类型：{{ event.eventType }}</p>
              <p class="text-sm leading-6 text-muted">{{ event.action }}</p>
              <p class="text-sm text-muted">{{ event.timestamp }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <NButton size="small" type="primary" tertiary @click.stop="handleViewDetail(event)">
                查看详情
              </NButton>
            </div>
          </div>
          <img
            v-if="event.imageUrl"
            :src="event.imageUrl"
            alt="事件截图"
            class="mt-4 h-24 w-40 rounded-[18px] object-cover shadow-card cursor-pointer hover:opacity-85 transition-opacity"
            @click="handleViewDetail(event)"
          />
        </article>
      </div>
    </NCard>

    <!-- 事件详情Modal -->
    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="事件详情"
      style="width: 800px; max-width: 90vw"
      :bordered="false"
      size="large"
    >
      <div v-if="selectedEvent" class="space-y-6">
        <!-- 基本信息 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="TimeOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">基本信息</h3>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="TimeOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">发生时间</p>
                <p class="text-base font-semibold text-text">{{ selectedEvent.timestamp }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon 
                :component="getStatusInfo(selectedEvent).icon" 
                size="20" 
                :class="{
                  'text-success': getStatusInfo(selectedEvent).type === 'success',
                  'text-warning': getStatusInfo(selectedEvent).type === 'warning',
                  'text-error': getStatusInfo(selectedEvent).type === 'error',
                  'text-primary': getStatusInfo(selectedEvent).type === 'default',
                  'mt-0.5 flex-shrink-0': true
                }"
              />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">事件状态</p>
                <NTag
                  size="small"
                  :type="getStatusInfo(selectedEvent).type"
                >
                  {{ getStatusInfo(selectedEvent).text }}
                </NTag>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">药品名称</p>
                <p class="text-base font-semibold text-text">{{ selectedEvent.medicineName }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">所属计划</p>
                <p class="text-base font-semibold text-text">{{ selectedEvent.planName }}</p>
              </div>
            </div>
          </div>
        </NCard>

        <!-- 事件详情 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="ImageOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">{{ getDetailSectionTitle(selectedEvent) }}</h3>
          </div>
          <div class="space-y-4">
            <!-- 检测录像（后端落盘于 /uploads/videos） -->
            <div v-if="shouldShowDetectionMedia(selectedEvent) && selectedEvent.videoUrl" class="flex flex-col gap-2">
              <p class="text-sm text-muted">检测录像</p>
              <video
                :src="selectedEvent.videoUrl"
                class="w-full max-w-lg rounded-[20px] border border-line/70 bg-black"
                controls
                playsinline
                preload="metadata"
              />
            </div>
            <!-- 事件图片 -->
            <div v-if="shouldShowDetectionMedia(selectedEvent) && selectedEvent.imageUrl" class="flex flex-col gap-2">
              <p class="text-sm text-muted">事件截图</p>
              <img
                :src="selectedEvent.imageUrl"
                alt="事件截图"
                class="w-full max-w-md rounded-[20px] object-contain border border-line/70 bg-white p-2"
                style="max-height: 400px;"
              />
            </div>
            <div
              v-if="shouldShowDetectionMedia(selectedEvent) && !selectedEvent.videoUrl && !selectedEvent.imageUrl"
              class="text-center py-8"
            >
              <NEmpty description="暂无图片与录像" size="small" />
            </div>
            <div
              v-else-if="!shouldShowDetectionMedia(selectedEvent)"
              class="rounded-[20px] bg-white p-4 shadow-card"
            >
              <p class="text-sm text-muted mb-2">事件说明</p>
              <p class="text-base text-text">{{ getDetailEmptyDescription(selectedEvent) }}</p>
            </div>

            <!-- 目标检测结果 -->
            <div v-if="shouldShowTargets(selectedEvent)" class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted mb-2">目标检测结果</p>
              <p class="text-base text-text">{{ formatTargets(parseTargetsJson(selectedEvent.targetsJson)) }}</p>
              <details class="mt-2">
                <summary class="text-xs text-muted cursor-pointer hover:text-primary">查看原始JSON</summary>
                <pre class="mt-2 max-h-32 overflow-auto rounded-[14px] bg-[#f5f1e8] p-3 text-xs">{{ selectedEvent.targetsJson }}</pre>
              </details>
            </div>

            <!-- 动作描述 -->
            <div v-if="shouldShowActionBlock(selectedEvent)" class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted mb-2">
                {{ selectedEvent.eventType === 'detection_completed' ? '动作检测' : '处理动作' }}
              </p>
              <p class="text-base text-text">{{ getActionLabel(selectedEvent) }}</p>
            </div>
          </div>
        </NCard>

        <!-- 确认信息 -->
        <NCard v-if="selectedEvent.eventType === 'review_decided'" class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="CheckmarkCircleOutline" size="20" class="text-success" />
            <h3 class="text-lg font-semibold text-text">确认信息</h3>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div v-if="selectedEvent.confirmedBy" class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">确认人</p>
                <p class="text-base font-semibold text-text">{{ selectedEvent.confirmedBy }}</p>
              </div>
            </div>
            <div v-if="selectedEvent.confirmedAt" class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">确认时间</p>
                <p class="text-base font-semibold text-text">
                  {{ formatDateTime(selectedEvent.confirmedAt) }}
                </p>
              </div>
            </div>
          </div>
        </NCard>

        <!-- 操作按钮 -->
        <div class="flex justify-end gap-3">
          <NButton @click="showDetailModal = false">关闭</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>

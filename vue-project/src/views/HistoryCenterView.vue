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
import { confirmIntakeEvent, fetchHistoryEvents, fetchHistorySummary } from '@/services/historyService'
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
const confirmingEventId = ref<string | null>(null)

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

/**
 * getStatusInfo 获取状态信息
 */
const getStatusInfo = (status: string) => {
  switch (status) {
    case 'confirmed':
      return { text: '已确认', type: 'success', icon: CheckmarkCircleOutline }
    case 'suspected':
      return { text: '待确认', type: 'warning', icon: WarningOutline }
    case 'abnormal':
      return { text: '异常', type: 'error', icon: CloseCircleOutline }
    default:
      return { text: '未知', type: 'default', icon: TimeOutline }
  }
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

const handleConfirmEvent = async (eventId: string) => {
  confirmingEventId.value = eventId
  try {
    await confirmIntakeEvent(eventId)
    message.success('事件已确认')
    await loadHistory()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '手动确认失败')
    message.error(errorMsg)
    logError(error, '确认服药事件')
  } finally {
    confirmingEventId.value = null
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
                  :label="
                    event.status === 'abnormal'
                      ? '异常'
                      : event.status === 'suspected'
                        ? '待确认'
                        : '已确认'
                  "
                  :tone="
                    event.status === 'abnormal'
                      ? 'danger'
                      : event.status === 'suspected'
                        ? 'warning'
                        : 'success'
                  "
                />
              </div>
              <p class="text-sm leading-6 text-muted">{{ event.planName }}</p>
              <p class="text-sm leading-6 text-muted">{{ event.action }}</p>
              <p class="text-sm text-muted">{{ event.timestamp }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <NButton
                v-if="event.status !== 'confirmed'"
                size="small"
                tertiary
                :loading="confirmingEventId === event.id"
                @click.stop="handleConfirmEvent(event.id)"
              >
                手动确认
              </NButton>
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
                :component="getStatusInfo(selectedEvent.status).icon" 
                size="20" 
                :class="{
                  'text-success': getStatusInfo(selectedEvent.status).type === 'success',
                  'text-warning': getStatusInfo(selectedEvent.status).type === 'warning',
                  'text-error': getStatusInfo(selectedEvent.status).type === 'error',
                  'text-primary': getStatusInfo(selectedEvent.status).type === 'default',
                  'mt-0.5 flex-shrink-0': true
                }"
              />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">事件状态</p>
                <NTag
                  size="small"
                  :type="getStatusInfo(selectedEvent.status).type"
                >
                  {{ getStatusInfo(selectedEvent.status).text }}
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

        <!-- 检测结果 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="ImageOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">检测结果</h3>
          </div>
          <div class="space-y-4">
            <!-- 检测录像（后端落盘于 /uploads/videos） -->
            <div v-if="selectedEvent.videoUrl" class="flex flex-col gap-2">
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
            <div v-if="selectedEvent.imageUrl" class="flex flex-col gap-2">
              <p class="text-sm text-muted">事件截图</p>
              <img
                :src="selectedEvent.imageUrl"
                alt="事件截图"
                class="w-full max-w-md rounded-[20px] object-contain border border-line/70 bg-white p-2"
                style="max-height: 400px;"
              />
            </div>
            <div v-if="!selectedEvent.videoUrl && !selectedEvent.imageUrl" class="text-center py-8">
              <NEmpty description="暂无图片与录像" size="small" />
            </div>

            <!-- 目标检测结果 -->
            <div v-if="selectedEvent.targetsJson" class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted mb-2">目标检测结果</p>
              <p class="text-base text-text">{{ formatTargets(parseTargetsJson(selectedEvent.targetsJson)) }}</p>
              <details class="mt-2">
                <summary class="text-xs text-muted cursor-pointer hover:text-primary">查看原始JSON</summary>
                <pre class="mt-2 max-h-32 overflow-auto rounded-[14px] bg-[#f5f1e8] p-3 text-xs">{{ selectedEvent.targetsJson }}</pre>
              </details>
            </div>

            <!-- 动作描述 -->
            <div v-if="selectedEvent.rawAction" class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted mb-2">动作检测</p>
              <p class="text-base text-text">{{ selectedEvent.rawAction }}</p>
            </div>
          </div>
        </NCard>

        <!-- 确认信息 -->
        <NCard v-if="selectedEvent.status === 'confirmed'" class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
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
          <NButton
            v-if="selectedEvent.status !== 'confirmed'"
            type="primary"
            :loading="confirmingEventId === selectedEvent.id"
            @click="handleConfirmEvent(selectedEvent.id)"
          >
            手动确认
          </NButton>
          <NButton @click="showDetailModal = false">关闭</NButton>
        </div>
      </div>
    </NModal>
  </div>
</template>

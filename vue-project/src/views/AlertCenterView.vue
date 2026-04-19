<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import {
  AlertCircleOutline,
  TimeOutline,
  CheckmarkCircleOutline,
  InformationCircleOutline,
} from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { AlertItem, AlertSeverity } from '@/types/alert'
import { fetchAlerts, resolveAlert } from '@/services/alertService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import { usePatientStore } from '@/stores/patient'

/** severityFilter 控制告警筛选。 */
const severityFilter = ref<'all' | AlertSeverity>('all')
/** alerts 保存告警列表。 */
const alerts = ref<AlertItem[]>([])
/** loading 标记数据加载。 */
const loading = ref(false)
const message = useMessage()
const patientStore = usePatientStore()

const severityLabel: Record<AlertSeverity, string> = {
  info: '一般',
  warning: '重要',
  danger: '紧急',
}

const severityTagType: Record<AlertSeverity, 'info' | 'warning' | 'error'> = {
  info: 'info',
  warning: 'warning',
  danger: 'error',
}

const filteredAlerts = computed(() => {
  if (severityFilter.value === 'all') {
    return alerts.value
  }
  return alerts.value.filter(alert => alert.severity === severityFilter.value)
})

const unresolvedCount = computed(() => alerts.value.filter(alert => !alert.resolved).length)
const resolvedCount = computed(() => alerts.value.filter(alert => alert.resolved).length)
const dangerCount = computed(() => alerts.value.filter(alert => alert.severity === 'danger').length)

const loadAlerts = async () => {
  loading.value = true
  try {
    alerts.value = await fetchAlerts()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载告警列表失败')
    message.error(errorMsg)
    logError(error, '加载告警列表')
  } finally {
    loading.value = false
  }
}

const handleResolve = async (alert: AlertItem) => {
  try {
    await resolveAlert(alert.id)
    message.success('已标记为已处理')
    // 重新加载列表以获取最新状态
    await loadAlerts()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '标记告警失败')
    message.error(errorMsg)
    logError(error, '标记告警已处理')
  }
}

// 告警详情 Modal 相关状态
const showDetailModal = ref(false)
const selectedAlert = ref<AlertItem | null>(null)

/**
 * handleViewDetail 查看告警详情
 */
const handleViewDetail = (alert: AlertItem) => {
  selectedAlert.value = alert
  showDetailModal.value = true
}

/**
 * formatDateTime 格式化日期时间显示（用于详情Modal）
 * 如果传入的是 ISO8601 格式字符串，则格式化；否则直接返回
 */
const formatDateTime = (dateStr: string | null | undefined): string => {
  if (!dateStr) return '--'
  try {
    // 尝试解析 ISO8601 格式（包含 T 和 Z）
    if (dateStr.includes('T') || dateStr.includes('Z')) {
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    }
    // 如果已经是格式化后的字符串，直接返回
    return dateStr
  } catch {
    return dateStr
  }
}

/**
 * getTypeLabel 获取告警类型的中文标签
 */
const getTypeLabel = (type: string | undefined): string => {
  const typeMap: Record<string, string> = {
    timeout: '超时未确认',
    detection_failed: '检测失败',
    missed: '漏服',
  }
  return typeMap[type || ''] || type || '未知类型'
}

/**
 * escapeCsvValue 转义 CSV 值中的特殊字符
 */
const escapeCsvValue = (value: string | null | undefined): string => {
  if (!value) return ''
  // 如果包含逗号、引号或换行符，需要用引号包裹，并转义引号
  if (value.includes(',') || value.includes('"') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

/**
 * handleExportCsv 导出告警数据为 CSV 文件
 */
const handleExportCsv = () => {
  if (filteredAlerts.value.length === 0) {
    message.warning('没有可导出的告警数据')
    return
  }

  try {
    // CSV 表头
    const headers = [
      '告警ID',
      '告警标题',
      '告警类型',
      '详细描述',
      '关联患者',
      '发生时间',
      '严重程度',
      '处理状态',
      '处理时间',
      '处理备注',
      '处理建议',
    ]

    // 构建 CSV 内容
    const csvRows: string[] = []
    
    // 添加 BOM 以支持 Excel 正确显示中文
    csvRows.push('\uFEFF' + headers.join(','))

    // 添加数据行
    filteredAlerts.value.forEach(alert => {
      const row = [
        escapeCsvValue(alert.id),
        escapeCsvValue(alert.title),
        escapeCsvValue(getTypeLabel(alert.type)),
        escapeCsvValue(alert.description),
        escapeCsvValue(alert.patient),
        escapeCsvValue(alert.occurredAt),
        escapeCsvValue(severityLabel[alert.severity]),
        escapeCsvValue(alert.resolved ? '已处理' : '待处理'),
        escapeCsvValue(alert.resolvedAt ? formatDateTime(alert.resolvedAt) : ''),
        escapeCsvValue(alert.actionNote || ''),
        escapeCsvValue(alert.suggestion),
      ]
      csvRows.push(row.join(','))
    })

    const csvContent = csvRows.join('\n')

    // 创建 Blob 对象
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    
    // 生成文件名（包含当前日期）
    const now = new Date()
    const dateStr = now.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).replace(/\//g, '-')
    const fileName = `告警记录_${dateStr}.csv`

    // 创建下载链接
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', fileName)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    message.success(`已导出 ${filteredAlerts.value.length} 条告警记录`)
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '导出 CSV 失败')
    message.error(errorMsg)
    logError(error, '导出告警 CSV')
  }
}

// 监听患者切换，自动刷新告警列表（护工/子女角色）
watch(
  () => patientStore.activePatientId,
  () => {
    loadAlerts()
  }
)

onMounted(loadAlerts)
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Alert center"
      title="异常与告警"
      :description="`只展示已经落库的真实告警实体，并按照严重程度筛选、导出和处理。当前未处理 ${unresolvedCount} 条。`"
      tone="warning"
    >
      <template #meta>
        <StatusBadge :label="`紧急 ${dangerCount}`" :tone="dangerCount > 0 ? 'danger' : 'neutral'" />
        <StatusBadge :label="`待处理 ${unresolvedCount}`" :tone="unresolvedCount > 0 ? 'warning' : 'neutral'" />
        <StatusBadge :label="`已处理 ${resolvedCount}`" tone="success" />
      </template>
      <template #actions>
        <div class="flex flex-wrap items-center gap-3">
          <NRadioGroup v-model:value="severityFilter" size="small">
            <NRadioButton value="all">全部</NRadioButton>
            <NRadioButton value="danger">紧急</NRadioButton>
            <NRadioButton value="warning">重要</NRadioButton>
            <NRadioButton value="info">一般</NRadioButton>
          </NRadioGroup>
          <NButton tertiary size="large" @click="handleExportCsv" :disabled="filteredAlerts.length === 0">
            导出 CSV
          </NButton>
        </div>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard label="全部告警" :value="alerts.length" helper="当前数据源中已同步的告警总数。" tone="neutral" />
      <MetricCard label="待处理告警" :value="unresolvedCount" helper="优先关注未处理且高严重度的告警。" :tone="unresolvedCount > 0 ? 'warning' : 'neutral'" />
      <MetricCard label="紧急级别" :value="dangerCount" helper="严重级别为 danger 的告警数量。" :tone="dangerCount > 0 ? 'danger' : 'neutral'" />
    </section>

    <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
            Alert list
          </p>
          <h3 class="mt-2 text-2xl font-semibold text-text">实时异常</h3>
        </div>
        <StatusBadge :label="`筛选后 ${filteredAlerts.length} 条`" tone="info" />
      </div>

      <div class="mt-5 space-y-4">
        <NSkeleton v-if="loading" text :repeat="4" />
        <NEmpty v-else-if="filteredAlerts.length === 0" description="暂无线上异常" />
        <article
          v-else
          v-for="alert in filteredAlerts"
          :key="alert.id"
          class="rounded-[26px] border p-5 shadow-card"
          :class="
            alert.resolved
              ? 'border-line/70 bg-[#f7f3eb]'
              : alert.severity === 'danger'
                ? 'border-danger/18 bg-danger/5'
                : alert.severity === 'warning'
                  ? 'border-warning/20 bg-warning/8'
                  : 'border-info/18 bg-info/6'
          "
        >
          <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div class="space-y-3">
              <div class="flex flex-wrap items-center gap-3">
                <NBadge :dot="!alert.resolved" :type="alert.resolved ? 'default' : 'error'">
                  <h4 class="text-xl font-semibold text-text" :class="alert.resolved ? 'text-muted line-through' : ''">
                    {{ alert.title }}
                  </h4>
                </NBadge>
                <StatusBadge
                  :label="severityLabel[alert.severity]"
                  :tone="alert.severity === 'danger' ? 'danger' : alert.severity === 'warning' ? 'warning' : 'info'"
                />
                <StatusBadge :label="alert.resolved ? '已处理' : '待处理'" :tone="alert.resolved ? 'success' : 'warning'" />
              </div>
              <div class="grid gap-2 text-sm text-muted md:grid-cols-2">
                <p>发生时间：{{ alert.occurredAt }}</p>
                <p>关联患者：{{ alert.patient }}</p>
                <p v-if="alert.medicineName" class="md:col-span-2">关联药品：{{ alert.medicineName }}</p>
              </div>
              <p class="text-sm leading-7 text-text/90">{{ alert.description }}</p>
            </div>
            <div class="flex flex-wrap gap-3">
              <NButton tertiary size="small" @click="handleViewDetail(alert)">查看详情</NButton>
              <NButton v-if="!alert.resolved" size="small" type="primary" ghost @click="handleResolve(alert)">
                标记已处理
              </NButton>
            </div>
          </div>

          <div class="mt-4 rounded-[20px] border border-white/80 bg-white/80 p-4">
            <p class="text-sm font-semibold text-text">处理建议</p>
            <p class="mt-2 text-sm leading-7 text-muted">{{ alert.suggestion }}</p>
          </div>
        </article>
      </div>
    </NCard>

    <!-- 告警详情 Modal -->
    <NModal
      v-model:show="showDetailModal"
      preset="card"
      title="告警详情"
      style="width: 700px; max-width: 90vw"
      :bordered="false"
      size="large"
    >
      <div v-if="selectedAlert" class="space-y-6">
        <!-- 基本信息 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="InformationCircleOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">基本信息</h3>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="AlertCircleOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">告警标题</p>
                <p class="text-base font-semibold text-text">{{ selectedAlert.title }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="InformationCircleOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">告警类型</p>
                <p class="text-base font-semibold text-text">{{ getTypeLabel(selectedAlert.type) }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="TimeOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">发生时间</p>
                <p class="text-base font-semibold text-text">{{ formatDateTime(selectedAlert.occurredAt) }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="AlertCircleOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">严重程度</p>
                <NTag :type="severityTagType[selectedAlert.severity]" size="small">
                  {{ severityLabel[selectedAlert.severity] }}
                </NTag>
              </div>
            </div>
            <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="InformationCircleOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">关联患者</p>
                <p class="text-base font-semibold text-text">{{ selectedAlert.patient }}</p>
              </div>
            </div>
            <div v-if="selectedAlert.medicineName" class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
              <NIcon :component="InformationCircleOutline" size="20" class="text-primary mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-xs text-muted mb-1">关联药品</p>
                <p class="text-base font-semibold text-text">{{ selectedAlert.medicineName }}</p>
              </div>
            </div>
          </div>
        </NCard>

        <!-- 详细描述 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="InformationCircleOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">详细描述</h3>
          </div>
          <div class="rounded-[20px] bg-white p-4 shadow-card">
            <p class="text-base text-text leading-relaxed">{{ selectedAlert.description }}</p>
          </div>
        </NCard>

        <!-- 处理建议 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="InformationCircleOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">处理建议</h3>
          </div>
          <NAlert :type="severityTagType[selectedAlert.severity]" :show-icon="true">
            {{ selectedAlert.suggestion }}
          </NAlert>
        </NCard>

        <!-- 处理状态 -->
        <NCard class="border-line/70 bg-[#fffcf6] shadow-card" :bordered="false">
          <div class="flex items-center gap-2 mb-4">
            <NIcon :component="CheckmarkCircleOutline" size="20" class="text-primary" />
            <h3 class="text-lg font-semibold text-text">处理状态</h3>
          </div>
          <div class="space-y-3">
            <div class="flex items-center gap-3">
              <NTag :type="selectedAlert.resolved ? 'success' : 'warning'" size="medium">
                {{ selectedAlert.resolved ? '已处理' : '待处理' }}
              </NTag>
            </div>
            <div v-if="selectedAlert.resolved" class="space-y-2">
              <div class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
                <NIcon :component="TimeOutline" size="18" class="text-muted mt-0.5 flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <p class="text-xs text-muted mb-1">处理时间</p>
                  <p class="text-sm font-semibold text-text">
                    {{ formatDateTime(selectedAlert.resolvedAt) }}
                  </p>
                </div>
              </div>
              <div v-if="selectedAlert.actionNote" class="flex items-start gap-3 rounded-[20px] bg-white p-4 shadow-card">
                <NIcon :component="InformationCircleOutline" size="18" class="text-muted mt-0.5 flex-shrink-0" />
                <div class="flex-1 min-w-0">
                  <p class="text-xs text-muted mb-1">处理备注</p>
                  <p class="text-sm text-text">{{ selectedAlert.actionNote }}</p>
                </div>
              </div>
            </div>
          </div>
        </NCard>
      </div>
    </NModal>
  </div>
</template>

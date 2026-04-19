import http from '@/lib/http'
import type { HistoryEvent, HistoryRange, HistorySummary } from '@/types/history'
import { useAuthStore } from '@/stores/auth'
import { usePatientStore } from '@/stores/patient'
import { appConfig } from '@/config/env'

export type { HistoryEvent } from '@/types/history'

type EventStatus = 'suspected' | 'confirmed' | 'abnormal'

interface ApiIntakeEvent {
  id: number
  patientId: number
  scheduleId: number
  scheduleName?: string
  medicineName?: string
  ts: string
  status: EventStatus
  action?: string
  targetsJson?: string
  imgUrl?: string
  videoUrl?: string
  confirmedBy?: string
  confirmedAt?: string
}

interface ApiReportSummary {
  patientId: number
  range: string
  totalReminders: number
  confirmedCount: number
  confirmRate: number
  avgResponseTime: number
  abnormalCount: number
  missedCount: number
}

export interface CreateIntakeEventPayload {
  scheduleId: number
  status: EventStatus
  action?: string
  targets?: Record<string, number> | string
  imageUrl?: string
  timestamp?: string
  patientId?: number
}

export interface ConfirmIntakeEventPayload {
  confirmedBy?: string
  confirmTime?: string
}

function getCurrentPatientId(): number {
  const authStore = useAuthStore()
  const patientStore = usePatientStore()

  if (authStore.role === 'elder' && authStore.user?.id) {
    return Number(authStore.user.id)
  }

  if (patientStore.activePatientId) {
    return Number(patientStore.activePatientId)
  }

  throw new Error('无法获取患者ID，请先选择患者或登录')
}

function formatTimestamp(ts: string): string {
  if (!ts) return '--'
  try {
    const date = new Date(ts)
    if (Number.isNaN(date.getTime())) {
      return ts
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return ts
  }
}

function resolveImageUrl(url?: string): string | undefined {
  if (!url) return undefined
  // 如果已经是完整的HTTP/HTTPS URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  const base = (appConfig.uploadBaseUrl ?? '').replace(/\/$/, '')

  // 后端已返回站点根相对路径（如 /uploads/videos/job-1.webm），避免与 base 拼成 /uploads/uploads/...
  if (url.startsWith('/uploads/')) {
    if (base.endsWith('/uploads')) {
      const origin = base.slice(0, -'/uploads'.length)
      return `${origin}${url}`
    }
    return url
  }
  if (url.startsWith('/logs/')) {
    return url
  }

  // 如果URL以 / 开头，直接拼接base和url
  if (url.startsWith('/')) {
    return `${base}${url}`
  }

  // 如果URL已经是 uploads/ 开头，直接拼接base和url
  if (url.startsWith('uploads/')) {
    return `${base}/${url}`
  }

  // 其他情况，拼接base和url
  return `${base}/${url}`
}

function mapApiEventToHistoryEvent(event: ApiIntakeEvent): HistoryEvent {
  const medicineName = event.medicineName ?? event.scheduleName ?? `计划 #${event.scheduleId}`
  const planName = event.scheduleName ?? event.medicineName ?? `计划 #${event.scheduleId}`
  const confirmedMessage =
    event.confirmedBy && event.confirmedAt
      ? `${event.confirmedBy} · ${formatTimestamp(event.confirmedAt)}`
      : event.confirmedBy
        ? `${event.confirmedBy} 已确认`
        : null

  return {
    id: String(event.id),
    timestamp: formatTimestamp(event.ts),
    medicineName,
    planName,
    status: event.status,
    action: confirmedMessage ?? event.action ?? '等待确认',
    imageUrl: resolveImageUrl(event.imgUrl),
    videoUrl: resolveImageUrl(event.videoUrl),
    scheduleId: event.scheduleId,
    patientId: event.patientId,
    targetsJson: event.targetsJson,
    confirmedBy: event.confirmedBy,
    confirmedAt: event.confirmedAt,
    rawAction: event.action,
  }
}

function formatResponseDuration(seconds?: number): string {
  if (!seconds || seconds <= 0) {
    return '暂无数据'
  }
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  if (mins === 0) {
    return `${secs} 秒`
  }
  if (secs === 0) {
    return `${mins} 分钟`
  }
  return `${mins} 分 ${secs} 秒`
}

function mapSummaryToCards(summary: ApiReportSummary): HistorySummary[] {
  const confirmRatePercent = Math.round((summary.confirmRate ?? 0) * 100)
  return [
    {
      label: '提醒次数',
      value: summary.totalReminders ?? 0,
      unit: '次',
      description: `已确认 ${summary.confirmedCount ?? 0} 次`,
    },
    {
      label: '确认率',
      value: confirmRatePercent,
      unit: '%',
      description: `平均响应 ${formatResponseDuration(summary.avgResponseTime)}`,
    },
    {
      label: '异常 / 漏服',
      value: (summary.abnormalCount ?? 0) + (summary.missedCount ?? 0),
      unit: '次',
      description: `异常 ${summary.abnormalCount ?? 0} · 漏服 ${summary.missedCount ?? 0}`,
    },
  ]
}

/**
 * fetchHistoryEvents 获取当前患者的服药事件列表
 * @param range 时间范围
 * @returns 服药事件列表
 */
export async function fetchHistoryEvents(range: HistoryRange): Promise<HistoryEvent[]> {
  const patientId = getCurrentPatientId()
  const events: ApiIntakeEvent[] = await http.get('/intake-events', {
    params: {
      patientId,
      range,
    },
  })
  return events.map(mapApiEventToHistoryEvent)
}

/**
 * fetchHistoryEventsByPatientId 根据患者ID获取服药事件列表
 * @param patientId 患者ID
 * @param range 时间范围，默认为 'day'
 * @returns 服药事件列表
 */
export async function fetchHistoryEventsByPatientId(
  patientId: number,
  range: HistoryRange = 'day',
): Promise<HistoryEvent[]> {
  const events: ApiIntakeEvent[] = await http.get('/intake-events', {
    params: {
      patientId,
      range,
    },
  })
  return events.map(mapApiEventToHistoryEvent)
}

export async function fetchHistorySummary(range: HistoryRange): Promise<HistorySummary[]> {
  const patientId = getCurrentPatientId()
  const summary: ApiReportSummary | null = await http.get('/reports/summary', {
    params: {
      patientId,
      range,
    },
  })

  if (!summary) {
    return mapSummaryToCards({
      patientId,
      range,
      totalReminders: 0,
      confirmedCount: 0,
      confirmRate: 0,
      avgResponseTime: 0,
      abnormalCount: 0,
      missedCount: 0,
    })
  }

  return mapSummaryToCards(summary)
}

export async function createIntakeEvent(payload: CreateIntakeEventPayload): Promise<HistoryEvent> {
  const patientId = payload.patientId ?? getCurrentPatientId()
  const requestBody = {
    patientId,
    scheduleId: payload.scheduleId,
    ts: payload.timestamp ?? new Date().toISOString(),
    status: payload.status,
    action: payload.action,
    targetsJson:
      typeof payload.targets === 'string'
        ? payload.targets
        : payload.targets
          ? JSON.stringify(payload.targets)
          : undefined,
    imgUrl: payload.imageUrl,
  }

  const event: ApiIntakeEvent = await http.post('/intake-events', requestBody)
  return mapApiEventToHistoryEvent(event)
}

export async function confirmIntakeEvent(
  eventId: string | number,
  payload?: ConfirmIntakeEventPayload,
): Promise<HistoryEvent> {
  const authStore = useAuthStore()
  const body = {
    confirmedBy: payload?.confirmedBy ?? authStore.user?.name ?? authStore.user?.id ?? 'unknown',
    confirmTime: payload?.confirmTime ?? new Date().toISOString(),
  }
  const event: ApiIntakeEvent = await http.post(`/intake-events/${eventId}/confirm`, body)
  return mapApiEventToHistoryEvent(event)
}

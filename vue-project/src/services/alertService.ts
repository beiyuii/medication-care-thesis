import http from '@/lib/http'
import { useAuthStore } from '@/stores/auth'
import { usePatientStore } from '@/stores/patient'
import type { AlertItem, AlertSeverity } from '@/types/alert'

export type { AlertItem } from '@/types/alert'

/**
 * ApiAlertItem 后端返回的告警数据格式
 */
interface ApiAlertItem {
  id: number
  patientId: number
  title: string
  description: string
  severity: 'high' | 'medium' | 'low'
  type: string
  ts: string
  status: 'pending' | 'resolved'
  resolvedAt: string | null
  actionNote: string | null
}

/**
 * ResolveAlertRequest 标记告警已处理的请求参数
 */
interface ResolveAlertRequest {
  actionNote?: string
}

/**
 * getCurrentPatientId 获取当前患者ID
 */
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

/**
 * getPatientName 获取患者名称
 */
function getPatientName(): string {
  const authStore = useAuthStore()
  const patientStore = usePatientStore()

  if (authStore.role === 'elder' && authStore.user?.name) {
    return authStore.user.name
  }

  const activePatient = patientStore.activePatient
  if (activePatient) {
    return activePatient.name
  }

  return '未知患者'
}

/**
 * formatTimestamp 格式化时间戳为可读格式
 */
function formatTimestamp(ts: string): string {
  if (!ts) return '--'
  try {
    const date = new Date(ts)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return ts
  }
}

/**
 * mapSeverity 将后端严重程度映射为前端格式
 */
function mapSeverity(severity: 'high' | 'medium' | 'low'): AlertSeverity {
  const map: Record<'high' | 'medium' | 'low', AlertSeverity> = {
    high: 'danger',
    medium: 'warning',
    low: 'info',
  }
  return map[severity] || 'info'
}

/**
 * extractMedicineName 从描述中提取药品名称
 */
function extractMedicineName(description: string, title: string): string | undefined {
  // 尝试从描述中提取药品名称
  // 常见的模式：药品名称 + 描述，例如"降压药用药时间窗结束仍未确认服药"
  const patterns = [
    /^([^用]+?)(?:用药|未确认|未服药|漏服|异常)/,
    /([^，,。.]+?)(?:用药时间窗|未在规定时间内)/,
    /药品[：:]\s*([^，,。.\n]+)/,
  ]
  
  for (const pattern of patterns) {
    const match = description.match(pattern) || title.match(pattern)
    if (match && match[1]) {
      const medicineName = match[1].trim()
      // 过滤掉常见的非药品名称词汇
      if (medicineName && !['患者', '时间', '规定', '确认'].includes(medicineName)) {
        return medicineName
      }
    }
  }
  
  // 如果描述中包含常见药品名称关键词，尝试提取
  const medicineKeywords = ['降压药', '维生素', '胰岛素', '助眠药', '感冒药', '止痛药']
  for (const keyword of medicineKeywords) {
    if (description.includes(keyword) || title.includes(keyword)) {
      return keyword
    }
  }
  
  return undefined
}

/**
 * generateSuggestion 根据告警类型生成处理建议
 */
function generateSuggestion(type: string): string {
  const suggestionMap: Record<string, string> = {
    timeout: '请致电确认是否已服药',
    detection_failed: '请检查摄像头和网络连接',
    missed: '请关注患者是否漏服，必要时联系家属',
  }
  return suggestionMap[type] || '请及时处理此异常'
}

/**
 * mapApiAlertToLocal 将后端返回的告警数据映射为前端使用的格式
 */
function mapApiAlertToLocal(apiAlert: ApiAlertItem): AlertItem {
  // 尝试从描述或标题中提取药品名称
  const medicineName = extractMedicineName(apiAlert.description, apiAlert.title)
  
  return {
    id: String(apiAlert.id),
    title: apiAlert.title,
    description: apiAlert.description,
    patient: getPatientName(),
    occurredAt: formatTimestamp(apiAlert.ts),
    severity: mapSeverity(apiAlert.severity),
    resolved: apiAlert.status === 'resolved',
    suggestion: apiAlert.actionNote || generateSuggestion(apiAlert.type),
    type: apiAlert.type,
    resolvedAt: apiAlert.resolvedAt,
    actionNote: apiAlert.actionNote,
    patientId: apiAlert.patientId,
    medicineName,
  }
}

/**
 * fetchAlerts 获取异常告警列表
 * @param patientId 可选的患者ID，如果不提供则自动获取当前患者ID
 * @returns 返回告警列表
 */
export async function fetchAlerts(patientId?: number): Promise<AlertItem[]> {
  const targetPatientId = patientId ?? getCurrentPatientId()
  if (!targetPatientId) {
    throw new Error('无法获取患者ID，请先选择患者或登录')
  }

  const apiAlerts: ApiAlertItem[] = await http.get('/alerts', {
    params: { patientId: targetPatientId },
  })

  return apiAlerts.map(mapApiAlertToLocal)
}

/**
 * resolveAlert 标记告警已处理
 * @param alertId 告警ID
 * @param actionNote 可选的处理备注
 * @returns 返回更新后的告警对象
 */
export async function resolveAlert(alertId: string, actionNote?: string): Promise<AlertItem> {
  const payload: ResolveAlertRequest = {}
  if (actionNote) {
    payload.actionNote = actionNote
  }

  const apiAlert: ApiAlertItem = await http.post(`/alerts/${alertId}/resolve`, payload)
  return mapApiAlertToLocal(apiAlert)
}

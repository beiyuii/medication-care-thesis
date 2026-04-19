import http from '@/lib/http'
import type { PatientSummary } from '@/types/auth'
import type { HistoryEvent } from '@/types/history'
import { appConfig } from '@/config/env'

export interface DashboardAlert {
  id: number
  patientId: number
  title: string
  description?: string
  severity: 'high' | 'medium' | 'low'
  type: string
  ts: string
  status: string
  resolvedAt?: string | null
  actionNote?: string | null
  reminderInstanceId?: number | null
  detectionJobId?: number | null
}

export interface ReminderInstanceItem {
  id: number
  patientId: number
  scheduleId: number
  scheduledDate: string
  windowStartAt: string
  windowEndAt: string
  status: 'pending' | 'detecting' | 'suspected' | 'confirmed' | 'missed' | 'abnormal' | 'resolved'
  confirmedAt?: string | null
  detectionJobId?: number | null
  lastEventId?: number | null
  medicineName: string
  dose: string
  frequency: string
  activeAlertTitles: string[]
}

export interface ElderDashboardData {
  nextReminder: ReminderInstanceItem | null
  todayInstances: ReminderInstanceItem[]
  activeAlerts: DashboardAlert[]
  completionRate: number
}

export interface CaregiverDashboardData {
  patients: PatientSummary[]
  activePatient: PatientSummary | null
  recentEvents: HistoryEvent[]
  activeAlerts: DashboardAlert[]
  completionRate: number
}

interface ApiDashboardEvent {
  id: number
  patientId: number
  scheduleId: number
  scheduleName?: string
  medicineName?: string
  ts: string
  status: 'suspected' | 'confirmed' | 'abnormal'
  action?: string
  targetsJson?: string
  imgUrl?: string
  confirmedBy?: string
  confirmedAt?: string
}

interface ApiCaregiverDashboardData {
  patients: PatientSummary[]
  activePatient: PatientSummary | null
  recentEvents: ApiDashboardEvent[]
  activeAlerts: DashboardAlert[]
  completionRate?: number
}

function formatTimestamp(ts: string): string {
  if (!ts) return '--'
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
}

function resolveImageUrl(url?: string): string | undefined {
  if (!url) return undefined
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  const base = appConfig.uploadBaseUrl ?? ''
  if (url.startsWith('/')) {
    return `${base}${url}`
  }
  return `${base}/${url}`
}

function mapDashboardEvent(event: ApiDashboardEvent): HistoryEvent {
  const medicineName = event.medicineName ?? event.scheduleName ?? `计划 #${event.scheduleId}`
  const planName = event.scheduleName ?? event.medicineName ?? `计划 #${event.scheduleId}`
  return {
    id: String(event.id),
    timestamp: formatTimestamp(event.ts),
    medicineName,
    planName,
    status: event.status,
    action: event.confirmedBy ?? event.action ?? '等待确认',
    imageUrl: resolveImageUrl(event.imgUrl),
    scheduleId: event.scheduleId,
    patientId: event.patientId,
    targetsJson: event.targetsJson,
    confirmedBy: event.confirmedBy,
    confirmedAt: event.confirmedAt,
    rawAction: event.action,
  }
}

export async function fetchElderDashboard(): Promise<ElderDashboardData> {
  return http.get('/dashboard/elder')
}

export async function fetchCaregiverDashboard(patientId?: number): Promise<CaregiverDashboardData> {
  const data: ApiCaregiverDashboardData = await http.get('/dashboard/caregiver', {
    params: patientId ? { patientId } : undefined,
  })
  return {
    ...data,
    recentEvents: data.recentEvents.map(mapDashboardEvent),
    completionRate: data.completionRate ?? 0,
  }
}

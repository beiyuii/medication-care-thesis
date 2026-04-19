import http from '@/lib/http'
import type { ReminderInstanceItem } from './dashboardService'

interface ConfirmReminderPayload {
  confirmedBy?: string
  confirmTime?: string
}

export async function fetchReminderInstances(
  patientId: number,
  date?: string,
  status?: string,
): Promise<ReminderInstanceItem[]> {
  return http.get('/reminder-instances', {
    params: {
      patientId,
      date,
      status,
    },
  })
}

export async function confirmReminderInstance(
  reminderInstanceId: number,
  payload?: ConfirmReminderPayload,
): Promise<ReminderInstanceItem> {
  return http.post(`/reminder-instances/${reminderInstanceId}/confirm`, payload ?? {})
}

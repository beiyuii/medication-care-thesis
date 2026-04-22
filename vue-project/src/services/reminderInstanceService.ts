import http from '@/lib/http'
import type { ReminderInstanceItem } from './dashboardService'

interface SubmitReminderPayload {
  submittedBy?: string
  submitTime?: string
}

export interface ReviewReminderPayload {
  decision: 'confirmed' | 'rejected' | 'needs_evidence'
  reviewedBy?: string
  reviewTime?: string
  reason?: string
}

interface RequestEvidencePayload {
  requestedBy?: string
  requestTime?: string
  note?: string
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

export async function submitReminderInstance(
  reminderInstanceId: number,
  payload?: SubmitReminderPayload,
): Promise<ReminderInstanceItem> {
  return http.post(`/reminder-instances/${reminderInstanceId}/submit`, payload ?? {})
}

export async function reviewReminderInstance(
  reminderInstanceId: number,
  payload: ReviewReminderPayload,
): Promise<ReminderInstanceItem> {
  return http.post(`/reminder-instances/${reminderInstanceId}/review`, payload)
}

export async function requestReminderEvidence(
  reminderInstanceId: number,
  payload?: RequestEvidencePayload,
): Promise<ReminderInstanceItem> {
  return http.post(`/reminder-instances/${reminderInstanceId}/request-evidence`, payload ?? {})
}

import http from '@/lib/http'

export interface DetectionJob {
  id: number
  patientId: number
  scheduleId: number
  reminderInstanceId: number
  status: 'queued' | 'processing' | 'succeeded' | 'failed'
  resultStatus?: 'suspected' | 'confirmed' | 'abnormal' | null
  confidence?: number | null
  actionDetected?: boolean | null
  targetsJson?: string | null
  latencyMs?: number | null
  errorCode?: string | null
  errorMessage?: string | null
  traceId?: string | null
  startedAt?: string | null
  completedAt?: string | null
}

export interface CreateDetectionJobPayload {
  patientId: number
  reminderInstanceId: number
  videoFile: Blob
  cameraId?: string
  modelVersion?: string
  samplingRate?: number
  maxFrames?: number
}

export async function createDetectionJob(payload: CreateDetectionJobPayload): Promise<DetectionJob> {
  const formData = new FormData()
  formData.append('patientId', String(payload.patientId))
  formData.append('reminderInstanceId', String(payload.reminderInstanceId))
  formData.append('videoFile', payload.videoFile, `detection-${Date.now()}.webm`)
  if (payload.cameraId) {
    formData.append('cameraId', payload.cameraId)
  }
  if (payload.modelVersion) {
    formData.append('modelVersion', payload.modelVersion)
  }
  if (typeof payload.samplingRate === 'number') {
    formData.append('samplingRate', String(payload.samplingRate))
  }
  if (typeof payload.maxFrames === 'number') {
    formData.append('maxFrames', String(payload.maxFrames))
  }
  return http.post('/detection-jobs', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export async function getDetectionJob(id: number): Promise<DetectionJob> {
  return http.get(`/detection-jobs/${id}`)
}

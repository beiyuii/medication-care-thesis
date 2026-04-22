<script setup lang="ts">
import { computed, onActivated, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useMessage } from 'naive-ui'
import {
  RefreshOutline,
  VideocamOutline,
} from '@vicons/ionicons5'
import PageHero from '@/components/ui/PageHero.vue'
import StateCard from '@/components/ui/StateCard.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import StatusBar from '@/components/ui/StatusBar.vue'
import { useCameraDetection } from '@/composables/useCameraDetection'
import { fetchUserSettings } from '@/services/settingsService'
import type { ScheduleItem } from '@/types/plan'
import type { PrivacySetting } from '@/types/settings'
import { useAuthStore } from '@/stores/auth'
import { mapApiPatientToLocal, usePatientStore } from '@/stores/patient'
import { getPatients } from '@/services/patientService'
import {
  createDetectionJob,
  getDetectionJob,
  type DetectionJob,
} from '@/services/detectionJobService'
import { fetchReminderInstances, submitReminderInstance } from '@/services/reminderInstanceService'
import type { ReminderInstanceItem } from '@/services/dashboardService'

type FlowTone = 'brand' | 'success' | 'warning' | 'danger' | 'neutral'

const message = useMessage()
const authStore = useAuthStore()
const patientStore = usePatientStore()

const userPrivacySetting = ref<PrivacySetting | null>(null)

const {
  videoElement,
  canvasElement,
  isStreaming,
  permissionGranted,
  errorMessage,
  startCamera,
  stopCamera,
  getPermissionGuide,
  isRecording,
  recordingDuration,
  videoBlob,
  startRecording,
  stopRecording,
} = useCameraDetection(userPrivacySetting)

const schedules = ref<ScheduleItem[]>([])
const reminderInstances = ref<ReminderInstanceItem[]>([])
const allReminderInstances = ref<ReminderInstanceItem[]>([])
const selectedScheduleId = ref<string | null>(null)
const isCreatingEvent = ref(false)
const isLoadingSchedules = ref(false)
const currentPatientId = ref<number | null>(null)

const isVideoProcessing = ref(false)
const videoProcessingMessage = ref<string | null>(null)
const videoDetectionResult = ref<{
  status: 'suspected' | 'confirmed' | 'abnormal'
  actionDetected: boolean
  targets: Array<{ label: string; score: number }>
  message: string
  targetConfidence?: number | null
  actionConfidence?: number | null
  finalConfidence?: number | null
  reasonCode?:
    | 'clear_intake'
    | 'target_only'
    | 'action_only'
    | 'possible_fake_intake'
    | 'insufficient_evidence'
    | 'no_medication_detected'
    | null
  riskTag?: 'possible_fake_intake' | 'insufficient_evidence' | 'clear_intake' | 'no_target' | null
  confidence?: number | null
  latencyMs?: number | null
  traceId?: string | null
  llmDecisionSource?: 'deepseek_vl' | 'cloud_vlm' | 'fallback_rules' | null
  llmProvider?: string | null
  llmModel?: string | null
  llmFrameCount?: number | null
  frameSummary?: string | null
} | null>(null)
const activeDetectionJob = ref<DetectionJob | null>(null)
const submittedFlowSnapshot = ref<{
  medicineName: string
  windowLabel: string
  submittedAt: string
} | null>(null)
let submittedFeedbackTimer: number | null = null

const detectionState = computed<'idle' | 'suspected' | 'confirmed' | 'abnormal'>(() => {
  if (videoDetectionResult.value?.status) {
    return videoDetectionResult.value.status
  }
  return 'idle'
})

const selectedReminderInstance = computed(() =>
  reminderInstances.value.find(instance => String(instance.scheduleId) === selectedScheduleId.value) ?? null,
)

const executableReviewStatuses = new Set(['not_submitted', 'caregiver_rejected', 'missed'])
const waitingReviewStatuses = new Set([
  'waiting_caregiver',
  'review_timeout',
  'abnormal_pending_review',
  'evidence_required',
  'waiting_caregiver_late',
])

const waitingReviewCount = computed(
  () => allReminderInstances.value.filter(instance => waitingReviewStatuses.has(instance.reviewStatus)).length,
)

const flowStage = computed<
  'prepare' | 'camera_ready' | 'recording' | 'processing' | 'ready_to_submit' | 'submitted' | 'needs_retry'
>(() => {
  if (isVideoProcessing.value) {
    return 'processing'
  }
  if (isRecording.value) {
    return 'recording'
  }
  if (videoDetectionResult.value) {
    return videoDetectionResult.value.status === 'abnormal' ? 'needs_retry' : 'ready_to_submit'
  }
  if (submittedFlowSnapshot.value) {
    return 'submitted'
  }
  if (isStreaming.value) {
    return 'camera_ready'
  }
  return 'prepare'
})

const showPinnedFlowBar = computed(() =>
  ['camera_ready', 'recording', 'processing', 'ready_to_submit', 'submitted', 'needs_retry'].includes(flowStage.value),
)

const currentTaskValue = computed(() => {
  if (flowStage.value === 'submitted' && submittedFlowSnapshot.value) {
    return submittedFlowSnapshot.value.medicineName
  }
  return selectedReminderInstance.value?.medicineName ?? '先选择任务'
})

const stageMeta = computed<{ value: string; helper: string; tone: FlowTone }>(() => {
  switch (flowStage.value) {
    case 'camera_ready':
      return {
        value: '待录制',
        helper: '摄像头已就绪，可以开始录制本次服药过程。',
        tone: 'brand',
      }
    case 'recording':
      return {
        value: '录制中',
        helper: `正在记录服药动作，已录制 ${recordingDuration.value} 秒。`,
        tone: 'warning',
      }
    case 'processing':
      return {
        value: '检测中',
        helper: videoProcessingMessage.value ?? '系统正在分析刚才的录制内容。',
        tone: 'brand',
      }
    case 'ready_to_submit':
      return {
        value: '可提交',
        helper: videoDetectionResult.value?.message ?? '检测已经完成，可以提交给护工审核。',
        tone: detectionState.value === 'confirmed' ? 'success' : 'warning',
      }
    case 'needs_retry':
      return {
        value: '需重检',
        helper: videoDetectionResult.value?.message ?? '本次检测证据不足，建议重新录制。',
        tone: 'danger',
      }
    case 'submitted':
      return {
        value: '已提交',
        helper: '本次记录已提交，等待护工审核确认。',
        tone: 'success',
      }
    default:
      return {
        value: '待准备',
        helper: selectedReminderInstance.value ? '先开启摄像头，再开始录制。' : '请先选择提醒实例。',
        tone: selectedReminderInstance.value ? 'neutral' : 'warning',
      }
  }
})

const nextActionMeta = computed<{ value: string; helper: string; tone: FlowTone }>(() => {
  switch (flowStage.value) {
    case 'prepare':
      return {
        value: '开启摄像头',
        helper: selectedReminderInstance.value ? '先开启摄像头进入录制准备。' : '需先选择提醒实例。',
        tone: selectedReminderInstance.value ? 'brand' : 'warning',
      }
    case 'camera_ready':
      return {
        value: '开始录制',
        helper: '画面准备好后开始录制，不要切换页面。',
        tone: 'brand',
      }
    case 'recording':
      return {
        value: '结束录制并检测',
        helper: '完成服药动作后结束录制，系统会立即进入检测。',
        tone: 'warning',
      }
    case 'processing':
      return {
        value: '等待检测完成',
        helper: '检测处理中，请勿重复点击或重新开始。',
        tone: 'brand',
      }
    case 'ready_to_submit':
      return {
        value: '提交给护工审核',
        helper: '提交后不会直接完成，而是进入护工确认流程。',
        tone: 'success',
      }
    case 'needs_retry':
      return {
        value: '重新检测',
        helper: '建议重新录制一段更清晰的视频，再重新检测。',
        tone: 'danger',
      }
    case 'submitted':
      return {
        value: '等待护工审核',
        helper: '本次提交已完成，暂时无需新的录制动作。',
        tone: 'success',
      }
  }
})

const primaryActionLabel = computed(() => nextActionMeta.value.value)
const primaryActionType = computed<'primary' | 'warning' | 'error' | 'success'>(() => {
  switch (flowStage.value) {
    case 'recording':
      return 'error'
    case 'ready_to_submit':
    case 'submitted':
      return 'success'
    case 'needs_retry':
      return 'warning'
    default:
      return 'primary'
  }
})
const primaryActionDisabled = computed(() => {
  if (flowStage.value === 'prepare') {
    return !selectedReminderInstance.value
  }
  return flowStage.value === 'processing' || flowStage.value === 'submitted'
})
const primaryActionLoading = computed(() => flowStage.value === 'processing' || isCreatingEvent.value)

const showCameraSecondaryAction = computed(
  () =>
    isStreaming.value
    && !isRecording.value
    && !isVideoProcessing.value
    && flowStage.value !== 'submitted',
)
const showRetrySecondaryAction = computed(
  () =>
    Boolean(videoDetectionResult.value)
    && !isRecording.value
    && !isVideoProcessing.value
    && flowStage.value === 'ready_to_submit',
)

const flowToneClassMap: Record<FlowTone, { shell: string; chip: string; badge: string }> = {
  brand: {
    shell: 'border-primary/22 bg-primary/10',
    chip: 'bg-primary text-white',
    badge: 'bg-primary/12 text-primary',
  },
  success: {
    shell: 'border-success/24 bg-success/10',
    chip: 'bg-success text-white',
    badge: 'bg-success/12 text-success',
  },
  warning: {
    shell: 'border-warning/24 bg-warning/12',
    chip: 'bg-warning text-white',
    badge: 'bg-warning/14 text-warning',
  },
  danger: {
    shell: 'border-danger/24 bg-danger/10',
    chip: 'bg-danger text-white',
    badge: 'bg-danger/12 text-danger',
  },
  neutral: {
    shell: 'border-line/80 bg-slate-50/90',
    chip: 'bg-slate-500 text-white',
    badge: 'bg-slate-100 text-muted',
  },
}

const pinnedFlowShellClass = computed(() =>
  showPinnedFlowBar.value
    ? `sticky top-[92px] z-30 rounded-[26px] border bg-[#fbf8f1]/97 px-4 py-3 shadow-[0_18px_56px_rgba(104,153,148,0.16)] backdrop-blur-xl lg:top-[104px] ${
        flowToneClassMap[stageMeta.value.tone].shell
      }`
    : '',
)

const stageBadgeClass = computed(() => flowToneClassMap[stageMeta.value.tone].chip)
const nextActionBadgeClass = computed(() => flowToneClassMap[nextActionMeta.value.tone].badge)
const pinnedTaskBadgeClass = computed(() =>
  selectedReminderInstance.value ? 'bg-primary/12 text-primary' : 'bg-warning/14 text-warning',
)
const pinnedStageIndex = computed(() => {
  if (flowStage.value === 'prepare' || flowStage.value === 'camera_ready') return '准备阶段'
  if (flowStage.value === 'recording') return '第 2 步'
  return '提交阶段'
})
const pinnedStageSummary = computed(() => {
  if (flowStage.value === 'camera_ready') {
    return '摄像头已准备好，当前只需要开始录制。'
  }
  if (flowStage.value === 'submitted') {
    return '本次记录已提交，正在等待护工审核。'
  }
  if (flowStage.value === 'needs_retry') {
    return '这次检测证据不足，顶部状态会保持，直到你重新检测。'
  }
  if (flowStage.value === 'ready_to_submit') {
    return '检测已经结束，当前只剩一个动作：提交给护工审核。'
  }
  if (flowStage.value === 'processing') {
    return '系统正在分析刚才的录制内容，请保持在当前页面等待结果。'
  }
  if (flowStage.value === 'recording') {
    return '正在录制，结束后会自动进入检测，不需要再找别的按钮。'
  }
  return stageMeta.value.helper
})
const pinnedTaskSummary = computed(() => {
  if (!selectedReminderInstance.value) {
    return '先在右侧选择一个今日提醒实例，再开启摄像头进入检测。'
  }
  return `${currentTaskValue.value} · ${toClock(selectedReminderInstance.value.windowStartAt)} - ${toClock(selectedReminderInstance.value.windowEndAt)}`
})
const compactSteps = computed(() => [
  {
    key: 'prepare',
    label: '准备',
    active: flowStage.value === 'prepare' || flowStage.value === 'camera_ready',
    completed: !['prepare', 'camera_ready'].includes(flowStage.value),
  },
  {
    key: 'record',
    label: '录制',
    active: flowStage.value === 'recording',
    completed: ['processing', 'ready_to_submit', 'submitted', 'needs_retry'].includes(flowStage.value),
  },
  {
    key: 'submit',
    label: flowStage.value === 'needs_retry' ? '重检' : '检测/提交',
    active: ['processing', 'ready_to_submit', 'submitted', 'needs_retry'].includes(flowStage.value),
    completed: flowStage.value === 'submitted',
  },
])

const videoStageBanner = computed(() => {
  if (flowStage.value === 'processing') {
    return {
      label: '当前阶段：检测中',
      tone: 'bg-primary/85 text-white',
      message: videoProcessingMessage.value ?? '系统正在分析刚才的录制内容。',
    }
  }
  if (flowStage.value === 'ready_to_submit' || flowStage.value === 'needs_retry') {
    return {
      label:
        flowStage.value === 'needs_retry'
          ? '当前阶段：建议重新检测'
          : detectionState.value === 'confirmed'
            ? '当前阶段：检测通过'
            : '当前阶段：检测完成，待提交',
      tone:
        flowStage.value === 'needs_retry'
          ? 'bg-danger/85 text-white'
          : detectionState.value === 'confirmed'
            ? 'bg-success/85 text-white'
            : 'bg-warning/85 text-white',
      message: videoDetectionResult.value?.message ?? '',
    }
  }
  return null
})

const heroTitle = computed(() => '服药检测流程')

const heroDescription = computed(() => {
  if (isVideoProcessing.value) {
    return videoProcessingMessage.value ?? '检测任务已提交，正在等待后端返回结构化结果。'
  }
  if (waitingReviewCount.value > 0 && flowStage.value !== 'submitted') {
    return '当前有记录正在等待护工审核，老人端暂时不再开放新的录制入口。'
  }
  if (isRecording.value) {
    return '保持药品、手部与面部都在画面中，结束录制后系统会自动进入检测。'
  }
  if (detectionState.value === 'confirmed') {
    return '系统已经识别到本次服药，提交后将进入护工审核。'
  }
  if (detectionState.value === 'suspected') {
    return '系统给出了疑似服药结果，提交后由护工进行最终审核。'
  }
  if (detectionState.value === 'abnormal') {
    return '本次检测结果异常，提交后将由护工决定是否补证或重服。'
  }
  return '按准备、录制、提交三步完成单次检测，不在页面里分散执行多个动作。'
})

const detectionStatusLabel = computed(() => {
  if (isVideoProcessing.value) return '检测中'
  if (detectionState.value === 'confirmed') return '检测通过'
  if (detectionState.value === 'suspected') return '疑似'
  if (detectionState.value === 'abnormal') return '异常'
  return '待开始'
})

const detectionStatusTone = computed(() => {
  if (isVideoProcessing.value) return 'info'
  if (detectionState.value === 'confirmed') return 'success'
  if (detectionState.value === 'suspected') return 'warning'
  if (detectionState.value === 'abnormal') return 'danger'
  return 'neutral'
})

const canSubmit = computed(
  () =>
    flowStage.value === 'ready_to_submit' &&
    !isCreatingEvent.value,
)

const statusBarConfig = computed(() => {
  if (permissionGranted.value === false) {
    return {
      type: 'warning',
      title: '摄像头权限未开启',
      message: errorMessage.value || '请检查浏览器权限设置后重试。',
      guide: getPermissionGuide(),
    }
  }
  if (errorMessage.value) {
    return { type: 'error', title: '检测异常', message: errorMessage.value }
  }
  if (detectionState.value === 'abnormal') {
    return { type: 'error', title: '检测异常', message: videoDetectionResult.value?.message ?? '护工端将禁用快速确认，只能补证或重服。' }
  }
  return null
})
const showInlineStatusBar = computed(() => Boolean(statusBarConfig.value))

const checklist = [
  '药品、手部和面部尽量保持在同一画面内。',
  '录制结束后系统会自动创建 detection job 并轮询结果。',
  '检测完成后由老人提交，最终结果由护工审核。',
]

const toClock = (value?: string | null): string => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getLocalDateString = (date = new Date()): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const mapReminderToSchedule = (instance: ReminderInstanceItem): ScheduleItem => ({
  id: String(instance.scheduleId),
  medicineName: instance.medicineName,
  type: 'PILL',
  dosage: instance.dose,
  frequency: instance.frequency,
  window: {
    start: toClock(instance.windowStartAt),
    end: toClock(instance.windowEndAt),
  },
  period: '每日',
  status: instance.reviewStatus === 'caregiver_confirmed' ? 'completed' : 'active',
  nextIntake: instance.windowStartAt,
})

const parseTargets = (targetsJson?: string | null): Array<{ label: string; score: number }> => {
  if (!targetsJson) return []
  try {
    const parsed = JSON.parse(targetsJson)
    if (Array.isArray(parsed)) {
      return parsed.map(item => ({
        label: String(item.label ?? 'UNKNOWN'),
        score: Number(item.score ?? 0),
      }))
    }
    return Object.entries(parsed).map(([label, score]) => ({
      label,
      score: Number(score ?? 0),
    }))
  } catch {
    return []
  }
}

const buildResultMessage = (job: DetectionJob): string => {
  if (job.status === 'failed') {
    return job.errorMessage ?? '检测任务失败'
  }
  if (job.reasonText) {
    return job.reasonText
  }
  if (job.resultStatus === 'confirmed') return '系统已识别本次服药，请提交给护工审核'
  if (job.resultStatus === 'suspected') return '系统判定为疑似服药，请提交后等待护工审核'
  return '检测结果异常，请重新尝试'
}

const formatPercent = (value?: number | null): string => {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--'
  }
  return `${(value * 100).toFixed(1)}%`
}

const sleep = (ms: number) => new Promise(resolve => window.setTimeout(resolve, ms))

const resolvePatientId = async (): Promise<number> => {
  if (patientStore.activePatientId) {
    return Number(patientStore.activePatientId)
  }
  const patients = await getPatients()
  if (patients.length > 0) {
    patientStore.setPatients(patients.map(mapApiPatientToLocal))
    return Number(patients[0]?.id)
  }
  if (authStore.user?.id) {
    return Number(authStore.user.id)
  }
  throw new Error('无法获取患者信息')
}

const loadSchedules = async () => {
  isLoadingSchedules.value = true
  try {
    currentPatientId.value = await resolvePatientId()
    const instances = await fetchReminderInstances(
      currentPatientId.value,
      getLocalDateString(),
    )
    allReminderInstances.value = instances
    reminderInstances.value = waitingReviewCount.value > 0
      ? []
      : instances.filter(instance => executableReviewStatuses.has(instance.reviewStatus))
    schedules.value = reminderInstances.value.map(mapReminderToSchedule)
    if (schedules.value.length > 0) {
      const preferred =
        reminderInstances.value.find(
          instance =>
            instance.reviewStatus === 'caregiver_rejected'
            || instance.reviewStatus === 'not_submitted',
        ) ??
        reminderInstances.value[0]
      selectedScheduleId.value = preferred ? String(preferred.scheduleId) : null
    } else {
      selectedScheduleId.value = null
      if (waitingReviewCount.value > 0) {
        message.info('当前记录正在等待护工审核，老人端暂时不能继续执行同一条服药任务')
      } else {
        message.warning('今日暂无可执行的提醒实例，请先创建并启用计划')
      }
    }
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '加载提醒实例失败'
    message.error(errorMsg)
    console.error('加载提醒实例失败:', error)
  } finally {
    isLoadingSchedules.value = false
  }
}

const resetDetection = () => {
  videoDetectionResult.value = null
  activeDetectionJob.value = null
  videoProcessingMessage.value = null
  submittedFlowSnapshot.value = null
  if (submittedFeedbackTimer) {
    window.clearTimeout(submittedFeedbackTimer)
    submittedFeedbackTimer = null
  }
}

const handleStartRecording = async () => {
  if (!isStreaming.value) {
    message.warning('请先开启摄像头')
    return
  }
  if (!selectedScheduleId.value) {
    message.warning('请先选择用药计划')
    return
  }

  const success = await startRecording()
  if (success) {
    submittedFlowSnapshot.value = null
    message.success('录制已开始，请按流程完成服药动作')
  } else {
    message.error('开始录像失败，请重试')
  }
}

const handleStopRecordingAndProcess = async () => {
  if (!isRecording.value) return
  if (!selectedScheduleId.value || !selectedReminderInstance.value) {
    message.warning('请先选择用药计划')
    return
  }

  isVideoProcessing.value = true
  videoProcessingMessage.value = '正在停止录制...'

  try {
    const finalBlob = await stopRecording()
    if (!finalBlob || finalBlob.size === 0) {
      message.error('视频数据获取失败，请重试')
      return
    }

    const patientId = currentPatientId.value ?? (await resolvePatientId())
    videoProcessingMessage.value = '正在创建检测任务...'
    const job = await createDetectionJob({
      patientId,
      reminderInstanceId: selectedReminderInstance.value.id,
      videoFile: finalBlob,
      cameraId: 'web-cam',
      modelVersion: 'spring-flask-v1',
      samplingRate: 30,
      maxFrames: 300,
    })
    activeDetectionJob.value = job
    videoProcessingMessage.value = '正在等待检测结果...'

    let latestJob = job
    for (let attempt = 0; attempt < 30; attempt += 1) {
      await sleep(1500)
      latestJob = await getDetectionJob(job.id)
      activeDetectionJob.value = latestJob
      if (latestJob.status === 'succeeded' || latestJob.status === 'failed') {
        break
      }
      videoProcessingMessage.value = `检测进行中（第 ${attempt + 1} 次轮询）...`
    }

    if (latestJob.status !== 'succeeded') {
      throw new Error(latestJob.errorMessage ?? '检测任务未成功完成')
    }

    videoDetectionResult.value = {
      status: latestJob.resultStatus ?? 'abnormal',
      actionDetected: Boolean(latestJob.actionDetected),
      targets: parseTargets(latestJob.targetsJson),
      message: buildResultMessage(latestJob),
      targetConfidence: latestJob.targetConfidence,
      actionConfidence: latestJob.actionConfidence,
      finalConfidence: latestJob.finalConfidence ?? latestJob.confidence,
      reasonCode: latestJob.reasonCode,
      riskTag: latestJob.riskTag,
      confidence: latestJob.confidence,
      latencyMs: latestJob.latencyMs,
      traceId: latestJob.traceId,
      llmDecisionSource: latestJob.llmDecisionSource,
      llmProvider: latestJob.llmProvider,
      llmModel: latestJob.llmModel,
      llmFrameCount: latestJob.llmFrameCount,
      frameSummary: latestJob.frameSummary,
    }

    await loadSchedules()

    if (latestJob.resultStatus === 'confirmed') {
      message.success('检测完成，可以提交给护工审核')
    } else if (latestJob.resultStatus === 'suspected') {
      message.warning('检测结果为疑似服药，请提交给护工审核')
    } else {
      message.error('检测结果异常，可重新录制或提交给护工补证')
    }
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '视频处理失败，请重试'
    console.error('视频处理失败:', error)
    message.error(`处理失败: ${errorMsg}`)
  } finally {
    isVideoProcessing.value = false
    videoProcessingMessage.value = null
  }
}

const handleSubmit = async () => {
  if (!selectedScheduleId.value || !selectedReminderInstance.value) {
    message.warning('请先选择用药计划')
    return
  }
  if (!canSubmit.value) {
    message.warning('请先完成检测，再提交给护工审核')
    return
  }

  isCreatingEvent.value = true
  try {
    const submittedInstance = selectedReminderInstance.value
    await submitReminderInstance(selectedReminderInstance.value.id, {
      submittedBy: authStore.user?.name ?? authStore.user?.id ?? 'elder',
      submitTime: new Date().toISOString(),
    })
    message.success('已提交服药记录，等待护工审核')
    if (submittedInstance) {
      submittedFlowSnapshot.value = {
        medicineName: submittedInstance.medicineName,
        windowLabel: `${toClock(submittedInstance.windowStartAt)} - ${toClock(submittedInstance.windowEndAt)}`,
        submittedAt: new Date().toISOString(),
      }
      if (submittedFeedbackTimer) {
        window.clearTimeout(submittedFeedbackTimer)
      }
      submittedFeedbackTimer = window.setTimeout(() => {
        submittedFlowSnapshot.value = null
        submittedFeedbackTimer = null
      }, 8000)
    }
    videoDetectionResult.value = null
    activeDetectionJob.value = null
    videoBlob.value = null
    await loadSchedules()
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '记录失败，请重试'
    console.error('提交服药失败:', error)
    message.error(`保存失败: ${errorMsg}`)
  } finally {
    isCreatingEvent.value = false
  }
}

const loadUserSettings = async () => {
  try {
    const settings = await fetchUserSettings()
    userPrivacySetting.value = settings.privacy
  } catch (error: unknown) {
    console.error('加载用户设置失败:', error)
    userPrivacySetting.value = {
      cameraPermission: true,
      uploadConsent: true,
      shareToCaregiver: true,
    }
  }
}

/**
 * handleStartCamera 开启摄像头并在失败时用消息提示原因（便于发现应用内开关未打开等情况）。
 */
const handleStartCamera = async () => {
  submittedFlowSnapshot.value = null
  if (waitingReviewCount.value > 0) {
    message.warning('当前记录正在等待护工审核，暂时不能开启新的检测流程')
    return
  }
  await startCamera()
  if (errorMessage.value) {
    message.error(errorMessage.value)
    return
  }
  if (isStreaming.value) {
    message.success('摄像头已开启')
  }
}

const handlePrimaryAction = async () => {
  if (flowStage.value === 'prepare') {
    await handleStartCamera()
    return
  }
  if (flowStage.value === 'camera_ready') {
    await handleStartRecording()
    return
  }
  if (flowStage.value === 'recording') {
    await handleStopRecordingAndProcess()
    return
  }
  if (flowStage.value === 'ready_to_submit') {
    await handleSubmit()
    return
  }
  if (flowStage.value === 'needs_retry') {
    resetDetection()
  }
}

onMounted(() => {
  loadSchedules()
  loadUserSettings()
})

onActivated(() => {
  loadUserSettings()
  loadSchedules()
})

onUnmounted(() => {
  if (submittedFeedbackTimer) {
    window.clearTimeout(submittedFeedbackTimer)
  }
  stopCamera()
})
</script>

<template>
  <div class="space-y-6">
    <StatusBar
      v-if="showInlineStatusBar && statusBarConfig"
      :type="statusBarConfig.type as 'info' | 'success' | 'warning' | 'error'"
      :title="statusBarConfig.title"
      :message="statusBarConfig.message"
    >
      <template #action>
        <div v-if="statusBarConfig.guide" class="max-w-xs text-sm">
          <div class="font-semibold">权限设置指引</div>
          <div class="mt-1 text-muted">{{ statusBarConfig.guide }}</div>
          <RouterLink v-if="userPrivacySetting && !userPrivacySetting.cameraPermission" to="/settings" class="mt-2 block font-semibold text-primary">
            前往设置中心
          </RouterLink>
        </div>
        <RouterLink v-else to="/settings" class="text-sm font-semibold text-primary">
          设置中心
        </RouterLink>
      </template>
    </StatusBar>

    <PageHero eyebrow="Single-task flow" :title="heroTitle" :description="heroDescription" tone="soft">
      <template #meta>
        <StatusBadge
          :label="
            selectedReminderInstance
              ? `${selectedReminderInstance.medicineName} · ${toClock(selectedReminderInstance.windowStartAt)} - ${toClock(selectedReminderInstance.windowEndAt)}`
              : '尚未选择提醒实例'
          "
          :tone="selectedReminderInstance ? 'info' : 'neutral'"
        />
        <StatusBadge :label="`检测状态：${detectionStatusLabel}`" :tone="detectionStatusTone" />
        <StatusBadge
          v-if="videoDetectionResult?.traceId"
          :label="`Trace ${videoDetectionResult.traceId}`"
          tone="neutral"
        />
      </template>
      <template #actions>
        <RouterLink to="/plans">
          <NButton tertiary size="large">返回计划管理</NButton>
        </RouterLink>
      </template>
    </PageHero>

    <section v-if="showPinnedFlowBar" :class="pinnedFlowShellClass">
      <div class="flex flex-col gap-3 xl:flex-row xl:items-center">
        <div class="flex min-w-0 flex-1 items-center gap-3">
          <span class="rounded-pill px-3 py-1 text-xs font-semibold" :class="pinnedTaskBadgeClass">
            {{ selectedReminderInstance ? '当前任务' : '待选任务' }}
          </span>
          <p class="min-w-0 truncate text-base font-semibold text-text">{{ pinnedTaskSummary }}</p>
        </div>
        <div class="flex min-w-0 flex-1 items-center gap-3">
          <span class="rounded-pill bg-white/75 px-3 py-1 text-xs font-semibold text-muted">
            {{ pinnedStageIndex }}
          </span>
          <span class="rounded-pill px-3 py-1 text-sm font-semibold shadow-soft" :class="stageBadgeClass">
            {{ stageMeta.value }}
          </span>
          <p class="min-w-0 flex-1 truncate text-sm text-muted">{{ pinnedStageSummary }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-3 xl:justify-end">
          <span class="rounded-pill px-3 py-1 text-xs font-semibold" :class="nextActionBadgeClass">
            唯一主操作
          </span>
          <NButton
            :type="primaryActionType"
            size="large"
            :disabled="primaryActionDisabled"
            :loading="primaryActionLoading"
            @click="handlePrimaryAction"
          >
            {{ flowStage === 'recording' ? '结束录制并检测' : primaryActionLabel }}
          </NButton>
          <NButton
            v-if="showCameraSecondaryAction"
            tertiary
            size="large"
            @click="stopCamera"
          >
            关闭摄像头
          </NButton>
          <NButton
            v-if="showRetrySecondaryAction"
            quaternary
            size="large"
            @click="resetDetection"
          >
            <template #icon>
              <NIcon :component="RefreshOutline" />
            </template>
            重新检测
          </NButton>
        </div>
      </div>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.45fr_0.95fr]">
      <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
        <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
              Detection stage
            </p>
            <h3 class="mt-2 text-2xl font-semibold text-text">单任务视频舞台</h3>
            <p class="mt-2 text-sm leading-7 text-muted">
              整个检测流程都围绕这个画面展开，先开启摄像头，再录制，再等待结果，最后提交给护工审核。
            </p>
          </div>
          <div v-if="!showPinnedFlowBar" class="flex flex-wrap gap-3">
            <NButton
              :type="primaryActionType"
              size="large"
              :disabled="primaryActionDisabled"
              :loading="primaryActionLoading"
              @click="handlePrimaryAction"
            >
              {{ flowStage === 'recording' ? '结束录制并检测' : primaryActionLabel }}
            </NButton>
            <NButton
              v-if="showCameraSecondaryAction"
              tertiary
              size="large"
              @click="stopCamera"
            >
              关闭摄像头
            </NButton>
            <NButton
              v-if="showRetrySecondaryAction"
              quaternary
              size="large"
              @click="resetDetection"
            >
              <template #icon>
                <NIcon :component="RefreshOutline" />
              </template>
              重新检测
            </NButton>
          </div>
          <div v-else class="rounded-pill bg-primary/8 px-4 py-2 text-sm font-medium text-primary">
            顶部流程条已接管当前操作
          </div>
        </div>

        <div
          class="mt-6 overflow-hidden rounded-[30px] border bg-[#111616] shadow-soft transition-all duration-300"
          :class="isRecording ? 'border-danger/60 ring-4 ring-danger/18' : 'border-line/70'"
        >
          <div class="relative aspect-video">
            <video
              ref="videoElement"
              autoplay
              playsinline
              muted
              class="absolute inset-0 h-full w-full object-cover"
            />
            <div
              v-if="!isStreaming"
              class="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-black/28 px-6 text-center text-white/75"
            >
              <div class="flex h-16 w-16 items-center justify-center rounded-[22px] bg-white/12">
                <NIcon :component="VideocamOutline" size="28" />
              </div>
              <div>
                <p class="text-xl font-semibold text-white">摄像头尚未开启</p>
                <p class="mt-2 text-sm leading-7 text-white/70">
                  选择提醒实例后点击上方主按钮，即可进入录制与检测流程。
                </p>
              </div>
            </div>

            <div
              v-if="isRecording"
              class="absolute left-5 top-5 flex items-center gap-2 rounded-pill bg-danger px-4 py-2 text-sm font-semibold text-white shadow-soft"
            >
              <span class="h-2.5 w-2.5 rounded-full bg-white animate-pulse" />
              录制中 {{ recordingDuration }}s
            </div>

            <div
              v-if="videoStageBanner"
              class="absolute right-5 top-5 max-w-[320px] rounded-[18px] px-4 py-3 text-sm shadow-card backdrop-blur"
              :class="videoStageBanner.tone"
            >
              <p class="font-semibold">{{ videoStageBanner.label }}</p>
              <p class="mt-1 text-xs leading-5 text-white/85">{{ videoStageBanner.message }}</p>
            </div>

            <div
              v-if="selectedReminderInstance"
              class="absolute bottom-5 left-5 rounded-[18px] bg-black/48 px-4 py-3 text-sm text-white shadow-card backdrop-blur"
            >
              <p class="font-semibold">{{ selectedReminderInstance.medicineName }}</p>
              <p class="mt-1 text-white/75">
                {{ toClock(selectedReminderInstance.windowStartAt) }} - {{ toClock(selectedReminderInstance.windowEndAt) }}
              </p>
            </div>

            <div
              v-if="isVideoProcessing"
              class="absolute inset-0 flex items-center justify-center bg-black/55 px-6 text-white"
            >
              <div class="space-y-4 text-center">
                <NSpin size="large" />
                <div class="space-y-1">
                  <p class="text-xl font-semibold">正在处理检测任务</p>
                  <p class="text-sm text-white/70">{{ videoProcessingMessage || '正在处理...' }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap items-center gap-3 rounded-[22px] border border-line/70 bg-[#fffcf6] px-4 py-3">
          <span class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">流程步骤</span>
          <div
            v-for="step in compactSteps"
            :key="step.key"
            class="rounded-pill border px-3 py-1.5 text-sm font-medium transition-colors"
            :class="
              step.active
                ? step.key === 'submit' && flowStage === 'needs_retry'
                  ? 'border-danger/25 bg-danger/10 text-danger'
                  : 'border-primary/25 bg-primary/10 text-primary'
                : step.completed
                  ? 'border-success/22 bg-success/10 text-success'
                  : 'border-line/70 bg-white text-muted'
            "
          >
            {{ step.label }}
          </div>
        </div>

        <div v-if="videoDetectionResult" class="mt-5 rounded-[24px] border border-line/70 bg-[#fffcf6] p-5">
          <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div class="space-y-2">
              <div class="flex items-center gap-3">
                <h4 class="text-lg font-semibold text-text">检测结果</h4>
                <StatusBadge :label="detectionStatusLabel" :tone="detectionStatusTone" />
              </div>
              <p class="text-sm leading-7 text-muted">
                {{ videoDetectionResult.message }}
              </p>
            </div>
            <div class="grid gap-2 text-sm text-muted md:text-right">
              <p v-if="videoDetectionResult.finalConfidence !== undefined && videoDetectionResult.finalConfidence !== null">
                综合判断：{{ formatPercent(videoDetectionResult.finalConfidence) }}
              </p>
              <p v-if="videoDetectionResult.targetConfidence !== undefined && videoDetectionResult.targetConfidence !== null">
                药品证据：{{ formatPercent(videoDetectionResult.targetConfidence) }}
              </p>
              <p v-if="videoDetectionResult.actionConfidence !== undefined && videoDetectionResult.actionConfidence !== null">
                动作证据：{{ formatPercent(videoDetectionResult.actionConfidence) }}
              </p>
              <p v-if="videoDetectionResult.latencyMs !== undefined && videoDetectionResult.latencyMs !== null">
                延迟：{{ videoDetectionResult.latencyMs }} ms
              </p>
            </div>
          </div>
          <div
            v-if="videoDetectionResult.riskTag === 'possible_fake_intake'"
            class="mt-4 rounded-[18px] border border-warning/25 bg-warning/10 px-4 py-3 text-sm text-warning"
          >
            系统检测到“药品证据强、动作证据弱”的组合，存在装作服药或假吃风险，需由护工重点复核。
          </div>
          <div v-if="videoDetectionResult.targets.length > 0" class="mt-4 flex flex-wrap gap-2">
            <StatusBadge
              v-for="target in videoDetectionResult.targets"
              :key="target.label"
              :label="`${target.label} ${Math.round(target.score * 100)}%`"
              tone="neutral"
            />
          </div>
        </div>

        <canvas ref="canvasElement" class="hidden" />
      </NCard>

      <div class="space-y-4">
        <StateCard
          title="提醒实例"
          :status-label="schedules.length === 0 ? (waitingReviewCount > 0 ? '等待护工审核' : '请先创建计划') : '今日可执行'"
          :status-type="schedules.length === 0 ? 'warning' : 'success'"
          :subtitle="waitingReviewCount > 0 ? '已提交的记录会锁定在待审核阶段，老人端不能继续重复执行同一条任务。' : '只针对今日已生成的提醒实例执行检测。'"
        >
          <div v-if="isLoadingSchedules" class="flex items-center gap-2 text-sm text-muted">
            <NSpin size="small" />
            正在加载提醒实例...
          </div>
          <div v-else-if="schedules.length === 0" class="space-y-2 text-sm text-muted">
            <p v-if="waitingReviewCount > 0">当前没有可继续执行的任务，已提交的记录需等待护工审核后才会进入下一步。</p>
            <template v-else>
              <p>今日暂无启用的提醒实例。</p>
              <RouterLink to="/plans" class="font-semibold text-primary hover:text-primary/80">
                前往创建计划
              </RouterLink>
            </template>
          </div>
          <div v-else class="space-y-3">
            <NSelect
              v-model:value="selectedScheduleId"
              :options="
                schedules.map(schedule => ({
                  label: `${schedule.medicineName} (${schedule.window.start}-${schedule.window.end})`,
                  value: schedule.id,
                }))
              "
              placeholder="选择用药计划"
              size="large"
            />
            <div v-if="selectedReminderInstance" class="rounded-[18px] bg-white p-3 shadow-card">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="font-semibold text-text">{{ selectedReminderInstance.medicineName }}</p>
                  <p class="mt-1 text-sm text-muted">
                    {{ selectedReminderInstance.dose }} · {{ selectedReminderInstance.frequency }}
                  </p>
                </div>
                <StatusBadge :label="selectedReminderInstance.reviewStatus" tone="info" />
              </div>
            </div>
          </div>
        </StateCard>

        <StateCard
          title="检测状态"
          :status-label="detectionStatusLabel"
          :status-type="
            detectionStatusTone === 'danger'
              ? 'error'
              : detectionStatusTone === 'warning'
                ? 'warning'
                : detectionStatusTone === 'success'
                  ? 'success'
                  : 'info'
          "
          subtitle="基于目标检测与动作判定的异步检测任务。"
        >
          <ul class="space-y-2 text-sm leading-6 text-muted">
            <li>模型输入 640×640，结果由 Spring 统一聚合返回。</li>
            <li>检测结果与护工审核状态已经拆开，不会再直接视为完成。</li>
            <li v-if="activeDetectionJob?.traceId">Trace: {{ activeDetectionJob.traceId }}</li>
          </ul>
        </StateCard>

        <StateCard title="执行提醒" status-label="按顺序完成" status-type="info" subtitle="避免同时打开多个入口，让检测只围绕一条主链路进行。">
          <ul class="space-y-2 text-sm leading-6 text-muted">
            <li v-for="item in checklist" :key="item">• {{ item }}</li>
          </ul>
        </StateCard>

        <NButton
          v-if="showRetrySecondaryAction || flowStage === 'needs_retry'"
          block
          quaternary
          size="large"
          @click="resetDetection"
        >
          {{ flowStage === 'needs_retry' ? '清除本次结果并重新开始' : '重新检测' }}
        </NButton>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useMessage } from 'naive-ui'
import {
  MedicalOutline,
  PulseOutline,
  RefreshOutline,
  ShieldCheckmarkOutline,
  VideocamOutline,
} from '@vicons/ionicons5'
import CountdownButton from '@/components/ui/CountdownButton.vue'
import MetricCard from '@/components/ui/MetricCard.vue'
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
import { confirmReminderInstance, fetchReminderInstances } from '@/services/reminderInstanceService'
import type { ReminderInstanceItem } from '@/services/dashboardService'

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
  confidence?: number | null
  latencyMs?: number | null
  traceId?: string | null
} | null>(null)
const activeDetectionJob = ref<DetectionJob | null>(null)

const detectionState = computed<'idle' | 'suspected' | 'confirmed' | 'abnormal'>(() => {
  if (videoDetectionResult.value?.status) {
    return videoDetectionResult.value.status
  }
  return 'idle'
})

const currentStep = computed(() => {
  if (isVideoProcessing.value || videoDetectionResult.value) {
    return 3
  }
  if (isRecording.value) {
    return 2
  }
  return 1
})

const selectedReminderInstance = computed(() =>
  reminderInstances.value.find(instance => String(instance.scheduleId) === selectedScheduleId.value) ?? null,
)

const heroTitle = computed(() => {
  if (!selectedReminderInstance.value) {
    return '请选择一个今日提醒实例开始检测'
  }
  return `${selectedReminderInstance.value.medicineName} 检测流程`
})

const heroDescription = computed(() => {
  if (isVideoProcessing.value) {
    return videoProcessingMessage.value ?? '检测任务已提交，正在等待后端返回结构化结果。'
  }
  if (isRecording.value) {
    return '保持药品、手部与面部都在画面中，结束录制后系统会自动进入检测。'
  }
  if (detectionState.value === 'confirmed') {
    return '系统已经识别到本次服药，可直接进行确认并保存记录。'
  }
  if (detectionState.value === 'suspected') {
    return '系统给出了疑似服药结果，建议人工确认后再写入记录。'
  }
  if (detectionState.value === 'abnormal') {
    return '本次检测结果异常，建议重新录制，或按情况人工确认。'
  }
  return '按准备、录制、确认三步完成单次检测，不在页面里分散执行多个动作。'
})

const detectionStatusLabel = computed(() => {
  if (isVideoProcessing.value) return '检测中'
  if (detectionState.value === 'confirmed') return '已确认'
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

const canConfirm = computed(
  () =>
    Boolean(selectedScheduleId.value) &&
    (detectionState.value === 'confirmed' || detectionState.value === 'suspected') &&
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
  if (isVideoProcessing.value) {
    return { type: 'info', title: '检测任务处理中', message: videoProcessingMessage.value ?? '正在等待算法结果。' }
  }
  if (detectionState.value === 'confirmed') {
    return { type: 'success', title: '检测完成', message: videoDetectionResult.value?.message ?? '系统已识别出服药动作。' }
  }
  if (detectionState.value === 'suspected') {
    return { type: 'warning', title: '疑似已服药', message: videoDetectionResult.value?.message ?? '请人工确认本次服药。' }
  }
  if (detectionState.value === 'abnormal') {
    return { type: 'error', title: '检测异常', message: videoDetectionResult.value?.message ?? '请重新录制或手动处理。' }
  }
  return {
    type: 'info',
    title: '准备开始检测',
    message: '先选择今日提醒实例，再开启摄像头并开始录制。',
  }
})

const checklist = [
  '药品、手部和面部尽量保持在同一画面内。',
  '录制结束后系统会自动创建 detection job 并轮询结果。',
  '只有 confirmed 或 suspected 状态才建议执行人工确认。',
]

const toClock = (value?: string | null): string => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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
  status: instance.status === 'resolved' ? 'paused' : 'active',
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
  if (job.resultStatus === 'confirmed') return '系统已确认本次服药'
  if (job.resultStatus === 'suspected') return '系统判定为疑似服药，请人工确认'
  return '检测结果异常，请重新尝试'
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
      new Date().toISOString().slice(0, 10),
    )
    reminderInstances.value = instances.filter(instance => instance.status !== 'resolved')
    schedules.value = reminderInstances.value.map(mapReminderToSchedule)
    if (schedules.value.length > 0) {
      const preferred =
        reminderInstances.value.find(instance => instance.status === 'pending' || instance.status === 'suspected') ??
        reminderInstances.value[0]
      selectedScheduleId.value = preferred ? String(preferred.scheduleId) : null
    } else {
      selectedScheduleId.value = null
      message.warning('今日暂无可执行的提醒实例，请先创建并启用计划')
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
    message.success('录像已开始，请按流程完成服药动作')
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
      confidence: latestJob.confidence,
      latencyMs: latestJob.latencyMs,
      traceId: latestJob.traceId,
    }

    await loadSchedules()

    if (latestJob.resultStatus === 'confirmed') {
      message.success('检测完成，系统已确认本次服药')
    } else if (latestJob.resultStatus === 'suspected') {
      message.warning('检测结果为疑似服药，请人工确认')
    } else {
      message.error('检测结果异常，请重试')
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

const handleConfirm = async () => {
  if (!selectedScheduleId.value || !selectedReminderInstance.value) {
    message.warning('请先选择用药计划')
    return
  }
  if (detectionState.value !== 'confirmed' && detectionState.value !== 'suspected') {
    message.warning('请等待系统检测到药品和服药动作')
    return
  }

  isCreatingEvent.value = true
  try {
    await confirmReminderInstance(selectedReminderInstance.value.id, {
      confirmedBy: authStore.user?.name ?? authStore.user?.id ?? 'elder',
      confirmTime: new Date().toISOString(),
    })
    message.success('提醒实例已确认，服药记录已同步保存')
    videoDetectionResult.value = null
    activeDetectionJob.value = null
    videoBlob.value = null
    await loadSchedules()
  } catch (error: unknown) {
    const errorMsg = error instanceof Error ? error.message : '记录失败，请重试'
    console.error('确认服药失败:', error)
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
  await startCamera()
  if (errorMessage.value) {
    message.error(errorMessage.value)
    return
  }
  if (isStreaming.value) {
    message.success('摄像头已开启')
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
  stopCamera()
})
</script>

<template>
  <div class="space-y-6">
    <StatusBar
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

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        label="当前提醒实例"
        :value="selectedReminderInstance?.medicineName ?? '未选择'"
        :helper="
          selectedReminderInstance
            ? `${toClock(selectedReminderInstance.windowStartAt)} - ${toClock(selectedReminderInstance.windowEndAt)}`
            : '请先从右侧选择一个今日提醒实例。'
        "
        tone="neutral"
        :icon="MedicalOutline"
      />
      <MetricCard
        label="检测阶段"
        :value="detectionStatusLabel"
        :helper="isVideoProcessing ? '等待异步任务返回结果。' : '页面始终保持一个主动作，减少决策分叉。'"
        :tone="detectionStatusTone === 'danger' ? 'danger' : detectionStatusTone === 'warning' ? 'warning' : detectionStatusTone === 'success' ? 'success' : 'brand'"
        :icon="PulseOutline"
      />
      <MetricCard
        label="确认入口"
        :value="canConfirm ? '可确认' : '等待结果'"
        :helper="canConfirm ? '建议完成倒计时确认后再保存记录。' : '只有 confirmed 或 suspected 状态才允许人工确认。'"
        :tone="canConfirm ? 'success' : 'neutral'"
        :icon="ShieldCheckmarkOutline"
      />
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
              整个检测流程都围绕这个画面展开，先开启摄像头，再录制，再等待结果，最后人工确认。
            </p>
          </div>
          <div class="flex flex-wrap gap-3">
            <NButton
              v-if="!isStreaming"
              type="primary"
              size="large"
              :disabled="schedules.length === 0 || !selectedScheduleId"
              @click="handleStartCamera"
            >
              开启摄像头
            </NButton>
            <NButton
              v-else-if="!isRecording && !isVideoProcessing"
              type="primary"
              size="large"
              :disabled="!selectedScheduleId"
              @click="handleStartRecording"
            >
              开始录像
            </NButton>
            <NButton
              v-else-if="isRecording"
              type="error"
              size="large"
              :loading="isVideoProcessing"
              @click="handleStopRecordingAndProcess"
            >
              结束并检测
            </NButton>
            <NButton v-if="isStreaming && !isRecording" tertiary size="large" @click="stopCamera">
              关闭摄像头
            </NButton>
            <NButton
              v-if="videoDetectionResult && !isRecording && !isVideoProcessing"
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

        <div class="mt-6 overflow-hidden rounded-[30px] border border-line/70 bg-[#111616] shadow-soft">
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

        <div class="mt-5 grid gap-3 md:grid-cols-3">
          <div
            class="rounded-[22px] border p-4"
            :class="currentStep >= 1 ? 'border-primary/20 bg-primary/8' : 'border-line/70 bg-[#fffcf6]'"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">Step 1</p>
            <p class="mt-2 text-base font-semibold text-text">准备</p>
            <p class="mt-1 text-sm leading-6 text-muted">选择提醒实例并开启摄像头。</p>
          </div>
          <div
            class="rounded-[22px] border p-4"
            :class="currentStep >= 2 ? 'border-warning/20 bg-warning/10' : 'border-line/70 bg-[#fffcf6]'"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">Step 2</p>
            <p class="mt-2 text-base font-semibold text-text">录制</p>
            <p class="mt-1 text-sm leading-6 text-muted">完整记录药品展示与服药动作。</p>
          </div>
          <div
            class="rounded-[22px] border p-4"
            :class="currentStep === 3 ? 'border-success/20 bg-success/10' : 'border-line/70 bg-[#fffcf6]'"
          >
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70">Step 3</p>
            <p class="mt-2 text-base font-semibold text-text">确认</p>
            <p class="mt-1 text-sm leading-6 text-muted">等待结果，并在需要时人工确认。</p>
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
              <p v-if="videoDetectionResult.confidence !== undefined && videoDetectionResult.confidence !== null">
                置信度：{{ Math.round(videoDetectionResult.confidence * 100) }}%
              </p>
              <p v-if="videoDetectionResult.latencyMs !== undefined && videoDetectionResult.latencyMs !== null">
                延迟：{{ videoDetectionResult.latencyMs }} ms
              </p>
            </div>
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
          :status-label="schedules.length === 0 ? '请先创建计划' : '今日可执行'"
          :status-type="schedules.length === 0 ? 'warning' : 'success'"
          subtitle="只针对今日已生成的提醒实例执行检测。"
        >
          <div v-if="isLoadingSchedules" class="flex items-center gap-2 text-sm text-muted">
            <NSpin size="small" />
            正在加载提醒实例...
          </div>
          <div v-else-if="schedules.length === 0" class="space-y-2 text-sm text-muted">
            <p>今日暂无启用的提醒实例。</p>
            <RouterLink to="/plans" class="font-semibold text-primary hover:text-primary/80">
              前往创建计划
            </RouterLink>
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
                <StatusBadge :label="selectedReminderInstance.status" tone="info" />
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
            <li>检测任务状态与提醒实例状态会保持同步。</li>
            <li v-if="activeDetectionJob?.traceId">Trace: {{ activeDetectionJob.traceId }}</li>
          </ul>
        </StateCard>

        <StateCard title="执行提醒" status-label="按顺序完成" status-type="info" subtitle="避免同时打开多个入口，让检测只围绕一条主链路进行。">
          <ul class="space-y-2 text-sm leading-6 text-muted">
            <li v-for="item in checklist" :key="item">• {{ item }}</li>
          </ul>
        </StateCard>

        <CountdownButton
          label="确认并保存记录"
          :duration="3"
          :disabled="!canConfirm"
          :loading="isCreatingEvent"
          @confirm="handleConfirm"
        />

        <NButton
          v-if="detectionState === 'confirmed' || detectionState === 'suspected' || detectionState === 'abnormal'"
          block
          quaternary
          size="large"
          @click="resetDetection"
        >
          重置结果
        </NButton>
      </div>
    </section>
  </div>
</template>

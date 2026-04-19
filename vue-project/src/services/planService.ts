import http from '@/lib/http'
import type { ScheduleItem, PlanStat } from '@/types/plan'
import { useAuthStore } from '@/stores/auth'
import { usePatientStore } from '@/stores/patient'

export type { ScheduleItem } from '@/types/plan'

/**
 * ApiScheduleItem 后端返回的用药计划数据格式
 */
export interface ApiScheduleItem {
  id: number
  patientId: number
  medicineName: string
  type: 'PILL' | 'BLISTER' | 'BOTTLE' | 'BOX'
  dose: string
  freq: string
  winStart: string
  winEnd: string
  period: string
  status: 'enabled' | 'disabled'
  nextIntake?: string | null
}

/**
 * CreateScheduleRequest 创建用药计划的请求参数
 */
export interface CreateScheduleRequest {
  patientId: number
  medicineName: string
  type: 'PILL' | 'BLISTER' | 'BOTTLE' | 'BOX'
  dose: string
  freq: string
  winStart: string
  winEnd: string
  period: string
  status: 'enabled' | 'disabled'
}

/**
 * UpdateScheduleRequest 更新用药计划的请求参数（部分字段）
 */
export type UpdateScheduleRequest = Partial<CreateScheduleRequest>

/**
 * ToggleScheduleRequest 启停用药计划的请求参数
 */
export interface ToggleScheduleRequest {
  status?: 'active' | 'paused'
}

/**
 * mapApiScheduleToLocal 将后端返回的用药计划数据映射为前端使用的格式
 */
export function mapApiScheduleToLocal(apiSchedule: ApiScheduleItem): ScheduleItem {
  return {
    id: String(apiSchedule.id),
    medicineName: apiSchedule.medicineName,
    type: apiSchedule.type,
    dosage: apiSchedule.dose,
    frequency: apiSchedule.freq,
    window: {
      start: apiSchedule.winStart,
      end: apiSchedule.winEnd,
    },
    period: apiSchedule.period,
    status: apiSchedule.status === 'enabled' ? 'active' : 'paused',
    nextIntake: apiSchedule.nextIntake ?? '',
  }
}

/**
 * mapLocalScheduleToApi 将前端格式转换为后端请求格式
 */
export function mapLocalScheduleToApi(
  localSchedule: Partial<ScheduleItem>,
  patientId: number,
): Partial<CreateScheduleRequest> {
  const apiSchedule: Partial<CreateScheduleRequest> = {
    patientId,
  }

  if (localSchedule.medicineName) {
    apiSchedule.medicineName = localSchedule.medicineName
  }
  if (localSchedule.type) {
    apiSchedule.type = localSchedule.type
  }
  if (localSchedule.dosage) {
    apiSchedule.dose = localSchedule.dosage
  }
  if (localSchedule.frequency) {
    apiSchedule.freq = localSchedule.frequency
  }
  if (localSchedule.window) {
    apiSchedule.winStart = localSchedule.window.start
    apiSchedule.winEnd = localSchedule.window.end
  }
  if (localSchedule.period) {
    apiSchedule.period = localSchedule.period
  }
  if (localSchedule.status) {
    apiSchedule.status = localSchedule.status === 'active' ? 'enabled' : 'disabled'
  }

  return apiSchedule
}

/**
 * getCurrentPatientId 获取当前患者ID
 * - elder角色：使用自己的userId
 * - caregiver/child角色：使用patient store中的activePatientId
 */
function getCurrentPatientId(): number | null {
  const authStore = useAuthStore()
  const patientStore = usePatientStore()

  if (authStore.role === 'elder') {
    // elder角色使用自己的userId
    const userId = authStore.user?.id
    if (userId) {
      return Number(userId)
    }
  } else {
    // caregiver/child角色使用选中的患者ID
    const activePatientId = patientStore.activePatientId
    if (activePatientId) {
      return Number(activePatientId)
    }
  }

  return null
}

/**
 * fetchSchedules 获取用药计划列表
 * @param patientId 可选的患者ID，如果不提供则自动获取当前患者ID
 * @returns 返回用药计划列表
 */
export async function fetchSchedules(patientId?: number): Promise<ScheduleItem[]> {
  const targetPatientId = patientId ?? getCurrentPatientId()
  if (!targetPatientId) {
    throw new Error('无法获取患者ID，请先选择患者或登录')
  }

  const apiSchedules: ApiScheduleItem[] = await http.get('/schedules', {
    params: { patientId: targetPatientId },
  })

  return apiSchedules.map(mapApiScheduleToLocal)
}

/**
 * getSchedulesByPatientId 根据患者ID获取用药计划列表
 * @param patientId 患者ID
 * @returns 返回用药计划列表
 */
export async function getSchedulesByPatientId(patientId: number): Promise<ScheduleItem[]> {
  if (!patientId) {
    throw new Error('患者ID不能为空')
  }

  const apiSchedules: ApiScheduleItem[] = await http.get('/schedules', {
    params: { patientId },
  })

  return apiSchedules.map(mapApiScheduleToLocal)
}

/**
 * createSchedule 创建用药计划
 * @param schedule 用药计划数据
 * @param patientId 可选的患者ID，如果不提供则自动获取当前患者ID
 * @returns 返回创建成功的计划对象
 */
export async function createSchedule(
  schedule: Partial<ScheduleItem>,
  patientId?: number,
): Promise<ScheduleItem> {
  const targetPatientId = patientId ?? getCurrentPatientId()
  if (!targetPatientId) {
    throw new Error('无法获取患者ID，请先选择患者或登录')
  }

  const apiSchedule = mapLocalScheduleToApi(schedule, targetPatientId) as CreateScheduleRequest
  const created: ApiScheduleItem = await http.post('/schedules', apiSchedule)

  return mapApiScheduleToLocal(created)
}

/**
 * updateSchedule 更新用药计划
 * @param scheduleId 计划ID
 * @param schedule 需要更新的字段
 * @returns 返回更新影响的行数
 */
export async function updateSchedule(
  scheduleId: string | number,
  schedule: Partial<ScheduleItem>,
): Promise<number> {
  const patientId = getCurrentPatientId()
  if (!patientId) {
    throw new Error('无法获取患者ID，请先选择患者或登录')
  }

  const apiSchedule = mapLocalScheduleToApi(schedule, patientId)
  const result: number = await http.patch(`/schedules/${scheduleId}`, apiSchedule)

  return result
}

/**
 * togglePlanStatus 启停用药计划
 * @param scheduleId 计划ID
 * @param targetStatus 可选的目标状态，如果不提供则自动切换
 * @returns 返回更新影响的行数
 */
export async function togglePlanStatus(
  scheduleId: string | number,
  targetStatus?: 'active' | 'paused',
): Promise<number> {
  const body: ToggleScheduleRequest | undefined = targetStatus
    ? { status: targetStatus }
    : undefined

  const result: number = await http.post(`/schedules/${scheduleId}/toggle`, body)

  return result
}

/**
 * fetchPlanStats 获取用药计划统计数据
 * @returns 返回统计数据
 */
export async function fetchPlanStats(): Promise<PlanStat[]> {
  try {
    const schedules = await fetchSchedules()
    const activeCount = schedules.filter(s => s.status === 'active').length
    const pausedCount = schedules.filter(s => s.status === 'paused').length
    const todayReminders = schedules.filter(s => s.status === 'active').length

    return [
      { label: '进行中', value: activeCount, unit: '个', trend: 0, trendLabel: '较昨日' },
      { label: '暂停中', value: pausedCount, unit: '个', trend: 0, trendLabel: '较昨日' },
      { label: '今日提醒', value: todayReminders, unit: '次', trend: 0, trendLabel: '较昨日' },
    ]
  } catch (error) {
    // 如果获取计划列表失败，返回空统计数据
    return [
      { label: '进行中', value: 0, unit: '个', trend: 0, trendLabel: '较昨日' },
      { label: '暂停中', value: 0, unit: '个', trend: 0, trendLabel: '较昨日' },
      { label: '今日提醒', value: 0, unit: '次', trend: 0, trendLabel: '较昨日' },
    ]
  }
}

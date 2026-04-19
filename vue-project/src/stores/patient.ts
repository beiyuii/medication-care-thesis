import { defineStore } from 'pinia'
import type { PatientSummary as ApiPatientSummary } from '@/types/auth'

export type PlanStatus = 'on_track' | 'delayed' | 'paused' | 'active'

/**
 * PatientSummary 用于展示在护工/子女端列表中的老年人信息。
 * 兼容后端返回的数据格式（id 为 number，planStatus 为 active/paused）
 */
export interface PatientSummary {
  /** id 唯一标识，用于接口查询。 */
  id: string | number
  /** name 展示名称。 */
  name: string
  /** nextIntakeTime 表示下一次提醒时间（ISO8601格式）。 */
  nextIntakeTime: string
  /** planStatus 指示整体执行状态（后端返回 active/paused，前端映射为 on_track/delayed/paused）。 */
  planStatus: PlanStatus | 'active' | 'paused'
  /** alertCount 当前未处理异常数量。 */
  alertCount: number
}

/**
 * mapApiPatientToLocal 将后端返回的患者数据映射为前端使用的格式
 */
export function mapApiPatientToLocal(apiPatient: ApiPatientSummary): PatientSummary {
  // 将后端的 planStatus (active/paused) 映射为前端的 planStatus
  let planStatus: PlanStatus = 'on_track'
  if (apiPatient.planStatus === 'paused') {
    planStatus = 'paused'
  } else if (apiPatient.planStatus === 'active') {
    planStatus = 'on_track'
  }

  return {
    id: String(apiPatient.id), // 转换为 string 以兼容现有代码
    name: apiPatient.name,
    nextIntakeTime: apiPatient.nextIntakeTime,
    planStatus,
    alertCount: apiPatient.alertCount,
  }
}

interface PatientState {
  /** patients 保存当前可查看的老年人列表。 */
  patients: PatientSummary[]
  /** activePatientId 标记当前查看的老年人。 */
  activePatientId: string | null
}

export const usePatientStore = defineStore('patient', {
  state: (): PatientState => ({
    patients: [],
    activePatientId: null,
  }),
  getters: {
    activePatient: state =>
      state.patients.find(patient => patient.id === state.activePatientId) ?? null,
  },
  actions: {
    /**
     * setPatients 用最新数据覆盖患者列表，并自动初始化选中项。
     */
    setPatients(patients: PatientSummary[]) {
      this.patients = patients
      if (!this.activePatientId && patients.length > 0) {
        const firstPatient = patients[0]
        if (firstPatient) {
          this.activePatientId = String(firstPatient.id)
        }
      }
    },
    /**
     * selectPatient 显式切换当前查看的老年人。
     */
    selectPatient(patientId: string) {
      this.activePatientId = patientId
    },
    /**
     * updatePatient 允许局部更新单个老年人状态。
     */
    updatePatient(summary: PatientSummary) {
      const index = this.patients.findIndex(patient => patient.id === summary.id)
      if (index > -1) {
        this.patients[index] = summary
      }
    },
  },
})

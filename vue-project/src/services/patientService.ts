import http from '@/lib/http'
import type { PatientSummary } from '@/types/auth'

/**
 * PatientDetail 患者详情响应数据
 */
export interface PatientDetail {
  /** id 为患者 ID */
  id: number
  /** name 为患者姓名 */
  name: string
  /** age 为年龄 */
  age: number
  /** phone 为联系电话 */
  phone: string
  /** schedules 为关联的用药计划（最近5条） */
  schedules: Array<{
    id: number
    patientId: number
    medicineName: string
    type: string
    dosage: string
    frequency: string
    window: {
      start: string
      end: string
    }
    period: string
    status: string
    nextIntake?: string | null
  }>
  /** recentAlerts 为最近告警（最近5条） */
  recentAlerts: Array<{
    id: number
    title: string
    occurredAt: string
  }>
}

/**
 * getPatients 获取患者列表
 * @returns 返回当前账号可见的老年人列表
 */
export async function getPatients(): Promise<PatientSummary[]> {
  return http.get('/patients')
}

/**
 * getCurrentPatient 获取当前患者（child角色专用）
 * @returns 返回唯一关联的患者信息（一对一关系），如果不是child角色或不存在则返回null
 */
export async function getCurrentPatient(): Promise<PatientSummary | null> {
  return http.get('/patients/current')
}

/**
 * getPatientDetail 获取患者详情
 * @param id 患者ID
 * @returns 返回患者详细信息，包含基础信息、关联计划摘要、最近告警
 */
export async function getPatientDetail(id: number): Promise<PatientDetail> {
  // 确保 ID 是整数
  const patientId = Math.floor(Number(id))
  if (isNaN(patientId) || patientId <= 0) {
    throw new Error(`无效的患者ID: ${id}`)
  }
  
  if (import.meta.env.DEV) {
    console.log('[getPatientDetail] 请求患者详情，ID:', patientId, '类型:', typeof patientId)
  }
  
  return http.get(`/patients/${patientId}`)
}

/**
 * bindPatientByElderUsername 通过老人用户名绑定当前护工/子女账号。
 * @param elderUsername 老人用户名
 * @returns 返回绑定后的患者摘要
 */
export async function bindPatientByElderUsername(elderUsername: string): Promise<PatientSummary> {
  return http.post('/patients/bind', { elderUsername })
}

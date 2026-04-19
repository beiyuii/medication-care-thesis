import { computed } from 'vue'
import { useMessage } from 'naive-ui'
import { usePatientStore, mapApiPatientToLocal, type PatientSummary } from '@/stores/patient'
import { getPatients } from '@/services/patientService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'

/**
 * usePatientList 封装患者选择与派发逻辑，方便多端复用。
 */
export function usePatientList() {
  const patientStore = usePatientStore()
  const message = useMessage()

  const patients = computed(() => patientStore.patients)
  const activePatient = computed(() => patientStore.activePatient)
  const activePatientId = computed<string | null>({
    get: () => patientStore.activePatientId,
    set: value => {
      if (value) {
        patientStore.selectPatient(value)
      }
    },
  })

  const selectPatient = (patientId: string) => {
    patientStore.selectPatient(patientId)
  }

  /**
   * hydratePatients 从后端加载患者列表
   */
  const hydratePatients = async () => {
    try {
      const apiPatients = await getPatients()
      // 将后端数据映射为前端格式
      const mappedPatients = apiPatients.map(mapApiPatientToLocal)
      patientStore.setPatients(mappedPatients)
    } catch (error: unknown) {
      const errorMsg = extractErrorMessage(error, '加载患者列表失败')
      message.error(errorMsg)
      logError(error, '加载患者列表')
    }
  }

  /**
   * loadPatients 兼容旧接口，支持传入 Mock 数据（用于测试）
   */
  const loadPatients = (list?: PatientSummary[]) => {
    if (list && list.length > 0) {
      // 如果提供了列表，直接使用（兼容旧代码）
      patientStore.setPatients(list)
    } else {
      // 否则从后端加载
      hydratePatients()
    }
  }

  return {
    patients,
    activePatient,
    activePatientId,
    selectPatient,
    hydratePatients,
    loadPatients, // 保留兼容性
  }
}

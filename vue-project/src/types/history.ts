/**
 * HistoryRange 表示查询范围。
 */
export type HistoryRange = 'day' | 'week' | 'month'

/**
 * HistorySummary 提供统计卡片的数据。
 */
export interface HistorySummary {
  /** label 展示名称。 */
  label: string
  /** value 数值。 */
  value: number
  /** unit 单位。 */
  unit: string
  /** description 补充说明。 */
  description: string
}

/**
 * HistoryEvent 记录一次服药事件。
 */
export interface HistoryEvent {
  /** id 唯一标识。 */
  id: string
  /** timestamp 发生时间。 */
  timestamp: string
  /** medicineName 药品名称。 */
  medicineName: string
  /** planName 所属计划。 */
  planName: string
  /** status 事件状态。 */
  status: 'confirmed' | 'suspected' | 'abnormal'
  /** action 手动确认人或动作说明。 */
  action: string
  /** imageUrl 缩略图地址。 */
  imageUrl?: string
  /** videoUrl 检测过程录像地址（若后端已落盘）。 */
  videoUrl?: string
  /** scheduleId 关联的计划ID（可选）。 */
  scheduleId?: number
  /** patientId 患者ID（可选）。 */
  patientId?: number
  /** targetsJson 目标检测JSON字符串（可选）。 */
  targetsJson?: string
  /** confirmedBy 确认人（可选）。 */
  confirmedBy?: string
  /** confirmedAt 确认时间（可选）。 */
  confirmedAt?: string
  /** rawAction 原始动作描述（可选）。 */
  rawAction?: string
}

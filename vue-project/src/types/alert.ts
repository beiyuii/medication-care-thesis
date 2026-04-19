/**
 * AlertSeverity 定义异常等级。
 */
export type AlertSeverity = 'info' | 'warning' | 'danger'

/**
 * AlertItem 描述一条异常/告警。
 */
export interface AlertItem {
  /** id 唯一标识。 */
  id: string
  /** title 短标题。 */
  title: string
  /** description 详情描述。 */
  description: string
  /** patient 关联老年人姓名。 */
  patient: string
  /** occurredAt 发生时间（格式化后的字符串）。 */
  occurredAt: string
  /** severity 异常等级。 */
  severity: AlertSeverity
  /** resolved 是否已处理。 */
  resolved: boolean
  /** suggestion 处理建议。 */
  suggestion: string
  /** type 告警类型（可选）。 */
  type?: string
  /** resolvedAt 处理时间（可选，ISO8601格式）。 */
  resolvedAt?: string | null
  /** actionNote 处理备注（可选）。 */
  actionNote?: string | null
  /** patientId 患者ID（可选）。 */
  patientId?: number
  /** medicineName 关联药品名称（可选）。 */
  medicineName?: string
}

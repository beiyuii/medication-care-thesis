/**
 * PlanStatus 表示用药计划的启停状态。
 */
export type PlanStatus = 'active' | 'paused' | 'completed'

/**
 * ReminderWindow 记录计划的时间窗。
 */
export interface ReminderWindow {
  /** start 为开始时间（HH:mm）。 */
  start: string
  /** end 为结束时间（HH:mm）。 */
  end: string
}

/**
 * ScheduleItem 响应后端的用药计划数据结构。
 */
export interface ScheduleItem {
  /** id 唯一标识。 */
  id: string
  /** medicineName 药品名称。 */
  medicineName: string
  /** type 四类药品的枚举值。 */
  type: 'PILL' | 'BLISTER' | 'BOTTLE' | 'BOX'
  /** dosage 描述剂量。 */
  dosage: string
  /** frequency 频率（如每日/每周）。 */
  frequency: string
  /** window 表示时间窗。 */
  window: ReminderWindow
  /** period 计划周期描述。 */
  period: string
  /** status 当前计划状态。 */
  status: PlanStatus
  /** nextIntake 下一次提醒时间。 */
  nextIntake: string
}

/**
 * PlanStat 用于页面顶部的统计卡片。
 */
export interface PlanStat {
  /** label 展示名称。 */
  label: string
  /** value 数值内容。 */
  value: number
  /** unit 展示单位。 */
  unit: string
  /** trend 最近趋势百分比。 */
  trend: number
  /** trendLabel 描述文案。 */
  trendLabel: string
}

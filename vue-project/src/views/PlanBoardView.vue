<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import MetricCard from '@/components/ui/MetricCard.vue'
import PageHero from '@/components/ui/PageHero.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { PlanStat, PlanStatus, ScheduleItem } from '@/types/plan'
import { fetchPlanStats, fetchSchedules, togglePlanStatus, createSchedule, updateSchedule } from '@/services/planService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'

/** statusFilter 保存当前筛选状态。 */
const statusFilter = ref<'all' | PlanStatus>('all')
/** loading 标记列表加载状态。 */
const loading = ref(false)
/** stats 保存顶部统计数据。 */
const stats = ref<PlanStat[]>([])
/** schedules 保存计划列表。 */
const schedules = ref<ScheduleItem[]>([])
const message = useMessage()

// 新增计划Modal相关状态
const showCreateModal = ref(false)
const createFormRef = ref<FormInst | null>(null)
const isSubmitting = ref(false)
const createFormData = ref({
  medicineName: '',
  type: 'PILL' as ScheduleItem['type'],
  dosageAmount: null as number | null, // 剂量数字
  dosageUnit: '片', // 剂量单位
  frequency: '',
  windowStart: null as number | null, // TimePicker 返回时间戳
  windowEnd: null as number | null,
  periodAmount: null as number | null, // 周期数字
  periodUnit: '天', // 周期单位
  status: 'active' as PlanStatus,
})

// 编辑计划Modal相关状态
const showEditModal = ref(false)
const editFormRef = ref<FormInst | null>(null)
const isEditing = ref(false)
const editingPlanId = ref<string | null>(null)
const editFormData = ref({
  medicineName: '',
  type: 'PILL' as ScheduleItem['type'],
  dosageAmount: null as number | null,
  dosageUnit: '片',
  frequency: '',
  windowStart: null as number | null,
  windowEnd: null as number | null,
  periodAmount: null as number | null,
  periodUnit: '天',
  status: 'active' as PlanStatus,
})

// 编辑表单验证规则（复用新增表单的规则）
const editFormRules: FormRules = {
  medicineName: [
    { required: true, message: '请输入药品名称', trigger: 'blur' },
  ],
  type: [
    { required: true, message: '请选择药品类型', trigger: 'change' },
  ],
  dosageAmount: [
    { required: true, message: '请输入剂量', trigger: 'blur', type: 'number' },
    { type: 'number', min: 0.01, message: '剂量必须大于0', trigger: 'blur' },
  ],
  dosageUnit: [
    { required: true, message: '请选择单位', trigger: 'change' },
  ],
  frequency: [
    { required: true, message: '请输入频次', trigger: 'blur' },
  ],
  windowStart: [
    {
      required: true,
      message: '请选择开始时间',
      trigger: ['change', 'blur'],
      validator: (_rule, value) => {
        if (value === null || value === undefined) {
          return new Error('请选择开始时间')
        }
        if (typeof value === 'number') {
          if (Number.isNaN(value)) {
            return new Error('请选择开始时间')
          }
        } else {
          return new Error('请选择开始时间')
        }
        return true
      },
    },
  ],
  windowEnd: [
    {
      required: true,
      message: '请选择结束时间',
      trigger: ['change', 'blur'],
      validator: (_rule, value) => {
        if (value === null || value === undefined) {
          return new Error('请选择结束时间')
        }
        if (typeof value === 'number') {
          if (Number.isNaN(value)) {
            return new Error('请选择结束时间')
          }
        } else {
          return new Error('请选择结束时间')
        }
        return true
      },
    },
  ],
  periodAmount: [
    {
      required: true,
      message: '请输入周期',
      trigger: 'blur',
      type: 'number',
      validator: (_rule, value) => {
        if (editFormData.value.periodUnit === '持续') {
          return true
        }
        if (value === null || value === undefined) {
          return new Error('请输入周期')
        }
        if (typeof value === 'number' && value < 1) {
          return new Error('周期必须大于0')
        }
        return true
      },
    },
  ],
  periodUnit: [
    { required: true, message: '请选择单位', trigger: 'change' },
  ],
}

const createFormRules: FormRules = {
  medicineName: [
    { required: true, message: '请输入药品名称', trigger: 'blur' },
  ],
  type: [
    { required: true, message: '请选择药品类型', trigger: 'change' },
  ],
  dosageAmount: [
    { required: true, message: '请输入剂量', trigger: 'blur', type: 'number' },
    { type: 'number', min: 0.01, message: '剂量必须大于0', trigger: 'blur' },
  ],
  dosageUnit: [
    { required: true, message: '请选择单位', trigger: 'change' },
  ],
  frequency: [
    { required: true, message: '请输入频次', trigger: 'blur' },
  ],
  windowStart: [
    {
      required: true,
      message: '请选择开始时间',
      trigger: ['change', 'blur'],
      validator: (_rule, value) => {
        if (value === null || value === undefined) {
          return new Error('请选择开始时间')
        }
        if (typeof value === 'number') {
          if (Number.isNaN(value)) {
            return new Error('请选择开始时间')
          }
        } else {
          return new Error('请选择开始时间')
        }
        return true
      },
    },
  ],
  windowEnd: [
    {
      required: true,
      message: '请选择结束时间',
      trigger: ['change', 'blur'],
      validator: (_rule, value) => {
        if (value === null || value === undefined) {
          return new Error('请选择结束时间')
        }
        if (typeof value === 'number') {
          if (Number.isNaN(value)) {
            return new Error('请选择结束时间')
          }
        } else {
          return new Error('请选择结束时间')
        }
        return true
      },
    },
  ],
  periodAmount: [
    {
      required: true,
      message: '请输入周期',
      trigger: 'blur',
      type: 'number',
      validator: (_rule, value) => {
        // 如果单位是"持续"，则不需要输入数字
        if (createFormData.value.periodUnit === '持续') {
          return true
        }
        if (value === null || value === undefined) {
          return new Error('请输入周期')
        }
        if (typeof value === 'number' && value < 1) {
          return new Error('周期必须大于0')
        }
        return true
      },
    },
  ],
  periodUnit: [
    { required: true, message: '请选择单位', trigger: 'change' },
  ],
}

const typeOptions = [
  { label: '药片', value: 'PILL' },
  { label: '泡罩板', value: 'BLISTER' },
  { label: '药瓶', value: 'BOTTLE' },
  { label: '药盒', value: 'BOX' },
]

const dosageUnitOptions = [
  { label: '片', value: '片' },
  { label: '粒', value: '粒' },
  { label: 'ml', value: 'ml' },
  { label: 'mg', value: 'mg' },
  { label: 'g', value: 'g' },
  { label: 'U', value: 'U' },
  { label: '袋', value: '袋' },
  { label: '支', value: '支' },
]

const periodUnitOptions = [
  { label: '天', value: '天' },
  { label: '周', value: '周' },
  { label: '月', value: '月' },
  { label: '持续', value: '持续' },
]

const statusLabelMap: Record<PlanStatus, string> = {
  active: '进行中',
  paused: '已暂停',
  completed: '已完成',
}

const typeMap: Record<ScheduleItem['type'], string> = {
  PILL: '药片',
  BLISTER: '泡罩板',
  BOTTLE: '药瓶',
  BOX: '药盒',
}

const filteredSchedules = computed(() => {
  if (statusFilter.value === 'all') {
    return schedules.value
  }
  return schedules.value.filter(schedule => schedule.status === statusFilter.value)
})

const activeCount = computed(() => schedules.value.filter(schedule => schedule.status === 'active').length)
const pausedCount = computed(() => schedules.value.filter(schedule => schedule.status === 'paused').length)
const completedCount = computed(() => schedules.value.filter(schedule => schedule.status === 'completed').length)

const heroDescription = computed(() => {
  if (schedules.value.length === 0) {
    return '先建立药品、剂量与时间窗，系统才能每天生成稳定的提醒实例。'
  }
  if (statusFilter.value === 'all') {
    return `当前共维护 ${schedules.value.length} 条计划，其中进行中 ${activeCount.value} 条。`
  }
  return `当前筛选展示 ${filteredSchedules.value.length} 条“${statusLabelMap[statusFilter.value]}”状态的计划。`
})

const formatNextIntake = (value?: string | null): string => {
  if (!value) return '待生成'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loadData = async () => {
  loading.value = true
  try {
    const [planStats, planList] = await Promise.all([fetchPlanStats(), fetchSchedules()])
    stats.value = planStats
    schedules.value = planList
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载计划失败')
    message.error(errorMsg)
    logError(error, '加载用药计划')
  } finally {
    loading.value = false
  }
}

/**
 * handleToggle 切换计划状态（暂停/启用）
 * 使用的接口：POST /api/schedules/{id}/toggle
 */
const handleToggle = async (planId: string) => {
  try {
    await togglePlanStatus(planId)
    message.success('已切换计划状态')
    // 重新加载数据以获取最新状态
    await loadData()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '操作失败')
    message.error(errorMsg)
    logError(error, '切换计划状态')
  }
}

/**
 * handleCreate 打开新增计划Modal
 */
const handleCreate = () => {
  // 重置表单
  createFormData.value = {
    medicineName: '',
    type: 'PILL',
    dosageAmount: null,
    dosageUnit: '片',
    frequency: '',
    windowStart: null,
    windowEnd: null,
    periodAmount: null,
    periodUnit: '天',
    status: 'active',
  }
  showCreateModal.value = true
}

/**
 * formatTimeFromTimestamp 将时间戳转换为 HH:mm 格式
 */
const formatTimeFromTimestamp = (timestamp: number | null): string => {
  if (timestamp === null || timestamp === undefined) return ''
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

/**
 * parseTimeToTimestamp 将 HH:mm 字符串转换为当天的时间戳
 */
const parseTimeToTimestamp = (timeStr: string): number | null => {
  if (!timeStr) return null
  try {
    const [hours = 0, minutes = 0] = timeStr.split(':').map(Number)
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return null
    const date = new Date()
    date.setHours(hours, minutes, 0, 0)
    return date.getTime()
  } catch {
    return null
  }
}

/**
 * parseDosage 解析剂量字符串，提取数字和单位
 * 例如："1片" -> { amount: 1, unit: "片" }
 */
const parseDosage = (dosage: string): { amount: number | null; unit: string } => {
  if (!dosage) return { amount: null, unit: '片' }
  // 尝试匹配数字和单位
  const match = dosage.match(/^([\d.]+)(.*)$/)
  if (match) {
    const amount = parseFloat(match[1] ?? '')
    const unit = match[2] || '片'
    return { amount: isNaN(amount) ? null : amount, unit }
  }
  return { amount: null, unit: '片' }
}

/**
 * parsePeriod 解析周期字符串，提取数字和单位
 * 例如："30天" -> { amount: 30, unit: "天" }
 * "持续" -> { amount: null, unit: "持续" }
 */
const parsePeriod = (period: string): { amount: number | null; unit: string } => {
  if (!period || period === '持续') return { amount: null, unit: '持续' }
  const match = period.match(/^([\d.]+)(.*)$/)
  if (match) {
    const amount = parseInt(match[1] ?? '', 10)
    const unit = match[2] || '天'
    return { amount: isNaN(amount) ? null : amount, unit }
  }
  return { amount: null, unit: '天' }
}

/**
 * handleEdit 打开编辑计划Modal，并填充当前计划数据
 */
const handleEdit = (plan: ScheduleItem) => {
  editingPlanId.value = plan.id
  
  // 解析剂量
  const dosageParsed = parseDosage(plan.dosage)
  
  // 解析周期
  const periodParsed = parsePeriod(plan.period)
  
  // 解析时间窗
  const windowStart = parseTimeToTimestamp(plan.window.start)
  const windowEnd = parseTimeToTimestamp(plan.window.end)
  
  // 填充表单数据
  editFormData.value = {
    medicineName: plan.medicineName,
    type: plan.type,
    dosageAmount: dosageParsed.amount,
    dosageUnit: dosageParsed.unit,
    frequency: plan.frequency,
    windowStart,
    windowEnd,
    periodAmount: periodParsed.amount,
    periodUnit: periodParsed.unit,
    status: plan.status,
  }
  
  showEditModal.value = true
}

/**
 * handleEditSubmit 提交编辑计划表单
 * 使用的接口：PATCH /api/schedules/{id}
 */
const handleEditSubmit = async () => {
  if (!editFormRef.value || !editingPlanId.value) return

  try {
    await editFormRef.value.validate()
  } catch {
    return
  }

  isEditing.value = true
  try {
    // 组合剂量：数字 + 单位
    const dosage = editFormData.value.dosageAmount !== null
      ? `${editFormData.value.dosageAmount}${editFormData.value.dosageUnit}`
      : ''

    // 组合周期：数字 + 单位，如果是"持续"则只显示"持续"
    const period = editFormData.value.periodUnit === '持续'
      ? '持续'
      : editFormData.value.periodAmount !== null
        ? `${editFormData.value.periodAmount}${editFormData.value.periodUnit}`
        : ''

    const scheduleData: Partial<ScheduleItem> = {
      medicineName: editFormData.value.medicineName,
      type: editFormData.value.type,
      dosage,
      frequency: editFormData.value.frequency,
      window: {
        start: formatTimeFromTimestamp(editFormData.value.windowStart),
        end: formatTimeFromTimestamp(editFormData.value.windowEnd),
      },
      period,
      status: editFormData.value.status,
    }

    await updateSchedule(editingPlanId.value, scheduleData)
    message.success('计划更新成功')
    showEditModal.value = false
    editingPlanId.value = null
    // 重新加载数据
    await loadData()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '更新计划失败')
    if (import.meta.env.DEV) {
      console.log('[更新计划错误] 提取的错误消息:', errorMsg)
      console.log('[更新计划错误] 原始错误对象:', error)
    }
    message.error(errorMsg, { duration: 5000 })
    logError(error, '更新用药计划')
  } finally {
    isEditing.value = false
  }
}

/**
 * handleCreateSubmit 提交新增计划表单
 * 使用的接口：POST /api/schedules
 */
const handleCreateSubmit = async () => {
  if (!createFormRef.value) return

  try {
    await createFormRef.value.validate()
  } catch {
    return
  }

  isSubmitting.value = true
  try {
    // 组合剂量：数字 + 单位
    const dosage = createFormData.value.dosageAmount !== null
      ? `${createFormData.value.dosageAmount}${createFormData.value.dosageUnit}`
      : ''

    // 组合周期：数字 + 单位，如果是"持续"则只显示"持续"
    const period = createFormData.value.periodUnit === '持续'
      ? '持续'
      : createFormData.value.periodAmount !== null
        ? `${createFormData.value.periodAmount}${createFormData.value.periodUnit}`
        : ''

    const scheduleData: Partial<ScheduleItem> = {
      medicineName: createFormData.value.medicineName,
      type: createFormData.value.type,
      dosage,
      frequency: createFormData.value.frequency,
      window: {
        start: formatTimeFromTimestamp(createFormData.value.windowStart),
        end: formatTimeFromTimestamp(createFormData.value.windowEnd),
      },
      period,
      status: createFormData.value.status,
    }

    await createSchedule(scheduleData)
    message.success('计划创建成功')
    showCreateModal.value = false
    // 重新加载数据
    await loadData()
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '创建计划失败')
    // 确保错误消息能够正确显示
    if (import.meta.env.DEV) {
      console.log('[创建计划错误] 提取的错误消息:', errorMsg)
      console.log('[创建计划错误] 原始错误对象:', error)
    }
    // 显示错误消息，使用较长的显示时间确保用户能看到
    message.error(errorMsg, { duration: 5000 })
    logError(error, '创建用药计划')
  } finally {
    isSubmitting.value = false
  }
}

// 监听周期单位变化，如果选择"持续"则清空数字
watch(
  () => createFormData.value.periodUnit,
  (newUnit) => {
    if (newUnit === '持续') {
      createFormData.value.periodAmount = null
      // 延迟验证，确保值已更新
      setTimeout(() => {
        createFormRef.value?.validate()
      }, 0)
    }
  }
)

// 监听编辑表单的周期单位变化
watch(
  () => editFormData.value.periodUnit,
  (newUnit) => {
    if (newUnit === '持续') {
      editFormData.value.periodAmount = null
      setTimeout(() => {
        editFormRef.value?.validate()
      }, 0)
    }
  }
)

onMounted(loadData)
</script>

<template>
  <div class="space-y-6">
    <PageHero
      eyebrow="Plan management"
      title="用药计划总览"
      :description="heroDescription"
      tone="soft"
    >
      <template #meta>
        <StatusBadge :label="`进行中 ${activeCount}`" tone="success" />
        <StatusBadge :label="`已暂停 ${pausedCount}`" tone="warning" />
        <StatusBadge :label="`已完成 ${completedCount}`" tone="neutral" />
      </template>
      <template #actions>
        <div class="flex flex-wrap items-center gap-3">
          <NRadioGroup v-model:value="statusFilter" size="small">
            <NRadioButton value="all">全部</NRadioButton>
            <NRadioButton value="active">进行中</NRadioButton>
            <NRadioButton value="paused">已暂停</NRadioButton>
            <NRadioButton value="completed">已完成</NRadioButton>
          </NRadioGroup>
          <NButton type="primary" size="large" @click="handleCreate">
            <template #icon>
              <NIcon :component="AddOutline" />
            </template>
            新增计划
          </NButton>
        </div>
      </template>
    </PageHero>

    <section class="grid gap-4 md:grid-cols-3">
      <MetricCard
        v-for="item in stats"
        :key="item.label"
        :label="item.label"
        :value="`${item.value}${item.unit}`"
        :helper="item.trendLabel"
        tone="neutral"
      />
    </section>

    <NCard class="border-white/80 bg-white/78 shadow-card" :bordered="false">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.16em] text-primary/70">
            Plan list
          </p>
          <h3 class="mt-2 text-2xl font-semibold text-text">当前计划</h3>
        </div>
        <StatusBadge :label="`共 ${filteredSchedules.length} 条`" tone="info" />
      </div>

      <div class="mt-5 space-y-4">
        <NSkeleton v-if="loading" text :repeat="4" />
        <NEmpty v-else-if="filteredSchedules.length === 0" description="暂无符合条件的计划" />
        <article
          v-else
          v-for="plan in filteredSchedules"
          :key="plan.id"
          class="rounded-[26px] border border-line/70 bg-[#fffcf6] p-5 shadow-card"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div class="space-y-3">
              <div class="flex flex-wrap items-center gap-3">
                <h4 class="text-xl font-semibold text-text">{{ plan.medicineName }}</h4>
                <StatusBadge :label="typeMap[plan.type]" tone="info" />
                <StatusBadge
                  :label="statusLabelMap[plan.status]"
                  :tone="plan.status === 'active' ? 'success' : plan.status === 'paused' ? 'warning' : 'neutral'"
                />
              </div>
              <p class="text-sm leading-6 text-muted">
                下一次提醒：{{ formatNextIntake(plan.nextIntake) }}
              </p>
            </div>
            <div class="flex flex-wrap gap-3">
              <NButton type="primary" size="small" @click="handleEdit(plan)">编辑</NButton>
              <NButton tertiary size="small" @click="handleToggle(plan.id)">
                {{ plan.status === 'active' ? '暂停' : '启用' }}
              </NButton>
            </div>
          </div>

          <div class="mt-5 grid gap-3 md:grid-cols-3">
            <div class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted">剂量与频次</p>
              <p class="mt-2 text-base font-semibold text-text">{{ plan.dosage }} · {{ plan.frequency }}</p>
            </div>
            <div class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted">时间窗</p>
              <p class="mt-2 text-base font-semibold text-text">{{ plan.window.start }} - {{ plan.window.end }}</p>
            </div>
            <div class="rounded-[20px] bg-white p-4 shadow-card">
              <p class="text-sm text-muted">周期</p>
              <p class="mt-2 text-base font-semibold text-text">{{ plan.period }}</p>
            </div>
          </div>
        </article>
      </div>
    </NCard>

    <!-- 新增计划Modal -->
    <NModal
      v-model:show="showCreateModal"
      preset="card"
      title="新增用药计划"
      style="width: 600px; max-width: 90vw"
      :bordered="false"
      size="large"
    >
      <NForm
        ref="createFormRef"
        :model="createFormData"
        :rules="createFormRules"
        label-placement="left"
        label-width="100"
        require-mark-placement="right-hanging"
        :show-feedback="true"
        :show-label="true"
      >
        <div class="space-y-4">
          <NFormItem label="药品名称" path="medicineName">
            <NInput
              v-model:value="createFormData.medicineName"
              placeholder="请输入药品名称，如：降压药"
              clearable
              size="medium"
            />
          </NFormItem>

          <NFormItem label="药品类型" path="type">
            <NSelect
              v-model:value="createFormData.type"
              :options="typeOptions"
              placeholder="请选择药品类型"
              size="medium"
            />
          </NFormItem>

          <NFormItem label="剂量" path="dosageAmount">
            <div class="flex items-center gap-2 w-full">
              <div class="flex-1">
                <NInputNumber
                  v-model:value="createFormData.dosageAmount"
                  placeholder="请输入数字"
                  :min="0.01"
                  :step="0.1"
                  :precision="2"
                  size="medium"
                  style="width: 100%"
                  clearable
                />
              </div>
              <div class="w-28 flex-shrink-0">
                <NSelect
                  v-model:value="createFormData.dosageUnit"
                  :options="dosageUnitOptions"
                  placeholder="单位"
                  size="medium"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="频次" path="frequency">
            <NInput
              v-model:value="createFormData.frequency"
              placeholder="请输入频次，如：每日3次"
              clearable
              size="medium"
            />
          </NFormItem>

          <NFormItem label="时间窗" path="windowStart">
            <div class="flex items-center gap-3 w-full">
              <div class="flex-1">
                <NTimePicker
                  v-model:value="createFormData.windowStart"
                  format="HH:mm"
                  placeholder="选择开始时间"
                  size="medium"
                  style="width: 100%"
                  clearable
                  @update:value="() => createFormRef?.validate()"
                />
              </div>
              <span class="text-muted text-sm whitespace-nowrap flex-shrink-0" style="line-height: 34px;">至</span>
              <div class="flex-1">
                <NTimePicker
                  v-model:value="createFormData.windowEnd"
                  format="HH:mm"
                  placeholder="选择结束时间"
                  size="medium"
                  style="width: 100%"
                  clearable
                  @update:value="() => createFormRef?.validate()"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="周期" path="periodAmount">
            <div class="flex items-center gap-2 w-full">
              <div class="flex-1" v-if="createFormData.periodUnit !== '持续'">
                <NInputNumber
                  v-model:value="createFormData.periodAmount"
                  placeholder="请输入数字"
                  :min="1"
                  :step="1"
                  :precision="0"
                  size="medium"
                  style="width: 100%"
                  clearable
                />
              </div>
              <div class="flex-1" v-else>
                <div class="h-[34px] flex items-center text-muted text-sm px-3 bg-slate-50 rounded border border-slate-200">
                  持续用药，无需输入天数
                </div>
              </div>
              <div class="w-28 flex-shrink-0">
                <NSelect
                  v-model:value="createFormData.periodUnit"
                  :options="periodUnitOptions"
                  placeholder="单位"
                  size="medium"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="初始状态" path="status">
            <NRadioGroup v-model:value="createFormData.status" size="medium">
              <NRadioButton value="active">启用</NRadioButton>
              <NRadioButton value="paused">暂停</NRadioButton>
            </NRadioGroup>
          </NFormItem>
        </div>
      </NForm>

      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton @click="showCreateModal = false">取消</NButton>
          <NButton type="primary" :loading="isSubmitting" @click="handleCreateSubmit">
            确定
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- 编辑计划Modal -->
    <NModal
      v-model:show="showEditModal"
      preset="card"
      title="编辑用药计划"
      style="width: 600px; max-width: 90vw"
      :bordered="false"
      size="large"
    >
      <NForm
        ref="editFormRef"
        :model="editFormData"
        :rules="editFormRules"
        label-placement="left"
        label-width="100"
        require-mark-placement="right-hanging"
        :show-feedback="true"
        :show-label="true"
      >
        <div class="space-y-4">
          <NFormItem label="药品名称" path="medicineName">
            <NInput
              v-model:value="editFormData.medicineName"
              placeholder="请输入药品名称，如：降压药"
              clearable
              size="medium"
            />
          </NFormItem>

          <NFormItem label="药品类型" path="type">
            <NSelect
              v-model:value="editFormData.type"
              :options="typeOptions"
              placeholder="请选择药品类型"
              size="medium"
            />
          </NFormItem>

          <NFormItem label="剂量" path="dosageAmount">
            <div class="flex items-center gap-2 w-full">
              <div class="flex-1">
                <NInputNumber
                  v-model:value="editFormData.dosageAmount"
                  placeholder="请输入数字"
                  :min="0.01"
                  :step="0.1"
                  :precision="2"
                  size="medium"
                  style="width: 100%"
                  clearable
                />
              </div>
              <div class="w-28 flex-shrink-0">
                <NSelect
                  v-model:value="editFormData.dosageUnit"
                  :options="dosageUnitOptions"
                  placeholder="单位"
                  size="medium"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="频次" path="frequency">
            <NInput
              v-model:value="editFormData.frequency"
              placeholder="请输入频次，如：每日3次"
              clearable
              size="medium"
            />
          </NFormItem>

          <NFormItem label="时间窗" path="windowStart">
            <div class="flex items-center gap-3 w-full">
              <div class="flex-1">
                <NTimePicker
                  v-model:value="editFormData.windowStart"
                  format="HH:mm"
                  placeholder="选择开始时间"
                  size="medium"
                  style="width: 100%"
                  clearable
                  @update:value="() => editFormRef?.validate()"
                />
              </div>
              <span class="text-muted text-sm whitespace-nowrap flex-shrink-0" style="line-height: 34px;">至</span>
              <div class="flex-1">
                <NTimePicker
                  v-model:value="editFormData.windowEnd"
                  format="HH:mm"
                  placeholder="选择结束时间"
                  size="medium"
                  style="width: 100%"
                  clearable
                  @update:value="() => editFormRef?.validate()"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="周期" path="periodAmount">
            <div class="flex items-center gap-2 w-full">
              <div class="flex-1" v-if="editFormData.periodUnit !== '持续'">
                <NInputNumber
                  v-model:value="editFormData.periodAmount"
                  placeholder="请输入数字"
                  :min="1"
                  :step="1"
                  :precision="0"
                  size="medium"
                  style="width: 100%"
                  clearable
                />
              </div>
              <div class="flex-1" v-else>
                <div class="h-[34px] flex items-center text-muted text-sm px-3 bg-slate-50 rounded border border-slate-200">
                  持续用药，无需输入天数
                </div>
              </div>
              <div class="w-28 flex-shrink-0">
                <NSelect
                  v-model:value="editFormData.periodUnit"
                  :options="periodUnitOptions"
                  placeholder="单位"
                  size="medium"
                />
              </div>
            </div>
          </NFormItem>

          <NFormItem label="状态" path="status">
            <NRadioGroup v-model:value="editFormData.status" size="medium">
              <NRadioButton value="active">启用</NRadioButton>
              <NRadioButton value="paused">暂停</NRadioButton>
            </NRadioGroup>
          </NFormItem>
        </div>
      </NForm>

      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton @click="showEditModal = false">取消</NButton>
          <NButton type="primary" :loading="isEditing" @click="handleEditSubmit">
            确定
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

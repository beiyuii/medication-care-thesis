<script setup lang="ts">
import { computed } from 'vue'
import {
  AlertCircleOutline,
  CheckmarkCircleOutline,
  InformationCircleOutline,
  WarningOutline,
} from '@vicons/ionicons5'

type StatusType = 'info' | 'success' | 'warning' | 'error'

/**
 * StatusBarProps 定义状态条的展示属性。
 */
interface StatusBarProps {
  /** title 为主提示语。 */
  title: string
  /** message 为补充说明，可选。 */
  message?: string
  /** type 控制配色。 */
  type?: StatusType
}

const props = withDefaults(defineProps<StatusBarProps>(), {
  type: 'info',
})

const typeConfig: Record<StatusType, { classes: string; icon: unknown }> = {
  info: { classes: 'bg-primary/10 text-primary', icon: InformationCircleOutline },
  success: { classes: 'bg-success/10 text-success', icon: CheckmarkCircleOutline },
  warning: { classes: 'bg-warning/10 text-warning', icon: WarningOutline },
  error: { classes: 'bg-danger/10 text-danger', icon: AlertCircleOutline },
}

const currentConfig = computed(() => typeConfig[props.type])
</script>

<template>
  <div
    :class="[
      'flex items-start justify-between gap-4 rounded-[24px] border border-white/70 px-5 py-4 shadow-card backdrop-blur',
      currentConfig.classes,
    ]"
  >
    <div class="flex items-start gap-3">
      <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/55">
        <NIcon :component="currentConfig.icon" size="18" />
      </div>
      <div>
        <p class="text-sm font-semibold">{{ title }}</p>
        <p v-if="message" class="mt-1 text-sm opacity-80">{{ message }}</p>
      </div>
    </div>
    <div v-if="$slots.action" class="shrink-0 text-right">
      <slot name="action" />
    </div>
  </div>
</template>

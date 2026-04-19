<script setup lang="ts">
type StateType = 'info' | 'success' | 'warning' | 'error'

/**
 * StateCardProps 描述带色条的卡片组件。
 */
interface StateCardProps {
  /** title 为标题文本。 */
  title: string
  /** statusLabel 展示当前状态字样。 */
  statusLabel: string
  /** statusType 控制配色。 */
  statusType?: StateType
  /** subtitle 为辅助说明。 */
  subtitle?: string
}

const props = withDefaults(defineProps<StateCardProps>(), {
  statusType: 'info',
})

const accentClassMap: Record<StateType, string> = {
  info: 'bg-primary/12 text-primary',
  success: 'bg-success/12 text-success',
  warning: 'bg-warning/14 text-warning',
  error: 'bg-danger/12 text-danger',
}
</script>

<template>
  <div
    class="relative overflow-hidden rounded-[26px] border border-line/70 bg-panel/90 p-5 shadow-card"
  >
    <div
      class="pointer-events-none absolute left-0 top-0 h-full w-1.5"
      :class="accentClassMap[props.statusType]"
    />
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="text-base font-semibold text-text">{{ title }}</p>
        <p v-if="subtitle" class="mt-1 text-sm leading-6 text-muted">{{ subtitle }}</p>
      </div>
      <span
        class="rounded-pill px-3 py-1 text-xs font-semibold ring-1 ring-inset"
        :class="accentClassMap[props.statusType]"
      >
        {{ statusLabel }}
      </span>
    </div>
    <div class="mt-4 text-sm leading-6 text-text">
      <slot />
    </div>
  </div>
</template>

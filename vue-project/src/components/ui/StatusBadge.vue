<script setup lang="ts">
type BadgeTone = 'info' | 'success' | 'warning' | 'danger' | 'neutral'

interface StatusBadgeProps {
  label: string
  tone?: BadgeTone
}

const props = withDefaults(defineProps<StatusBadgeProps>(), {
  tone: 'info',
})

const toneMap: Record<BadgeTone, string> = {
  info: 'bg-primary/12 text-primary ring-primary/15',
  success: 'bg-success/12 text-success ring-success/15',
  warning: 'bg-warning/14 text-warning ring-warning/20',
  danger: 'bg-danger/12 text-danger ring-danger/18',
  neutral: 'bg-slate-100 text-muted ring-slate-200',
}
</script>

<template>
  <span
    class="inline-flex items-center gap-2 rounded-pill px-3 py-1.5 text-xs font-semibold ring-1 ring-inset"
    :class="toneMap[props.tone]"
  >
    <span class="h-1.5 w-1.5 rounded-full bg-current opacity-70" />
    {{ label }}
  </span>
</template>

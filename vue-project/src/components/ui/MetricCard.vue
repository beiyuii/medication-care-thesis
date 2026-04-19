<script setup lang="ts">
import type { Component } from 'vue'

type MetricTone = 'brand' | 'success' | 'warning' | 'danger' | 'neutral'

interface MetricCardProps {
  label: string
  value: string | number
  helper?: string
  tone?: MetricTone
  icon?: Component
}

const props = withDefaults(defineProps<MetricCardProps>(), {
  helper: '',
  tone: 'brand',
  icon: undefined,
})

const toneMap: Record<MetricTone, { shell: string; chip: string; value: string }> = {
  brand: {
    shell: 'border-primary/15 bg-white/85',
    chip: 'bg-primary/12 text-primary',
    value: 'text-primary',
  },
  success: {
    shell: 'border-success/15 bg-white/85',
    chip: 'bg-success/12 text-success',
    value: 'text-success',
  },
  warning: {
    shell: 'border-warning/20 bg-white/85',
    chip: 'bg-warning/14 text-warning',
    value: 'text-warning',
  },
  danger: {
    shell: 'border-danger/18 bg-white/85',
    chip: 'bg-danger/12 text-danger',
    value: 'text-danger',
  },
  neutral: {
    shell: 'border-line/80 bg-white/85',
    chip: 'bg-slate-100 text-muted',
    value: 'text-text',
  },
}
</script>

<template>
  <article
    class="rounded-[26px] border p-5 shadow-card transition-transform duration-200 hover:-translate-y-0.5"
    :class="toneMap[props.tone].shell"
  >
    <div class="flex items-start justify-between gap-4">
      <div class="space-y-3">
        <p class="text-sm font-medium text-muted">{{ label }}</p>
        <div class="space-y-1">
          <p class="text-3xl font-semibold tracking-tight" :class="toneMap[props.tone].value">
            {{ value }}
          </p>
          <p v-if="helper" class="text-sm leading-6 text-muted">
            {{ helper }}
          </p>
        </div>
      </div>
      <div
        v-if="icon"
        class="flex h-11 w-11 items-center justify-center rounded-2xl"
        :class="toneMap[props.tone].chip"
      >
        <NIcon :component="icon" size="20" />
      </div>
    </div>
    <div v-if="$slots.default" class="mt-4 border-t border-line/60 pt-4 text-sm text-text">
      <slot />
    </div>
  </article>
</template>

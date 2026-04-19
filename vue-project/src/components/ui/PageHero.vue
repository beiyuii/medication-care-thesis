<script setup lang="ts">
type HeroTone = 'brand' | 'soft' | 'warning' | 'success'

interface PageHeroProps {
  eyebrow?: string
  title: string
  description?: string
  tone?: HeroTone
}

const props = withDefaults(defineProps<PageHeroProps>(), {
  eyebrow: '',
  description: '',
  tone: 'brand',
})

const toneMap: Record<HeroTone, string> = {
  brand:
    'from-primary/18 via-white/90 to-accent/15 border-primary/15',
  soft:
    'from-white/95 via-white/80 to-slate-100/70 border-line/80',
  warning:
    'from-warning/15 via-white/90 to-accent/12 border-warning/20',
  success:
    'from-success/15 via-white/90 to-primary/12 border-success/20',
}
</script>

<template>
  <section
    class="relative overflow-hidden rounded-[32px] border bg-gradient-to-br px-6 py-6 shadow-soft md:px-8 md:py-8"
    :class="toneMap[props.tone]"
  >
    <div class="pointer-events-none absolute -right-10 top-0 h-36 w-36 rounded-full bg-white/40 blur-3xl" />
    <div class="pointer-events-none absolute bottom-0 left-0 h-28 w-28 rounded-full bg-primary/10 blur-2xl" />
    <div class="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-3xl space-y-3">
        <p v-if="eyebrow" class="text-xs font-semibold uppercase tracking-[0.2em] text-primary/80">
          {{ eyebrow }}
        </p>
        <div class="space-y-2">
          <h1 class="text-3xl font-semibold tracking-tight text-text md:text-4xl">
            {{ title }}
          </h1>
          <p v-if="description" class="max-w-2xl text-base leading-7 text-muted md:text-lg">
            {{ description }}
          </p>
        </div>
        <div v-if="$slots.meta" class="flex flex-wrap gap-3 pt-1">
          <slot name="meta" />
        </div>
      </div>
      <div v-if="$slots.actions" class="flex flex-wrap gap-3 lg:justify-end">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>

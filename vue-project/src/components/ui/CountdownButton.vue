<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * CountdownButtonProps 控制倒计时确认按钮行为。
 */
interface CountdownButtonProps {
  /** label 为按钮文案。 */
  label: string
  /** duration 为倒计时时长（秒）。 */
  duration?: number
}

const props = withDefaults(defineProps<CountdownButtonProps>(), {
  duration: 3,
})

const emit = defineEmits<{
  /** confirm 事件在倒计时结束并点击按钮时触发。 */
  confirm: []
}>()

const remainingSeconds = ref(props.duration)
const isCounting = ref(true)
let timer: ReturnType<typeof setInterval> | null = null

const startCountdown = () => {
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value -= 1
    }
    if (remainingSeconds.value === 0 && timer) {
      clearInterval(timer)
      timer = null
      isCounting.value = false
    }
  }, 1000)
}

const handleConfirm = () => {
  if (isCounting.value) {
    return
  }
  emit('confirm')
  remainingSeconds.value = props.duration
  isCounting.value = true
  startCountdown()
}

onMounted(() => {
  startCountdown()
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <NButton
    block
    type="primary"
    size="large"
    :disabled="isCounting"
    :loading="isCounting"
    @click="handleConfirm"
  >
    {{ isCounting ? `确认中 (${remainingSeconds}s)` : label }}
  </NButton>
</template>

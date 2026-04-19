<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { login, register } from '@/services/authService'
import { extractErrorMessage, logError } from '@/utils/errorHandler'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const isRegisterMode = ref(false)
const isSubmitting = ref(false)

/**
 * LoginFormModel 描述登录/注册表单字段。
 */
interface FormModel {
  /** username 为登录/注册账户名 */
  username: string
  /** password 为明文输入密码 */
  password: string
  /** role 记录注册时选择的角色 */
  role: 'elder' | 'caregiver' | 'child'
  /** remember 表示是否记住登录状态 */
  remember: boolean
}

const formModel = reactive<FormModel>({
  username: '',
  password: '',
  role: 'elder',
  remember: true,
})

const canSubmit = computed(
  () => formModel.username.trim().length > 0 && formModel.password.length >= 6,
)

/**
 * quickLoginPresets 为与后端种子数据一致的演示账号，用于登录页一键登录。
 */
const quickLoginPresets = [
  { label: '老人端', username: 'elder1', password: '123456' },
  { label: '护工端', username: 'care1', password: '123456' },
  { label: '子女端', username: 'child1', password: '123456' },
] as const

/**
 * handleQuickLogin 填入演示账号并触发登录（仅登录模式可用）。
 *
 * @param username 演示用户名
 * @param password 演示密码
 */
const handleQuickLogin = async (username: string, password: string) => {
  if (isRegisterMode.value || isSubmitting.value) {
    return
  }
  formModel.username = username
  formModel.password = password
  await nextTick()
  await handleSubmit()
}

/**
 * handleSubmit 处理登录或注册提交
 */
const handleSubmit = async () => {
  if (!canSubmit.value) {
    message.warning('请输入用户名和密码（不少于6位）')
    return
  }

  isSubmitting.value = true
  try {
    // 如果用户已经登录，先清除旧的 token 和会话信息
    // 确保切换账号时不会残留旧 token
    if (authStore.isAuthenticated) {
      authStore.clearSession()
    }

    if (isRegisterMode.value) {
      // 注册模式
      const res = await register({
        username: formModel.username,
        password: formModel.password,
        role: formModel.role,
      })
      authStore.setSession({
        token: res.token,
        user: {
          id: String(res.userId),
          name: res.displayName,
          role: res.role as 'elder' | 'caregiver' | 'child',
        },
      })
      message.success('注册成功！')
    } else {
      // 登录模式
      const res = await login({
        username: formModel.username,
        password: formModel.password,
      })
      authStore.setSession({
        token: res.token,
        user: {
          id: String(res.userId),
          name: res.displayName,
          role: res.role as 'elder' | 'caregiver' | 'child',
        },
      })
      message.success('登录成功！')
    }

    // 跳转到对应角色首页
    const roleHomeMap = {
      elder: '/elder/home',
      caregiver: '/caregiver/home',
      child: '/child/home',
    }
    // 确保 role 存在，否则使用默认首页
    const targetRole = authStore.role ?? 'elder'
    await router.push(roleHomeMap[targetRole] ?? '/elder/home')
  } catch (error: unknown) {
    // 提取错误消息
    const errorMsg = extractErrorMessage(error, '操作失败，请重试')
    message.error(errorMsg)

    // 开发环境下输出详细错误信息
    logError(error, '登录/注册')
  } finally {
    isSubmitting.value = false
  }
}

/**
 * toggleMode 切换登录/注册模式
 */
const toggleMode = () => {
  isRegisterMode.value = !isRegisterMode.value
  formModel.username = ''
  formModel.password = ''
}
</script>

<template>
  <div class="min-h-screen bg-paper-glow px-4 py-8 md:px-6 md:py-10">
    <div class="mx-auto grid max-w-7xl gap-8 md:grid-cols-[1.08fr_0.92fr]">
      <section class="relative overflow-hidden rounded-[36px] border border-white/80 bg-white/72 px-6 py-8 shadow-float backdrop-blur md:px-8 md:py-10">
        <div class="pointer-events-none absolute -right-10 top-0 h-40 w-40 rounded-full bg-primary/15 blur-3xl" />
        <div class="pointer-events-none absolute bottom-0 left-0 h-32 w-32 rounded-full bg-accent/20 blur-3xl" />
        <div class="relative flex h-full flex-col justify-between gap-8">
          <div class="space-y-6">
            <div class="inline-flex rounded-pill bg-primary/10 px-4 py-2 text-sm font-semibold text-primary">
              温和医疗感 · 适老化工作台
            </div>
            <div class="space-y-4">
              <p class="text-xs font-semibold uppercase tracking-[0.24em] text-primary/70">
                Medication care system
              </p>
              <h1 class="max-w-xl text-4xl font-semibold tracking-tight text-text md:text-5xl">
                基于目标检测的用药提醒与管理
              </h1>
              <p class="max-w-2xl text-base leading-8 text-muted md:text-lg">
                把提醒、检测、确认和异常处理整合到一个更容易看懂的健康辅助流程里，减少老人端和照护端的认知负担。
              </p>
            </div>
            <div class="grid gap-4 md:grid-cols-3">
              <div class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-5 shadow-card">
                <p class="text-sm font-semibold text-text">实时检测</p>
                <p class="mt-2 text-sm leading-7 text-muted">摄像头记录服药动作，结果回流到提醒实例。</p>
              </div>
              <div class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-5 shadow-card">
                <p class="text-sm font-semibold text-text">异常追踪</p>
                <p class="mt-2 text-sm leading-7 text-muted">所有异常都能在工作台与告警中心内追溯处理。</p>
              </div>
              <div class="rounded-[24px] border border-line/70 bg-[#fffcf6] p-5 shadow-card">
                <p class="text-sm font-semibold text-text">多人协同</p>
                <p class="mt-2 text-sm leading-7 text-muted">护工和子女共享统一的患者状态视图。</p>
              </div>
            </div>
          </div>

          <NAlert type="info" class="border-white/80 bg-white/70" title="隐私提示" :show-icon="true">
            摄像头画面仅用于本次检测任务，系统主要保留事件信息与必要的关键帧日志。
          </NAlert>
        </div>
      </section>

      <section class="rounded-[36px] border border-white/80 bg-white/84 p-4 shadow-float backdrop-blur md:p-5">
        <NCard
          class="h-full border border-line/70 bg-[#fffdfa] shadow-none"
          size="large"
          :title="isRegisterMode ? '注册账号' : '登录系统'"
        >
          <template #header-extra>
            <span class="rounded-pill bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              {{ isRegisterMode ? 'New account' : 'Welcome back' }}
            </span>
          </template>

          <NForm class="space-y-5" @submit.prevent="handleSubmit">
            <NFormItem label="用户名">
              <NInput
                v-model:value="formModel.username"
                size="large"
                placeholder="请输入用户名"
                clearable
              />
            </NFormItem>

            <NFormItem label="密码">
              <NInput
                v-model:value="formModel.password"
                type="password"
                show-password-on="click"
                size="large"
                placeholder="不少于 6 位数字或字母"
              />
            </NFormItem>

            <!-- 注册模式下显示角色选择 -->
            <NFormItem v-if="isRegisterMode" label="选择角色">
              <NRadioGroup v-model:value="formModel.role" class="flex flex-wrap gap-3">
                <NRadioButton value="elder">老年人</NRadioButton>
                <NRadioButton value="caregiver">护工</NRadioButton>
                <NRadioButton value="child">子女</NRadioButton>
              </NRadioGroup>
            </NFormItem>

            <!-- 登录模式下显示记住状态 -->
            <div v-if="!isRegisterMode" class="flex items-center justify-between">
              <div class="flex items-center gap-3 text-sm text-muted">
                <NSwitch v-model:value="formModel.remember" size="small" />
                <span>记住登录状态</span>
              </div>
              <RouterLink to="/settings" class="text-primary text-sm">浏览器权限指南</RouterLink>
            </div>

            <div
              v-if="!isRegisterMode"
              class="rounded-2xl border border-line/60 bg-white/60 px-4 py-3"
            >
              <p class="mb-2 text-xs font-medium text-muted">
                快捷登录（演示账号，与种子数据一致）
              </p>
              <div class="flex flex-wrap gap-2">
                <NButton
                  v-for="preset in quickLoginPresets"
                  :key="preset.username"
                  size="small"
                  tertiary
                  :disabled="isSubmitting"
                  @click="handleQuickLogin(preset.username, preset.password)"
                >
                  {{ preset.label }} · {{ preset.username }}
                </NButton>
              </div>
            </div>

            <NButton
              block
              size="large"
              type="primary"
              :disabled="!canSubmit"
              :loading="isSubmitting"
              @click="handleSubmit"
            >
              {{ isRegisterMode ? '注册' : '登录' }}
            </NButton>

            <!-- 切换登录/注册模式 -->
            <div class="mt-4 text-center text-sm text-muted">
              {{ isRegisterMode ? '已有账号？' : '还没有账号？' }}
              <NButton text type="primary" @click="toggleMode">
                {{ isRegisterMode ? '立即登录' : '立即注册' }}
              </NButton>
            </div>
          </NForm>
        </NCard>
      </section>
    </div>
  </div>
</template>

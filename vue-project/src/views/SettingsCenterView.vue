<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import type { UserSetting } from '@/types/settings'
import { fetchUserSettings, updateUserSettings } from '@/services/settingsService'
import { getProfile } from '@/services/authService'
import { useRequest } from '@/composables/useRequest'
import { extractErrorMessage, logError } from '@/utils/errorHandler'
import type { UserProfileResponse } from '@/types/auth'

/** formModel 保存设置表单。 */
const formModel = reactive<UserSetting>({
  reminder: { enableVoice: true, advanceMinutes: 5, volume: 80 },
  detection: { autoStart: true, lowLightEnhance: false, fallbackMode: 'webgpu' },
  privacy: { cameraPermission: true, uploadConsent: true, shareToCaregiver: true },
})

/** loading 控制初次加载状态。 */
const loading = ref(true)
/** saving 控制保存按钮 loading。 */
const saving = ref(false)
/** profileLoading 控制用户信息加载状态。 */
const profileLoading = ref(false)
/** userProfile 保存用户详细信息。 */
const userProfile = ref<UserProfileResponse | null>(null)
const message = useMessage()

const fallbackOptions = [
  { label: 'WebGPU 优先', value: 'webgpu' },
  { label: 'WebGL 回退', value: 'webgl' },
  { label: 'WASM 兼容', value: 'wasm' },
]

const { execute: loadSettings } = useRequest(fetchUserSettings, {
  immediate: true,
  onSuccess(data) {
    Object.assign(formModel.reminder, data.reminder)
    Object.assign(formModel.detection, data.detection)
    Object.assign(formModel.privacy, data.privacy)
    loading.value = false
  },
  onError(error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载设置失败')
    message.error(errorMsg)
    logError(error, '加载设置')
    loading.value = false
  },
})

const handleSubmit = async () => {
  saving.value = true
  try {
    const updatedSettings = await updateUserSettings(formModel)
    // 更新表单数据为服务器返回的最新值
    Object.assign(formModel.reminder, updatedSettings.reminder)
    Object.assign(formModel.detection, updatedSettings.detection)
    Object.assign(formModel.privacy, updatedSettings.privacy)
    message.success('设置已保存')
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '保存失败')
    message.error(errorMsg)
    logError(error, '保存设置')
  } finally {
    saving.value = false
  }
}

/**
 * loadUserProfile 加载用户详细信息
 */
const loadUserProfile = async () => {
  profileLoading.value = true
  try {
    const profile = await getProfile()
    userProfile.value = profile
  } catch (error: unknown) {
    const errorMsg = extractErrorMessage(error, '加载用户信息失败')
    message.error(errorMsg)
    logError(error, '加载用户信息')
  } finally {
    profileLoading.value = false
  }
}

onMounted(() => {
  loadSettings()
  loadUserProfile()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center">
      <h2 class="text-2xl font-semibold text-text">系统设置</h2>
    </div>

    <!-- 账户信息卡片 -->
    <NCard class="shadow-card" title="账户信息">
      <NSkeleton v-if="profileLoading" text :repeat="4" />
      <div v-else-if="userProfile" class="space-y-4">
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <p class="text-sm text-muted mb-1">用户ID</p>
            <p class="text-text font-medium">{{ userProfile.userId }}</p>
          </div>
          <div>
            <p class="text-sm text-muted mb-1">用户名</p>
            <p class="text-text font-medium">{{ userProfile.username }}</p>
          </div>
          <div>
            <p class="text-sm text-muted mb-1">姓名</p>
            <p class="text-text font-medium">{{ userProfile.name }}</p>
          </div>
          <div>
            <p class="text-sm text-muted mb-1">角色</p>
            <NTag :type="userProfile.role === 'elder' ? 'success' : 'info'">
              {{
                userProfile.role === 'elder'
                  ? '老年人'
                  : userProfile.role === 'caregiver'
                    ? '护工'
                    : '子女'
              }}
            </NTag>
          </div>
        </div>
        <!-- 关联患者列表（护工/子女角色显示） -->
        <div v-if="userProfile.patients && userProfile.patients.length > 0">
          <p class="text-sm text-muted mb-2">关联患者</p>
          <div class="grid gap-2">
            <NCard
              v-for="patient in userProfile.patients"
              :key="patient.id"
              class="border border-slate-100"
            >
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-text font-medium">{{ patient.name }}</p>
                  <p class="text-sm text-muted">
                    下次服药：{{ new Date(patient.nextIntakeTime).toLocaleString('zh-CN') }}
                  </p>
                </div>
                <div class="text-right">
                  <NTag
                    :type="patient.planStatus === 'active' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ patient.planStatus === 'active' ? '进行中' : '已暂停' }}
                  </NTag>
                  <p v-if="patient.alertCount > 0" class="text-danger text-sm mt-1">
                    {{ patient.alertCount }} 条告警
                  </p>
                </div>
              </div>
            </NCard>
          </div>
        </div>
        <div v-else-if="userProfile.role === 'elder'" class="text-sm text-muted">
          当前账户为老年人角色，无需关联患者
        </div>
      </div>
      <div v-else class="text-muted text-center py-4">暂无用户信息</div>
    </NCard>

    <NCard class="shadow-card" title="提醒设置">
      <NSkeleton v-if="loading" text :repeat="3" />
      <div v-else class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">语音播报</p>
            <p class="text-sm text-muted">开启后在时间窗自动播放语音提示</p>
          </div>
          <NSwitch v-model:value="formModel.reminder.enableVoice" />
        </div>
        <NForm class="grid gap-4 md:grid-cols-2">
          <NFormItem label="提前提醒时间（分钟）">
            <NInputNumber v-model:value="formModel.reminder.advanceMinutes" :min="0" :max="30" />
          </NFormItem>
          <NFormItem label="语音音量">
            <NInputNumber v-model:value="formModel.reminder.volume" :min="0" :max="100" />
          </NFormItem>
        </NForm>
      </div>
    </NCard>

    <NCard class="shadow-card" title="检测设置">
      <NSkeleton v-if="loading" text :repeat="3" />
      <div v-else class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">时间窗自动开启检测</p>
            <p class="text-sm text-muted">时间窗内将自动打开摄像头并加载模型</p>
          </div>
          <NSwitch v-model:value="formModel.detection.autoStart" />
        </div>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">低光增强模式</p>
            <p class="text-sm text-muted">在弱光环境下自动提升亮度</p>
          </div>
          <NSwitch v-model:value="formModel.detection.lowLightEnhance" />
        </div>
        <NFormItem label="推理回退模式">
          <NSelect v-model:value="formModel.detection.fallbackMode" :options="fallbackOptions" />
        </NFormItem>
      </div>
    </NCard>

    <NCard class="shadow-card" title="隐私与共享">
      <NSkeleton v-if="loading" text :repeat="3" />
      <div v-else class="space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">摄像头权限</p>
            <p class="text-sm text-muted">若关闭将无法进入检测页</p>
          </div>
          <NSwitch v-model:value="formModel.privacy.cameraPermission" />
        </div>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">上传关键帧日志</p>
            <p class="text-sm text-muted">只会上传模糊处理后的关键帧用于追溯</p>
          </div>
          <NSwitch v-model:value="formModel.privacy.uploadConsent" />
        </div>
        <div class="flex items-center justify-between">
          <div>
            <p class="text-text font-medium">与护工/子女共享</p>
            <p class="text-sm text-muted">允许关联账号查看实时状态与历史</p>
          </div>
          <NSwitch v-model:value="formModel.privacy.shareToCaregiver" />
        </div>
      </div>
    </NCard>

    <!-- 保存按钮区域 -->
    <div class="flex justify-end pt-4 pb-6 border-t border-slate-200">
      <NButton type="primary" size="large" :loading="saving" @click="handleSubmit">
        保存设置
      </NButton>
    </div>
  </div>
</template>

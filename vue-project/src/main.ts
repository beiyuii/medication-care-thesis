import { createApp } from 'vue'
import App from './App.vue'
import './style.css'
import { naive } from '@/plugins/naive'
import router from '@/router'
import pinia from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from '@/services/authService'
import { validateStoredSession } from '@/bootstrap/sessionValidation'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(naive)

/**
 * validateToken 在应用启动时验证 token 是否有效
 */
async function validateToken() {
  const authStore = useAuthStore()

  await validateStoredSession(authStore, getProfile)
}

// 应用启动时验证 token（不阻塞应用启动）
validateToken().catch(error => {
  console.error('Token validation error:', error)
})

app.mount('#app')

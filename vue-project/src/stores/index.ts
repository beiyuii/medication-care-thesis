import { createPinia } from 'pinia'

/**
 * piniaInstance 作为全局唯一的 Pinia 容器，供 main.ts 与其他模块共享。
 */
const piniaInstance = createPinia()

export default piniaInstance

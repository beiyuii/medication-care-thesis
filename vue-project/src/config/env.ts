/**
 * appConfig 统一暴露运行时环境变量，便于在不同模块中调用。
 */
export const appConfig = {
  /** apiBaseUrl 为业务接口地址。开发环境使用相对路径走 Vite 代理，生产环境使用完整 URL。 */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? '/api' : 'http://localhost:8080/api'),
  /** uploadBaseUrl 用于日志图片上传。开发环境使用相对路径走 Vite 代理，生产环境使用完整 URL。 */
  uploadBaseUrl: import.meta.env.VITE_UPLOAD_BASE_URL ?? (import.meta.env.DEV ? '/uploads' : 'http://localhost:8080/uploads'),
  /** detectionBaseUrl 为算法检测服务地址。开发环境使用相对路径走 Vite 代理，生产环境使用完整 URL。 */
  /** 注意：Flask 后端运行在 8000 端口，路径为 /v1/detections 和 /v1/videos，所以使用空字符串 */
  detectionBaseUrl: import.meta.env.VITE_DETECTION_BASE_URL ?? (import.meta.env.DEV ? '' : 'http://localhost:8000'),
}

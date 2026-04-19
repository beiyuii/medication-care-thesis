import { ref } from 'vue'

interface UseRequestOptions<T> {
  /** immediate 表示是否在创建时立即执行 */
  immediate?: boolean
  /** onSuccess 成功回调 */
  onSuccess?: (data: T) => void
  /** onError 失败回调 */
  onError?: (error: { status: number; message: string }) => void
}

/**
 * useRequest 封装常见的加载、错误状态处理。
 */
export function useRequest<T>(requestFn: () => Promise<T>, options?: UseRequestOptions<T>) {
  const loading = ref(false)
  const error = ref<{ status: number; message: string } | null>(null)
  const data = ref<T | null>(null)

  const execute = async () => {
    loading.value = true
    error.value = null
    try {
      const result = await requestFn()
      data.value = result
      options?.onSuccess?.(result)
    } catch (err) {
      const detail = err as { status: number; message: string }
      error.value = detail
      options?.onError?.(detail)
    } finally {
      loading.value = false
    }
  }

  if (options?.immediate) {
    execute()
  }

  return {
    loading,
    error,
    data,
    execute,
  }
}

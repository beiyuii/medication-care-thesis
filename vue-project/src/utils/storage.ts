/**
 * storage 提供安全的 localStorage 操作，包含异常处理和环境检查。
 */
export const storage = {
  /**
   * getItem 安全地获取 localStorage 中的值
   * @param key 存储键名
   * @returns 存储的值，如果不存在或出错则返回 null
   */
  getItem(key: string): string | null {
    try {
      return typeof window !== 'undefined' && window.localStorage
        ? localStorage.getItem(key)
        : null
    } catch (error) {
      console.warn(`Failed to get ${key} from localStorage:`, error)
      return null
    }
  },

  /**
   * setItem 安全地设置 localStorage 中的值
   * @param key 存储键名
   * @param value 要存储的值
   */
  setItem(key: string, value: string): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.setItem(key, value)
      }
    } catch (error) {
      console.warn(`Failed to set ${key} to localStorage:`, error)
      // 常见原因：存储空间已满、隐私模式等
    }
  },

  /**
   * removeItem 安全地删除 localStorage 中的值
   * @param key 存储键名
   */
  removeItem(key: string): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.removeItem(key)
      }
    } catch (error) {
      console.warn(`Failed to remove ${key} from localStorage:`, error)
    }
  },

  /**
   * clear 安全地清空所有 localStorage
   */
  clear(): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.clear()
      }
    } catch (error) {
      console.warn('Failed to clear localStorage:', error)
    }
  },
}


import { ref } from 'vue'

export type ToastType = 'success' | 'error'

export interface ToastItem {
  id: number
  type: ToastType
  message: string
}

/** 模块级状态，全局单例；与 useAuthDialog 同一做法 */
const toasts = ref<ToastItem[]>([])
let seed = 0

function push(type: ToastType, message: string, duration = 2800): void {
  const id = ++seed
  toasts.value.push({ id, type, message })
  setTimeout(() => {
    toasts.value = toasts.value.filter((item) => item.id !== id)
  }, duration)
}

export function useToast() {
  return {
    toasts,
    success: (message: string) => push('success', message),
    error: (message: string) => push('error', message),
  }
}

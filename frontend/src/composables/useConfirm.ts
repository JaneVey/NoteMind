import { ref } from 'vue'

export interface ConfirmOptions {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
  /** 二次确认用：额外显示一条不可撤销的红色警示 */
  severe?: boolean
}

interface ConfirmState extends ConfirmOptions {
  resolve: (ok: boolean) => void
}

/** 模块级状态，全局单例 */
const state = ref<ConfirmState | null>(null)

export function useConfirm() {
  function confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      state.value = { ...options, resolve }
    })
  }

  function settle(ok: boolean): void {
    state.value?.resolve(ok)
    state.value = null
  }

  return { state, confirm, settle }
}

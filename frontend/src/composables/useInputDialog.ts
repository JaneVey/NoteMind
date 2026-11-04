import { ref } from 'vue'

export interface InputDialogOptions {
  title: string
  placeholder?: string
  initialValue?: string
  multiline?: boolean
  allowEmpty?: boolean
  confirmText?: string
}

interface InputDialogState extends InputDialogOptions {
  resolve: (value: string | null) => void
}

/** 模块级状态，全局单例；与 useConfirm / useAuthDialog 一致 */
const state = ref<InputDialogState | null>(null)

export function useInputDialog() {
  /** @returns 用户输入的文本；点取消或遮罩返回 null */
  function open(options: InputDialogOptions): Promise<string | null> {
    return new Promise<string | null>((resolve) => {
      state.value = { ...options, resolve }
    })
  }

  function settle(value: string | null): void {
    state.value?.resolve(value)
    state.value = null
  }

  return { state, open, settle }
}

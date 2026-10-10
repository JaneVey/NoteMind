import { ref } from 'vue'

/**
 * 全局认证弹框的状态。
 *
 * <p>弹框显隐是界面状态，不是领域状态，所以用模块级 ref 而不是 Pinia ——
 * 模块级 ref 天然是全局单例，任何组件 import 后拿到的都是同一份。
 *
 * <p>open() 返回 Promise，调用方才能"等登录完成再继续"：
 * <pre>
 *   if (!(await requireAuth({ reason: '发送消息' }))) return
 *   await chat.send(inputText)      // 登录成功后自动续做
 * </pre>
 */

export type AuthTab = 'login' | 'register'

/** 弹框是否可见 —— 模块级，全局唯一 */
const visible = ref(false)

/** 当前标签页 */
const tab = ref<AuthTab>('login')

/** 为什么需要登录（显示给用户，例如"发送消息需要先登录"） */
const reason = ref('')

/**
 * 等待弹框结果的调用方。
 *
 * <p>用数组而不是单个变量：可能同时有多个动作在等待（例如点了两个需要登录的按钮），
 * 它们都应得到结果。
 */
let resolvers: Array<(ok: boolean) => void> = []

export function useAuthDialog() {
  /**
   * 打开弹框并等待用户操作。
   *
   * @returns 登录/注册成功返回 true；用户取消（点遮罩、Esc、关闭）返回 false
   */
  function open(options: { tab?: AuthTab; reason?: string } = {}): Promise<boolean> {
    tab.value = options.tab ?? 'login'
    reason.value = options.reason ?? ''
    visible.value = true
    return new Promise<boolean>((resolve) => {
      resolvers.push(resolve)
    })
  }

  /** 结束弹框并兑现所有等待者。由弹框组件在成功/取消时调用 */
  function settle(ok: boolean): void {
    visible.value = false
    const pending = resolvers
    resolvers = []
    pending.forEach((resolve) => resolve(ok))
  }

  function switchTab(next: AuthTab): void {
    tab.value = next
  }

  return { visible, tab, reason, open, settle, switchTab }
}

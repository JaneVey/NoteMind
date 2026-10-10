import { ref } from 'vue'

/**
 * 全局认证弹框的状态。
 *
 * <h3>为什么用「模块级 ref」而不是 Pinia</h3>
 *
 * 弹框显隐是**界面状态**，不是领域状态 —— 按《前端开发规范》，
 * 这类状态不该进 Pinia。而模块级的 `ref` 天然是全局单例，
 * 任何组件 import 后拿到的都是同一份，正好满足需求。
 *
 * <h3>为什么需要 Promise 语义</h3>
 *
 * 这是「游客模式」体验的核心。调用方要能这样写：
 * <pre>
 *   if (!(await requireAuth({ reason: '发送消息' }))) return
 *   await chat.send(inputText)      // 登录成功后自动续做
 * </pre>
 * 即"等用户登录完成再继续"。所以 {@link open} 返回 Promise，
 * 弹框关闭时（无论成功还是取消）兑现它。
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
 * <p>用数组而不是单个变量：可能同时有多个动作在等待
 * （例如用户点了两个需要登录的按钮），它们都应得到结果。
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

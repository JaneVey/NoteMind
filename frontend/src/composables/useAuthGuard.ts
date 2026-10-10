import { useAuthStore } from '@/stores/authStore'
import { useAuthDialog } from './useAuthDialog'

/**
 * 「需要登录才能做这件事」的统一入口，在动作层判断，不用路由守卫拦截页面。
 *
 * <p>比路由守卫好在三点：「哪里需要登录」在代码里是显式的；能做「登录后自动续做」
 * （点了发送 → 弹框登录 → 回来后消息自动发出去，输入内容不丢）；弹框不破坏用户所处的
 * 上下文（滚动位置、已填内容）。
 *
 * <pre>
 *   async function sendMessage() {
 *     if (!(await requireAuth({ reason: '发送消息' }))) return
 *     await chat.send(inputText)
 *   }
 * </pre>
 */
export function useAuthGuard() {
  const auth = useAuthStore()
  const dialog = useAuthDialog()

  /**
   * 确保当前已登录。已登录时立即返回 true、不弹框，所以可以在每个动作前放心调用。
   *
   * @returns 调用方是否可以继续执行原动作
   */
  async function requireAuth(options: { reason?: string } = {}): Promise<boolean> {
    if (auth.isLoggedIn) {
      return true
    }

    const loggedIn = await dialog.open({ reason: options.reason })

    // 双重确认：用户可能在弹框关闭前就已登录成功
    return loggedIn && auth.isLoggedIn
  }

  return { requireAuth }
}

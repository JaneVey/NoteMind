import { useAuthStore } from '@/stores/authStore'
import { useAuthDialog } from './useAuthDialog'

/**
 * 「需要登录才能做这件事」的统一入口。
 *
 * <h3>为什么用这个，而不是路由守卫</h3>
 *
 * 旧实现是路由守卫拦截**所有页面**，未登录一律跳登录页。
 * 那是后台管理系统的思路，对 C 端产品是反模式 ——
 * 市面产品（ChatGPT、Claude、Notion）都让用户**先用起来**，到需要身份时才索取。
 *
 * 改成在**动作层**判断后，好处有三：
 * <ol>
 *   <li>「哪里需要登录」在代码里是**显式**的，一眼可见，不依赖路由表这种间接映射</li>
 *   <li>可以做到「登录后自动续做」—— 用户点了发送、弹框登录、回来后消息自动发出去，
 *       输入的内容一点不丢</li>
 *   <li>弹框而非整页跳转，不破坏用户所处的上下文（滚动位置、已填内容）</li>
 * </ol>
 *
 * <h3>用法</h3>
 * <pre>
 * async function sendMessage() {
 *   if (!(await requireAuth({ reason: '发送消息' }))) return
 *   await chat.send(inputText)
 * }
 * </pre>
 */
export function useAuthGuard() {
  const auth = useAuthStore()
  const dialog = useAuthDialog()

  /**
   * 确保当前已登录。
   *
   * <p>已登录时**立即返回 true，不弹框**（零开销，所以可以放心地在每个动作前调用）。
   * 未登录时弹出认证框并等待：登录成功返回 true，用户取消返回 false。
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

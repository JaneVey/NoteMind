/**
 * Access Token 的内存持有者。
 *
 * <h3>为什么单独一个模块（而不是放在 store 或 request 里）</h3>
 *
 * 三处都需要读写 access token，如果任一处作为"所有者"都会形成循环引用：
 * <pre>
 *   request.ts  →  stores/authStore.ts  →  api/auth.ts  →  request.ts   ✗ 循环
 * </pre>
 *
 * 抽成这个**无依赖的叶子模块**后，依赖方向是单向的：
 * <pre>
 *   utils/authToken.ts  ←  api/request.ts
 *                       ←  stores/authStore.ts
 *                       ←  utils/sse.ts
 * </pre>
 *
 * <h3>为什么不用 localStorage</h3>
 *
 * 这是双 Token 方案的核心安全设计：**access token 只存在内存里**，
 * 页面刷新即丢失（由启动时的静默刷新换回）。
 * 这样即使发生 XSS，攻击者也拿不到可持久使用的凭据 ——
 * 长期凭据（Refresh Token）在 HttpOnly Cookie 里，JavaScript 根本读不到。
 *
 * <p><b>本模块刻意不依赖 Vue</b>：api 层不应感知框架。
 * 需要响应式状态的地方（组件、store）自己用 ref 包一层。
 */

let accessToken: string | null = null

let sessionExpiredHandler: (() => void) | null = null

/** 读取当前 access token（未登录返回 null） */
export function getAccessToken(): string | null {
  return accessToken
}

/** 设置 access token。传 null 表示清除 */
export function setAccessToken(token: string | null): void {
  accessToken = token
}

/**
 * 注册"会话失效"回调。
 *
 * <p>同样是为了避免循环引用 —— api 层不能直接 import store。
 * 由 `main.ts` 在应用启动时把 store 的处理函数注入进来（依赖注入）。
 *
 * <p>触发时机：refresh token 也失效了，说明会话真的结束了，
 * 需要清空前端状态并引导用户重新登录。
 */
export function onSessionExpired(handler: () => void): void {
  sessionExpiredHandler = handler
}

/** 通知"会话已失效"。由 api 层在刷新失败时调用 */
export function notifySessionExpired(): void {
  sessionExpiredHandler?.()
}

/** 清空 token（登出、或会话失效时使用） */
export function clearAccessToken(): void {
  accessToken = null
}

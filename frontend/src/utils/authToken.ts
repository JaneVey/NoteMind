/**
 * Access Token 的内存持有者。
 *
 * <p>必须是独立的叶子模块：有三个地方要读写 token，任一处当所有者都会成环
 * （request → store → api/auth → request）。本模块不依赖任何内部模块，也不依赖 Vue。
 *
 * <p>只放内存、不落盘，刷新页面即丢失，靠启动时的静默刷新换回。
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
 * 注册"会话失效"回调。用注入而不是让 api 层直接 import store，同样是为了避免循环引用。
 *
 * <p>触发时机：refresh token 也失效了，会话真的结束，需要清空前端状态并引导重新登录。
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

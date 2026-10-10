import axios from 'axios'
import type { AuthResponse } from '@/types/user'
import { setAccessToken } from '@/utils/authToken'

/**
 * Access Token 的静默刷新（single-flight）。
 *
 * <p>必须用不经拦截器的裸 axios 实例：否则刷新请求自己收到 401 时又会触发刷新，无限递归。
 * 也不能放进 api/auth.ts —— 那个文件既要 import request.ts，又要被 request.ts import，会成环。
 *
 * <p>并发去重是必需的：后端每次刷新都会轮换 refresh token，页面上 5 个请求同时 401
 * 若各自刷新，第 2~5 次用的是已作废的 token，全部失败并把用户登出。
 * 所以用模块级共享的同一个 Promise，无论多少并发都只刷新一次。
 *
 * <p>返回完整的 AuthResponse 而不是只返回 token：响应体本来就带用户信息，不必再多拉一次 profile。
 */

/** 裸实例：不走 request.ts 的拦截器 */
const raw = axios.create({
  baseURL: '/api',
  timeout: 30000,
  // 携带 Cookie —— refresh token 在 HttpOnly Cookie 里
  withCredentials: true,
})

/** 进行中的刷新。null 表示当前没有刷新在跑 */
let refreshing: Promise<AuthResponse | null> | null = null

/**
 * 用 Cookie 中的 refresh token 换一对新凭据。
 *
 * <p>本函数不判断"会话是否失效"，那是调用方的语义：
 * request.ts 在 401 后调用时失败说明会话真的结束；{@code authStore.bootstrap()} 在启动时
 * 调用时失败只说明"当前是游客"，不该提示。放在调用方可以避免启动时弹出莫名其妙的失效提示。
 *
 * @returns 成功返回新的凭据与用户信息；失败返回 null
 */
export function refreshAccessToken(): Promise<AuthResponse | null> {
  refreshing ??= doRefresh().finally(() => {
    // 必须清空，否则后续刷新会一直复用这个已完成的 Promise，全部拿到旧结果
    refreshing = null
  })
  return refreshing
}

async function doRefresh(): Promise<AuthResponse | null> {
  try {
    const response = await raw.post<{ data: AuthResponse }>('/auth/refresh')
    const auth = response.data?.data ?? null
    setAccessToken(auth?.accessToken ?? null)
    return auth
  } catch {
    // refresh token 过期 / 被吊销 / 因检测到重用而全量登出
    setAccessToken(null)
    return null
  }
}

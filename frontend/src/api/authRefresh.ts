import axios from 'axios'
import type { AuthResponse } from '@/types/user'
import { setAccessToken } from '@/utils/authToken'

/**
 * Access Token 的静默刷新（single-flight）。
 *
 * <h3>为什么单独一个文件，而不是写在 api/request.ts 或 api/auth.ts 里</h3>
 *
 * 刷新必须在**未经过拦截器**的裸 axios 实例上发起，否则刷新请求自己收到 401 时
 * 又会触发刷新，形成无限递归。因此本模块用独立实例，不 import request.ts。
 *
 * 而如果放在 api/auth.ts 里，它既要 import request.ts（给登录用）、
 * 又被 request.ts import（给刷新用），就成环了。所以单独一个文件。
 *
 * <h3>single-flight 是什么，为什么必须有</h3>
 *
 * 页面上 5 个请求同时返回 401 时，若各自去刷新，会发出 5 次刷新请求。
 * 而后端每次刷新都会**轮换** refresh token（旧的立即作废），
 * 于是第 2~5 次用的是已作废的 token → 全部失败 → 用户被莫名登出。
 *
 * 所以用**模块级共享的同一个 Promise**：第一个调用发起请求，
 * 其余调用复用它，无论多少并发都只刷新一次。
 *
 * <h3>为什么返回完整的 AuthResponse 而不是只返回 token</h3>
 *
 * 刷新的响应体里本来就带用户信息（昵称、头像可能已变）。
 * 只取 token 再额外拉一次 profile 是白白多一个请求。
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
 * <p><b>本函数不判断"会话是否失效"</b> —— 那是调用方的语义：
 * <ul>
 *   <li>{@code request.ts} 在 401 后调用：刷新失败说明会话真的结束，应通知状态失效</li>
 *   <li>{@code authStore.bootstrap()} 在启动时调用：刷新失败只说明"当前是游客"，不该提示</li>
 * </ul>
 * 把判断放在调用方，避免了"启动时弹一个莫名其妙的会话失效提示"。
 *
 * @returns 成功返回新的凭据与用户信息；失败返回 null
 */
export function refreshAccessToken(): Promise<AuthResponse | null> {
  refreshing ??= doRefresh().finally(() => {
    // 必须清空，否则后续刷新会一直复用这个已完成的 Promise，
    // 导致第一次之后的刷新全部拿到旧结果
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

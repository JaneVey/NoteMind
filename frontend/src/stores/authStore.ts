import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, register as registerApi } from '@/api/auth'
import { refreshAccessToken } from '@/api/authRefresh'
import { clearAccessToken, setAccessToken } from '@/utils/authToken'
import type { AuthResponse, LoginRequest, RegisterRequest, UserInfo } from '@/types/user'

/**
 * 认证状态。
 *
 * <p>前端不持久化任何认证数据：access token 在内存（utils/authToken），refresh token 在
 * HttpOnly Cookie 里由浏览器自动携带。启动时无条件调一次 {@code /auth/refresh}，
 * 成功即已登录，失败即游客 —— 比在 localStorage 存个标记再判断简单，也少一种状态不一致。
 */
export const useAuthStore = defineStore('auth', () => {
  /** 当前用户。null 表示游客 */
  const user = ref<UserInfo | null>(null)

  /** 是否已登录。组件用它做响应式判断（token 本身在 utils/authToken 中，不是响应式的） */
  const isLoggedIn = ref(false)

  /** access token 到期时间戳（毫秒）。仅用于预判与调试，不作为登录判断依据 */
  const expiresAt = ref(0)

  /**
   * 会话已失效标记。
   *
   * <p>与"未登录"不同：它是"本来登录着，但会话结束了"（refresh token 过期或被吊销）。
   * 界面据此提示重新登录，而不是当成普通游客。
   */
  const sessionExpired = ref(false)

  let bootstrapped = false

  /** 展示名：昵称优先，回落到用户名 */
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  /** 邮箱是否已验证 */
  const isEmailVerified = computed(() => user.value?.emailVerified === true)

  /** 从认证响应写入状态 */
  function applyAuth(auth: AuthResponse): void {
    setAccessToken(auth.accessToken)
    user.value = auth.user
    expiresAt.value = Date.now() + auth.expiresIn * 1000
    isLoggedIn.value = true
    sessionExpired.value = false
  }

  /** 只清本地状态（不通知服务端） */
  function clearLocal(): void {
    clearAccessToken()
    user.value = null
    expiresAt.value = 0
    isLoggedIn.value = false
  }

  /**
   * 标记会话失效。由 main.ts 通过 onSessionExpired 注入给 api 层调用。
   *
   * <p>用回调注入而不是让 api 层直接 import store：那会形成
   * request → authRefresh → store → api/auth → request 的循环引用。
   */
  function markSessionExpired(): void {
    clearLocal()
    sessionExpired.value = true
  }

  function dismissSessionExpired(): void {
    sessionExpired.value = false
  }

  // ------------------------------------------------------------------
  // 对外动作
  // ------------------------------------------------------------------

  async function login(payload: LoginRequest): Promise<void> {
    applyAuth(await loginApi(payload))
  }

  /** 注册。成功后不自动登录 —— 由界面引导登录，保持流程可预期 */
  async function register(payload: RegisterRequest): Promise<void> {
    await registerApi(payload)
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 服务端调用失败也必须清本地状态，否则会出现"点了登出但还登录着"
    }
    clearLocal()
  }

  /**
   * 应用启动时恢复登录态。
   *
   * <p>失败时不设置 sessionExpired —— 首次访问的用户本来就没登录过，
   * 不该看到"会话已失效"的提示。
   */
  async function bootstrap(): Promise<void> {
    if (bootstrapped) return
    bootstrapped = true

    const auth = await refreshAccessToken()
    if (auth) {
      applyAuth(auth)
    } else {
      clearLocal()
    }
  }

  /** 本地更新用户信息（例如改完昵称后，避免再拉一次 profile） */
  function patchUser(patch: Partial<UserInfo>): void {
    if (!user.value) return
    user.value = { ...user.value, ...patch }
  }

  return {
    user,
    isLoggedIn,
    expiresAt,
    sessionExpired,
    displayName,
    isEmailVerified,
    login,
    register,
    logout,
    bootstrap,
    markSessionExpired,
    dismissSessionExpired,
    patchUser,
  }
})

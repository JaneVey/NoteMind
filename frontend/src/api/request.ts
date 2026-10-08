import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 业务错误：携带后端返回的 code，便于调用方按码分支处理 */
export class ApiError extends Error {
  readonly code: number
  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

type RequestConfig = AxiosRequestConfig

interface TypedRequest {
  get<T = unknown>(url: string, config?: RequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: RequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: RequestConfig): Promise<T>
  <T = unknown>(config: RequestConfig): Promise<T>
}

export const TOKEN_KEY = 'notemind_token'
export const USER_KEY = 'notemind_user'

/**
 * 这些接口返回的 401 表示"凭据错误"，不是"会话过期"，因此不能触发自动登出。
 * 否则用户在登录页输错密码会被提示"登录已过期"，并清掉刚填的状态。
 */
const AUTH_ENDPOINTS = ['/auth/login', '/auth/register']

function isAuthEndpoint(url?: string): boolean {
  if (!url) return false
  return AUTH_ENDPOINTS.some((path) => url.includes(path))
}

const client = axios.create({ baseURL: '/api', timeout: 30000 })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 清除本地登录状态并跳转登录页。
 * 用 window.location 而不是 router，避免 api 层反向依赖 router 造成循环引用。
 * 导出供流式请求（utils/sse.ts）复用，保证两条链路的 401 行为一致。
 */
export function redirectToLogin(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

client.interceptors.response.use(
  <T>(response: AxiosResponse<ApiResponse<T>>): T => {
    const result = response.data
    if (result.code !== 200) {
      // 说明：后端现在用「HTTP 状态码 + 业务码(1xxx/2xxx/3xxx…)」双轨表达失败原因。
      // HTTP 状态码负责协议语义（401/403/404/400/500），业务码负责精确定位原因。
      // 因此这里不再对业务码做特殊分支 —— 控制流一律交给 HTTP 状态码（见下方 error 分支），
      // 业务码仅用于展示与按需分支，避免前端把它们写成魔法数字。
      throw new ApiError(result.code, result.message)
    }
    return result.data
  },
  (error: unknown) => {
    // HTTP 层失败：401 未登录 / 403 无权限 / 5xx 服务端异常
    if (axios.isAxiosError(error)) {
      const status = error.response?.status
      const body = error.response?.data as ApiResponse<unknown> | undefined
      const authRequest = isAuthEndpoint(error.config?.url)

      if (status === 401) {
        if (authRequest) {
          // 登录 / 注册的凭据错误：交给调用方展示，不做登出
          return Promise.reject(new ApiError(401, body?.message || '用户名或密码错误'))
        }
        redirectToLogin()
        return Promise.reject(new ApiError(401, '登录已过期，请重新登录'))
      }
      if (status === 403) {
        return Promise.reject(new ApiError(403, body?.message || '没有访问权限'))
      }
      if (status && status >= 500) {
        return Promise.reject(new ApiError(status, body?.message || '服务器异常，请稍后重试'))
      }
      if (body?.message) {
        return Promise.reject(new ApiError(body.code ?? status ?? 0, body.message))
      }
      if (error.code === 'ECONNABORTED') {
        return Promise.reject(new ApiError(0, '请求超时'))
      }
      return Promise.reject(new ApiError(0, error.message || '网络请求失败'))
    }
    return Promise.reject(error)
  },
)

export default client as unknown as TypedRequest

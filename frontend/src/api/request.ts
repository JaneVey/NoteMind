import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { getAccessToken, notifySessionExpired } from '@/utils/authToken'
import { refreshAccessToken } from './authRefresh'

/** 后端统一响应壳 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/**
 * 业务错误，同时携带业务码与 HTTP 状态码。
 * 前端控制流一律依赖 HTTP 状态码，业务码只用于展示与按需分支。
 */
export class ApiError extends Error {
  readonly code: number
  readonly status: number

  constructor(code: number, message: string, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
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

/** 带到重试逻辑上的自定义标记 */
type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/**
 * 这些接口的 401 表示凭据错误或会话确实结束，不能触发自动刷新：
 * login/register 是密码错了，触发刷新会让用户在登录页看到"登录已过期"；
 * refresh 是刷新本身失败，再刷新即无限递归；logout 时 token 已失效属正常。
 */
const NO_REFRESH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

function shouldSkipRefresh(url?: string): boolean {
  if (!url) return false
  return NO_REFRESH_ENDPOINTS.some((path) => url.includes(path))
}

const client = axios.create({
  baseURL: '/api',
  timeout: 30000,
  // 携带 Cookie：refresh token 在 HttpOnly Cookie 里
  withCredentials: true,
})

client.interceptors.request.use((config) => {
  // 从内存读，不读 localStorage —— access token 从不落盘
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  <T>(response: AxiosResponse<ApiResponse<T>>): T => {
    const result = response.data
    if (result.code !== 200) {
      throw new ApiError(result.code, result.message, response.status)
    }
    return result.data
  },
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      return Promise.reject(error)
    }

    const status = error.response?.status
    const body = error.response?.data as ApiResponse<unknown> | undefined
    const message = body?.message || ''
    const config = error.config as RetriableConfig | undefined

    // ---------- 401：尝试刷新一次并重放原请求 ----------
    if (status === 401) {
      if (!config || shouldSkipRefresh(config.url) || config._retried) {
        return Promise.reject(new ApiError(401, message || '用户名或密码错误', 401))
      }

      const auth = await refreshAccessToken()
      if (!auth) {
        // refresh token 也失效了 —— 会话真的结束。
        // 只有这里才该通知；启动时的静默刷新失败只代表"当前是游客"。
        notifySessionExpired()
        return Promise.reject(new ApiError(401, '登录已过期，请重新登录', 401))
      }

      // 重放原请求。_retried 保证只重试一次 ——
      // 否则新 token 仍被拒（例如账号刚被禁用）会无限循环
      config._retried = true
      return client.request(config)
    }

    // ---------- 其它状态码：转成 ApiError，保留业务码 ----------
    if (status === 403) {
      return Promise.reject(new ApiError(body?.code ?? 1003, message || '没有访问权限', 403))
    }
    if (status && status >= 500) {
      return Promise.reject(new ApiError(body?.code ?? 1005, message || '服务器异常，请稍后重试', status))
    }
    if (body?.message) {
      return Promise.reject(new ApiError(body.code ?? status ?? 0, body.message, status ?? 0))
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError(0, '请求超时'))
    }
    return Promise.reject(new ApiError(0, error.message || '网络请求失败'))
  },
)

export default client as unknown as TypedRequest

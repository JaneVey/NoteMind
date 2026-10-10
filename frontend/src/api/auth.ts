import request from './request'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/user'

/**
 * 认证接口。
 *
 * <p>刷新（{@code /auth/refresh}）不在这里 —— 它必须绕过拦截器，放在 authRefresh.ts。
 * 返回值里也不会出现 refresh token，它在 HttpOnly Cookie 中。
 */

export function login(data: LoginRequest): Promise<AuthResponse> {
  return request({ url: '/auth/login', method: 'POST', data })
}

export function register(data: RegisterRequest): Promise<void> {
  return request({ url: '/auth/register', method: 'POST', data })
}

/**
 * 登出。服务端会真正吊销 refresh token，而不只是让前端"忘记"它。
 * 即使请求失败，调用方也应继续清理本地状态。
 */
export function logout(): Promise<void> {
  return request({ url: '/auth/logout', method: 'POST' })
}

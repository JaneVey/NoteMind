import request from './request'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/user'

/**
 * 认证接口。
 *
 * <p><b>注意这里只有三个方法</b>：
 * <ul>
 *   <li>刷新（{@code /auth/refresh}）不在这里 —— 它在 {@code api/authRefresh.ts}，
 *       因为那个调用必须绕过拦截器，且要和 request.ts 保持单向依赖</li>
 *   <li>refresh token 不出现在任何返回值里 —— 它在 HttpOnly Cookie 中，JavaScript 读不到</li>
 * </ul>
 */

export function login(data: LoginRequest): Promise<AuthResponse> {
  return request({ url: '/auth/login', method: 'POST', data })
}

export function register(data: RegisterRequest): Promise<void> {
  return request({ url: '/auth/register', method: 'POST', data })
}

/**
 * 登出。
 *
 * <p>与旧实现的关键区别：服务端会**真正吊销** refresh token，
 * 而不只是让前端"忘记"它。即使请求失败（如网络问题），
 * 调用方也应继续清理本地状态。
 */
export function logout(): Promise<void> {
  return request({ url: '/auth/logout', method: 'POST' })
}

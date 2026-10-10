import type { Id } from './common'

/**
 * 用户与认证相关类型，对应后端 `com.notemind.user`（VO）与 `com.notemind.auth`（DTO）。
 *
 * <p>响应体里没有 refreshToken —— 它在 HttpOnly Cookie 里，JavaScript 读不到。
 */

/**
 * 当前登录用户（对应后端 `UserProfileVO`）。
 *
 * <p>字段名 `userId` 而非 `id`：语义上是"我的 id"。
 */
export interface UserInfo {
  userId: Id
  username: string
  email: string
  /** 邮箱是否已验证。未验证的账号可正常使用，界面引导验证 */
  emailVerified: boolean
  nickname: string | null
  avatar: string | null
}

export interface LoginRequest {
  /** 用户名或邮箱 */
  identifier: string
  password: string
  /** 记住我：refresh token 有效期由 7 天延长到 30 天 */
  rememberMe?: boolean
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname: string
}

/** 认证成功响应（登录 / 刷新共用） */
export interface AuthResponse {
  /** Access Token。只放内存，绝不进 localStorage */
  accessToken: string
  /** 有效秒数，用于预判刷新时机 */
  expiresIn: number
  user: UserInfo
}

/** 修改个人资料 */
export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
}

/** 修改密码 */
export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
}

import type { Id } from './common'

/**
 * 用户与认证相关类型。
 *
 * <p>与后端 `com.notemind.user`（VO）和 `com.notemind.auth`（DTO）对应。
 *
 * <p><b>变更记录（2026-10-08，认证重设计）</b>：登录响应由
 * `{ token, userId, username, ... }` 平铺结构改为 `AuthResponse{ accessToken, expiresIn, user }`。
 *
 * <p><b>为什么响应体里没有 refreshToken</b>：它在 HttpOnly Cookie 里，
 * JavaScript 根本读不到 —— 这正是双 Token 方案防 XSS 的关键。
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
  /** Access Token。前端**只放内存**，绝不进 localStorage */
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

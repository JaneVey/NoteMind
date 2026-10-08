import type { Id } from './common'

/**
 * 用户与认证相关类型。
 *
 * <p>与后端 `com.notemind.user` / `com.notemind.auth` 模块对应。
 */

/**
 * 当前登录用户信息（前端视角，对应后端 `UserProfileVO`）。
 *
 * <p><b>注意</b>：后端不叫 `UserVO` 而叫 `UserProfileVO`，且主键字段名是 `userId`
 * 而非 `id` —— 因为它描述的是"当前登录者"，与 `SysUser` 实体刻意区分开
 * （实体含 `password`，绝不外传）。
 */
export interface UserInfo {
  userId: Id | null
  username: string
  nickname: string
  avatar: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname: string
}

/**
 * 登录响应。
 *
 * <p><b>变更记录</b>：此前这里有 `user?: Partial<UserInfo>` 作为兼容分支，
 * 导致 `authStore` 里写满了 `data.userId ?? data.user?.userId ?? null` 这种三重兜底。
 * 后端实际返回的就是扁平结构，已移除该字段。
 */
export interface LoginResponse {
  token: string
  userId: Id
  username: string
  nickname: string
  avatar: string | null
}

/** 修改个人资料（后端字段均可选，只更新传了的） */
export interface UpdateProfileRequest {
  nickname?: string
  avatar?: string
}

/** 修改密码 */
export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
}

import request from './request'
import type { UserInfo } from '@/types/domain'

export function getUserProfile(): Promise<UserInfo> { return request({ url: '/user/profile', method: 'GET' }) }
export function updateUserProfile(data: Partial<UserInfo>): Promise<UserInfo> { return request({ url: '/user/profile', method: 'PUT', data }) }
export function updatePassword(data: { currentPassword: string; newPassword: string }): Promise<void> { return request({ url: '/user/password', method: 'PUT', data }) }

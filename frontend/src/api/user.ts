import request from './request'
import type { UpdatePasswordRequest, UpdateProfileRequest, UserInfo } from '@/types/user'

export function getUserProfile(): Promise<UserInfo> { return request({ url: '/user/profile', method: 'GET' }) }
export function updateUserProfile(data: UpdateProfileRequest): Promise<UserInfo> { return request({ url: '/user/profile', method: 'PUT', data }) }
export function updatePassword(data: UpdatePasswordRequest): Promise<void> { return request({ url: '/user/password', method: 'PUT', data }) }

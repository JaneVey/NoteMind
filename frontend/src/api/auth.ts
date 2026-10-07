import request from './request'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/domain'

export function login(data: LoginRequest): Promise<LoginResponse> { return request({ url: '/auth/login', method: 'POST', data }) }
export function register(data: RegisterRequest): Promise<void> { return request({ url: '/auth/register', method: 'POST', data }) }

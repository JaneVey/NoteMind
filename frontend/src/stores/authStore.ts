import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '@/api/auth'
import type { Id } from '@/types/common'
import type { UserInfo } from '@/types/user'

const emptyUser = (): UserInfo => ({ userId: null, username: '', nickname: '', avatar: '' })
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const userInfo = ref<UserInfo>(emptyUser())
  const isAuthenticated = computed(() => token.value !== null)
  async function login(username: string, password: string): Promise<void> { const data = await loginApi({ username, password }); token.value = data.token; userInfo.value = { userId: data.userId, username: data.username, nickname: data.nickname, avatar: data.avatar ?? '' }; localStorage.setItem('notemind_token', data.token); localStorage.setItem('notemind_user', JSON.stringify(userInfo.value)) }
  async function register(username: string, password: string, email: string, nickname: string): Promise<void> { await registerApi({ username, password, email, nickname }) }
  function logout(): void { token.value = null; userInfo.value = emptyUser(); localStorage.removeItem('notemind_token'); localStorage.removeItem('notemind_user') }
  function getToken(): string | null { return localStorage.getItem('notemind_token') }
  function initFromStorage(): void { const savedToken = localStorage.getItem('notemind_token'); const savedUser = localStorage.getItem('notemind_user'); if (savedToken) token.value = savedToken; if (savedUser) { try { const parsed: unknown = JSON.parse(savedUser); if (typeof parsed === 'object' && parsed !== null) userInfo.value = { ...emptyUser(), ...(parsed as Partial<UserInfo>) } } catch { userInfo.value = emptyUser() } } }
  return { token, userInfo, isAuthenticated, login, register, logout, getToken, initFromStorage }
})
export type { Id }

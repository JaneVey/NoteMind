import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // state
  const token = ref(null)
  const userInfo = ref({
    userId: null,
    username: '',
    nickname: '',
    avatar: '',
  })

  // getters
  const isAuthenticated = computed(() => !!token.value)

  // actions
  async function login(username, password) {
    const data = await loginApi({ username, password })
    token.value = data.token
    userInfo.value = {
      userId: data.userId || data.user?.userId,
      username: data.username || data.user?.username,
      nickname: data.nickname || data.user?.nickname || '',
      avatar: data.avatar || data.user?.avatar || '',
    }
    localStorage.setItem('notemind_token', token.value)
    localStorage.setItem('notemind_user', JSON.stringify(userInfo.value))
  }

  async function register(username, password, email, nickname) {
    const data = await registerApi({ username, password, email, nickname })
    return data
  }

  function logout() {
    token.value = null
    userInfo.value = {
      userId: null,
      username: '',
      nickname: '',
      avatar: '',
    }
    localStorage.removeItem('notemind_token')
    localStorage.removeItem('notemind_user')
  }

  function getToken() {
    return localStorage.getItem('notemind_token')
  }

  function initFromStorage() {
    const savedToken = localStorage.getItem('notemind_token')
    const savedUser = localStorage.getItem('notemind_user')
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUser) {
      try {
        userInfo.value = JSON.parse(savedUser)
      } catch {
        // ignore parse error
      }
    }
  }

  return {
    token,
    userInfo,
    isAuthenticated,
    login,
    register,
    logout,
    getToken,
    initFromStorage,
  }
})

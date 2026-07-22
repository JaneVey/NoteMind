import request from './request'

/* ======== AI 供应商配置 ======== */

export function getAiConfigs() {
  return request.get('/settings/ai-configs')
}

export function saveAiConfig(data) {
  return request.post('/settings/ai-config', data)
}

export function updateAiConfig(id, data) {
  return request.put(`/settings/ai-config/${id}`, data)
}

export function deleteAiConfig(id) {
  return request.delete(`/settings/ai-config/${id}`)
}

export function setActiveProvider(id) {
  return request.put(`/settings/ai-config/${id}/active`)
}

/* ======== Prompt 快捷指令 ======== */

export function getPromptShortcuts() {
  return request.get('/settings/prompt-shortcuts')
}

export function createPromptShortcut(data) {
  return request.post('/settings/prompt-shortcut', data)
}

export function updatePromptShortcut(id, data) {
  return request.put(`/settings/prompt-shortcut/${id}`, data)
}

export function deletePromptShortcut(id) {
  return request.delete(`/settings/prompt-shortcut/${id}`)
}

/* ======== 用户画像 ======== */

export function getUserProfile() {
  return request.get('/settings/user-profile')
}

export function updateUserProfile(data) {
  return request.put('/settings/user-profile', data)
}

/* ======== 系统配置 ======== */

export function getSystemConfig() {
  return request.get('/settings/system-config')
}

export function updateSystemConfig(data) {
  return request.put('/settings/system-config', data)
}

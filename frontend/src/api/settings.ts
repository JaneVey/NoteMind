import request from './request'
import type { AiConfig, PromptShortcut } from '@/types/ai'
import type { Id } from '@/types/common'

export function getAiConfigs(): Promise<AiConfig[]> { return request.get('/settings/ai-configs') }
export function saveAiConfig(data: Omit<AiConfig, 'id'>): Promise<AiConfig> { return request.post('/settings/ai-config', data) }
export function updateAiConfig(id: Id, data: Partial<AiConfig>): Promise<AiConfig> { return request.put(`/settings/ai-config/${id}`, data) }
export function deleteAiConfig(id: Id): Promise<void> { return request.delete(`/settings/ai-config/${id}`) }
export function setActiveProvider(id: Id): Promise<void> { return request.put(`/settings/ai-config/${id}/active`) }
export function getPromptShortcuts(): Promise<PromptShortcut[]> { return request.get('/settings/prompt-shortcuts') }
export function createPromptShortcut(data: Omit<PromptShortcut, 'id'>): Promise<PromptShortcut> { return request.post('/settings/prompt-shortcut', data) }
export function updatePromptShortcut(id: Id, data: Partial<PromptShortcut>): Promise<PromptShortcut> { return request.put(`/settings/prompt-shortcut/${id}`, data) }
export function deletePromptShortcut(id: Id): Promise<void> { return request.delete(`/settings/prompt-shortcut/${id}`) }
export function getUserProfile(): Promise<Record<string, unknown>> { return request.get('/settings/user-profile') }
export function updateUserProfile(data: Record<string, unknown>): Promise<void> { return request.put('/settings/user-profile', data) }
export function getSystemConfig(): Promise<Record<string, unknown>> { return request.get('/settings/system-config') }
export function updateSystemConfig(data: Record<string, unknown>): Promise<void> { return request.put('/settings/system-config', data) }

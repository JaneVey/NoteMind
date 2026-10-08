import request from './request'
import type { ChatMessage, Conversation } from '@/types/ai'
import type { Id } from '@/types/common'

export interface CreateConversationRequest { title?: string }
export function getConversations(): Promise<Conversation[]> { return request.get('/ai/conversations') }
export function createConversation(data: CreateConversationRequest): Promise<Conversation> { return request.post('/ai/conversation', data) }
export function deleteConversation(id: Id): Promise<void> { return request.delete(`/ai/conversation/${id}`) }
export function renameConversation(id: Id, title: string): Promise<void> { return request.put(`/ai/conversation/${id}`, { title }) }
export function favoriteConversation(id: Id, isFavorite: boolean): Promise<void> { return request.put(`/ai/conversation/${id}/favorite`, { isFavorite }) }
export function getMessages(conversationId: Id): Promise<ChatMessage[]> { return request.get(`/ai/messages/${conversationId}`) }
export const URL_SEND_CHAT = '/ai/chat/stream'

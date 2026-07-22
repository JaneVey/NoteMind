import request from './request'

/**
 * 获取所有对话记录
 */
export function getConversations() {
  return request.get('/ai/conversations')
}

/**
 * 创建新对话
 */
export function createConversation(data) {
  return request.post('/ai/conversation', data)
}

/**
 * 删除对话
 */
export function deleteConversation(id) {
  return request.delete(`/ai/conversation/${id}`)
}

/**
 * 重命名对话
 */
export function renameConversation(id, title) {
  return request.put(`/ai/conversation/${id}`, { title })
}

/**
 * 收藏/取消收藏对话
 */
export function favoriteConversation(id, isFavorite) {
  return request.put(`/ai/conversation/${id}/favorite`, { isFavorite })
}

/**
 * 获取对话的消息列表
 */
export function getMessages(conversationId) {
  return request.get(`/ai/messages/${conversationId}`)
}

/**
 * AI 对话流式接口地址
 */
export const URL_SEND_CHAT = '/ai/chat/stream'

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createConversation as createConversationApi, deleteConversation as deleteConversationApi, favoriteConversation as favoriteConversationApi, getConversations as fetchConversationsApi, getMessages as getMessagesApi, renameConversation as renameConversationApi } from '@/api/ai'
import type { ChatMessage, Conversation } from '@/types/ai'
import type { Id } from '@/types/common'

export const useAiStore = defineStore('ai', () => {
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<Id | null>(null)
  const messages = ref<ChatMessage[]>([])
  const isStreaming = ref(false)
  async function fetchConversations(): Promise<void> { try { conversations.value = await fetchConversationsApi() } catch { console.error('获取对话列表失败') } }
  async function createConversation(data: { title?: string }): Promise<Conversation | null> { try { const conversation = await createConversationApi(data); conversations.value.unshift(conversation); currentConversationId.value = conversation.id; messages.value = []; return conversation } catch { console.error('创建对话失败'); return null } }
  async function deleteConversation(id: Id): Promise<void> { try { await deleteConversationApi(id); conversations.value = conversations.value.filter((item) => item.id !== id); if (currentConversationId.value === id) { currentConversationId.value = null; messages.value = [] } } catch { console.error('删除对话失败') } }
  async function renameConversation(id: Id, title: string): Promise<void> { try { await renameConversationApi(id, title); const conversation = conversations.value.find((item) => item.id === id); if (conversation) conversation.title = title } catch { console.error('重命名对话失败') } }
  async function favoriteConversation(id: Id, isFavorite: boolean): Promise<void> { try { await favoriteConversationApi(id, isFavorite); const conversation = conversations.value.find((item) => item.id === id); if (conversation) conversation.isFavorite = isFavorite } catch { console.error('操作失败') } }
  async function fetchMessages(conversationId: Id): Promise<void> { try { messages.value = await getMessagesApi(conversationId) } catch { console.error('获取消息失败') } }
  function selectConversation(conversationId: Id): void { currentConversationId.value = conversationId; void fetchMessages(conversationId) }
  function appendMessage(message: ChatMessage): void { messages.value.push(message) }
  function updateLastMessage(content: string): void { const last = messages.value.at(-1); if (last) last.content = content }
  function setStreaming(value: boolean): void { isStreaming.value = value }
  return { conversations, currentConversationId, messages, isStreaming, fetchConversations, createConversation, deleteConversation, renameConversation, favoriteConversation, fetchMessages, selectConversation, appendMessage, updateLastMessage, setStreaming }
})

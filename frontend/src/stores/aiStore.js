import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getConversations as fetchConversationsApi,
  createConversation as createConversationApi,
  deleteConversation as deleteConversationApi,
  renameConversation as renameConversationApi,
  favoriteConversation as favoriteConversationApi,
  getMessages as getMessagesApi,
} from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  /* ========== State ========== */
  const conversations = ref([])
  const currentConversationId = ref(null)
  const messages = ref([])
  const isStreaming = ref(false)

  /* ========== Actions ========== */

  /** 获取所有对话 */
  async function fetchConversations() {
    try {
      conversations.value = await fetchConversationsApi()
    } catch (e) {
      console.error('获取对话列表失败')
    }
  }

  /** 创建新对话 */
  async function createConversation(data) {
    try {
      const conv = await createConversationApi(data)
      conversations.value.unshift(conv)
      currentConversationId.value = conv.id
      messages.value = []
      return conv
    } catch (e) {
      console.error('创建对话失败')
      return null
    }
  }

  /** 删除对话 */
  async function deleteConversation(id) {
    try {
      await deleteConversationApi(id)
      conversations.value = conversations.value.filter((c) => c.id !== id)
      if (currentConversationId.value === id) {
        currentConversationId.value = null
        messages.value = []
      }
    } catch (e) {
      console.error('删除对话失败')
    }
  }

  /** 重命名对话 */
  async function renameConversation(id, title) {
    try {
      await renameConversationApi(id, title)
      const conv = conversations.value.find((c) => c.id === id)
      if (conv) conv.title = title
    } catch (e) {
      console.error('重命名对话失败')
    }
  }

  /** 收藏/取消收藏 */
  async function favoriteConversation(id, isFavorite) {
    try {
      await favoriteConversationApi(id, isFavorite)
      const conv = conversations.value.find((c) => c.id === id)
      if (conv) conv.isFavorite = isFavorite
    } catch (e) {
      console.error('操作失败')
    }
  }

  /** 获取消息列表 */
  async function fetchMessages(conversationId) {
    try {
      messages.value = await getMessagesApi(conversationId)
    } catch (e) {
      console.error('获取消息失败')
    }
  }

  /** 选择对话 */
  function selectConversation(conversationId) {
    currentConversationId.value = conversationId
    fetchMessages(conversationId)
  }

  /** 追加一条消息（本地） */
  function appendMessage(msg) {
    messages.value.push(msg)
  }

  /** 更新最后一条消息的内容（用于流式输出） */
  function updateLastMessage(content) {
    if (messages.value.length > 0) {
      messages.value[messages.value.length - 1].content = content
    }
  }

  /** 设置流式状态 */
  function setStreaming(val) {
    isStreaming.value = val
  }

  return {
    // state
    conversations,
    currentConversationId,
    messages,
    isStreaming,
    // actions
    fetchConversations,
    createConversation,
    deleteConversation,
    renameConversation,
    favoriteConversation,
    fetchMessages,
    selectConversation,
    appendMessage,
    updateLastMessage,
    setStreaming,
  }
})

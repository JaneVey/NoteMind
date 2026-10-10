<template>
  <div class="flex h-full min-w-0 overflow-hidden bg-[#fdfdfc] text-zinc-800">
    <ConversationList
      :conversations="conversations"
      :pinned-conversations="pinnedConversations"
      :active-conversation-id="activeConversationId"
      @new-chat="startNewChat"
      @select="selectConversation"
      @activate="activateConversation"
      @pin="pinConversation"
      @unpin="unpinConversation"
      @rename="renameConversation"
      @remove="deleteConversation"
      @reorder="reorderConversations"
    />

    <main class="relative flex min-w-0 flex-1 flex-col bg-[#fdfdfc]">
      <MessageList ref="messageListRef" :messages="messages">
        <template #composer>
          <ChatComposer
            v-model="inputText"
            class="mt-8 w-full max-w-[920px]"
            placeholder="向 NoteMind AI 提问，或绑定知识库后检索资料"
            @send="sendMessage"
          />
        </template>
      </MessageList>

      <div v-if="messages.length" class="w-full shrink-0 px-6 pb-8">
        <ChatComposer
          v-model="inputText"
          class="mx-auto w-full max-w-[920px]"
          placeholder="继续追问，或让 NoteMind AI 帮你整理成笔记"
          @send="sendMessage"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ChatMessage, Conversation } from '@/types/ai'
import type { Id } from '@/types/common'
import ChatComposer from './aichat/ChatComposer.vue'
import ConversationList from './aichat/ConversationList.vue'
import MessageList from './aichat/MessageList.vue'

type ConversationSummary = Pick<Conversation, 'id' | 'title'>
type PrototypeMessage = Required<Pick<ChatMessage, 'id' | 'role' | 'content'>>

const inputText = ref('')
const messageListRef = ref<InstanceType<typeof MessageList> | null>(null)
const activeConversationId = ref<Id>('history-1')
const messages = ref<PrototypeMessage[]>([])

const conversations = ref<ConversationSummary[]>([
  { id: 'history-1', title: '实习协议审核' },
  { id: 'history-2', title: '毕业设计流程咨询' },
  { id: 'history-3', title: 'Steam 域名与 VPN 设置' },
  { id: 'history-4', title: '需求分析后续步骤' },
  { id: 'history-5', title: '崩坏星穹铁道服务器切换' },
  { id: 'history-6', title: '产品定位与需求分析' },
  { id: 'history-7', title: '千问重排模型定位' },
  { id: 'history-8', title: 'Milvus Docker 部署建议' },
])

const pinnedConversations = ref<ConversationSummary[]>([
  { id: 'pinned-1', title: '非遗数字资源上传存储管理' },
  { id: 'pinned-2', title: '图片生成' },
  { id: 'pinned-3', title: '面试聊天回复' },
  { id: 'pinned-4', title: '面试结果分析' },
  { id: 'pinned-5', title: '简历写作与工具选择' },
  { id: 'pinned-6', title: '日报撰写建议' },
])

const selectedConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value)
  || pinnedConversations.value.find((item) => item.id === activeConversationId.value))

function startNewChat(): void {
  activeConversationId.value = 'new'
  messages.value = []
  inputText.value = ''
}

function selectConversation(item: ConversationSummary): void {
  activeConversationId.value = item.id
  inputText.value = item.title
  messages.value = []
}

/** 右键菜单只切高亮，不重置消息与输入框（与左键切换行为不一致） */
function activateConversation(id: Id): void {
  activeConversationId.value = id
}

function pinConversation(item: ConversationSummary): void {
  const index = conversations.value.findIndex((conversation) => conversation.id === item.id)
  if (index === -1) return
  const [conversation] = conversations.value.splice(index, 1)
  pinnedConversations.value.unshift({ ...conversation })
  activeConversationId.value = conversation.id
}

function unpinConversation(item: ConversationSummary): void {
  const index = pinnedConversations.value.findIndex((conversation) => conversation.id === item.id)
  if (index === -1) return
  const [conversation] = pinnedConversations.value.splice(index, 1)
  conversations.value.unshift({ id: conversation.id, title: conversation.title })
  activeConversationId.value = conversation.id
}

function renameConversation(payload: { id: Id; title: string }): void {
  const conversation = conversations.value.find((item) => item.id === payload.id)
    || pinnedConversations.value.find((item) => item.id === payload.id)
  if (conversation && payload.title) conversation.title = payload.title
}

function deleteConversation(id: Id): void {
  const target = conversations.value.find((item) => item.id === id)
    || pinnedConversations.value.find((item) => item.id === id)
  if (target) {
    conversations.value = conversations.value.filter((item) => item.id !== target.id)
    pinnedConversations.value = pinnedConversations.value.filter((item) => item.id !== target.id)
    if (activeConversationId.value === target.id) startNewChat()
  }
}

function reorderConversations(payload: { sourceId: Id; targetId: Id }): void {
  const sourceIndex = conversations.value.findIndex((item) => item.id === payload.sourceId)
  const targetIndex = conversations.value.findIndex((item) => item.id === payload.targetId)
  if (sourceIndex === -1 || targetIndex === -1) return
  const [moved] = conversations.value.splice(sourceIndex, 1)
  const insertIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  conversations.value.splice(insertIndex, 0, moved)
}

function sendMessage(): void {
  const text = inputText.value.trim()
  if (!text) return
  if (!selectedConversation.value && activeConversationId.value !== 'new') {
    activeConversationId.value = 'new'
  }

  messages.value.push({
    id: `user-${Date.now()}`,
    role: 'user',
    content: text,
  })
  inputText.value = ''

  setTimeout(() => {
    messages.value.push({
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '这是前端原型中的模拟回复。后续接入 Spring AI 和 SSE 后，这里会展示真实流式回答；对话也可以绑定知识库，返回带引用来源的 RAG 结果。',
    })
    scrollToBottom()
  }, 450)
  scrollToBottom()
}

function scrollToBottom(): void {
  messageListRef.value?.scrollToBottom()
}
</script>

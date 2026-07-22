<template>
  <div class="flex h-full min-w-0 overflow-hidden bg-[#fdfdfc] text-zinc-800">
    <aside class="flex w-[288px] shrink-0 flex-col border-r border-zinc-100 bg-white">
      <header class="flex h-14 items-center justify-between px-4">
        <button class="icon-button" title="收起侧栏">
          <PanelLeftClose class="h-[18px] w-[18px]" />
        </button>
      </header>

      <div class="space-y-1 px-3 pb-5">
        <button class="side-action side-action-active" @click="startNewChat">
          <MessageSquare class="h-[18px] w-[18px]" />
          <span>新建对话</span>
          <span class="ml-auto text-xs font-normal text-zinc-400">通用AI</span>
        </button>
      </div>

      <div class="px-5 pb-4 pt-4 text-[15px] font-medium text-zinc-500">会话历史</div>
      <div class="flex-1 overflow-y-auto px-2">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="history-item"
          :class="activeConversationId === item.id ? 'bg-[#edf7f1] text-zinc-900' : ''"
          @click="selectConversation(item)"
        >
          <MessageSquare class="h-4 w-4 shrink-0" />
          <span class="truncate">{{ item.title }}</span>
        </button>

        <div class="mx-9 mt-12 border-t border-zinc-100 pt-0 text-center">
          <span class="-mt-2 inline-block bg-white px-3 text-xs text-zinc-300">
            为你保留最近90天历史记录
          </span>
        </div>
      </div>
    </aside>

    <main class="relative flex min-w-0 flex-1 flex-col bg-[#fdfdfc]">
      <section v-if="messages.length" ref="messageListRef" class="flex-1 overflow-y-auto px-8 py-8">
        <div class="mx-auto flex max-w-3xl flex-col gap-5">
          <div
            v-for="message in messages"
            :key="message.id"
            class="flex"
            :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-7 shadow-sm"
              :class="message.role === 'user'
                ? 'bg-[#e8f4ee] text-zinc-800'
                : 'border border-zinc-100 bg-white text-zinc-600'"
            >
              {{ message.content }}
            </div>
          </div>
        </div>
      </section>

      <section v-else class="flex flex-1 flex-col items-center justify-center px-6 pb-8">
        <div class="flex flex-col items-center">
          <div class="mb-2 rounded-md border border-[#bcebd5] bg-[#effaf4] px-2.5 py-1 text-xs font-medium text-[#4f9c78]">
            笔记 · 知识库 · AI
          </div>
          <div class="font-black leading-none tracking-normal text-zinc-900 text-[58px]">NoteMind</div>
          <div class="mt-2 text-[15px] font-semibold tracking-[0.35em] text-zinc-600">AI ASSISTANT</div>
        </div>

        <ChatComposer
          v-model="inputText"
          class="mt-8 w-full max-w-[920px]"
          placeholder="向 NoteMind AI 提问，或绑定知识库后检索资料"
          @send="sendMessage"
        />

        <div class="mt-20 grid grid-cols-4 gap-14 text-xs text-zinc-500">
          <button
            v-for="action in quickActions"
            :key="action.label"
            class="quick-action"
            :title="action.label"
            @click="fillPrompt(action.prompt)"
          >
            <span class="quick-action-icon">
              <component :is="action.icon" class="h-6 w-6" />
            </span>
            <span>{{ action.label }}</span>
          </button>
        </div>
      </section>

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
import { computed, defineComponent, h, nextTick, ref } from 'vue'
import type { Component, PropType } from 'vue'
import type { ChatMessage, MessageRole } from '@/types/domain'
import {
  AtSign,
  Bot,
  ChevronDown,
  FilePenLine,
  Globe2,
  MessageSquare,
  PanelLeftClose,
  Paperclip,
  Send,
  Sparkles,
  WandSparkles,
} from 'lucide-vue-next'

interface ChatConversation { id: string; title: string }
interface QuickAction { label: string; prompt: string; icon: Component }
type PrototypeMessage = Required<Pick<ChatMessage, 'id' | 'role' | 'content'>>

const inputText = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const activeConversationId = ref('history-1')
const messages = ref<PrototypeMessage[]>([])

const conversations: ChatConversation[] = [
  { id: 'history-1', title: '帮我梳理 RAG 的完整链路' },
  { id: 'history-2', title: 'Java 后端实习项目表达优化' },
  { id: 'history-3', title: '解释 JWT 双 Token 鉴权流程' },
  { id: 'history-4', title: '把笔记整理成面试回答' },
]

const quickActions: QuickAction[] = [
  { label: '总结笔记', prompt: '请用清晰的条目总结这段笔记：', icon: Sparkles },
  { label: '文档解读', prompt: '请解读下面这份文档：', icon: FilePenLine },
  { label: '写作润色', prompt: '请优化下面这段内容的表达：', icon: WandSparkles },
  { label: '学习规划', prompt: '请根据我的当前阶段，制定一份学习计划：', icon: Bot },
]

const selectedConversation = computed(() => conversations.find((item) => item.id === activeConversationId.value))

function startNewChat() {
  activeConversationId.value = 'new'
  messages.value = []
  inputText.value = ''
}

function selectConversation(item: ChatConversation) {
  activeConversationId.value = item.id
  inputText.value = item.title
  messages.value = []
}

function fillPrompt(prompt: string) {
  inputText.value = prompt
}

function sendMessage() {
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

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const ChatComposer = defineComponent({
  name: 'ChatComposer',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'send'],
  setup(props, { emit, attrs }) {
    const updateValue = (event: Event) => emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
    const send = () => emit('send')
    const enterSend = (event: KeyboardEvent) => {
      if (!event.shiftKey) {
        event.preventDefault()
        send()
      }
    }

    return () => h('div', { ...attrs }, [
      h('div', { class: 'composer' }, [
        h('textarea', {
          value: props.modelValue,
          rows: 2,
          placeholder: props.placeholder,
          class: 'composer-input',
          onInput: updateValue,
          onKeydown: (event) => {
            if (event.key === 'Enter') enterSend(event)
          },
        }),
        h('div', { class: 'composer-toolbar' }, [
          h('div', { class: 'flex items-center gap-2' }, [
            h('button', { class: 'composer-chip', title: '选择模型' }, [
              h(Globe2, { class: 'h-4 w-4' }),
              'DS 快速',
              h(ChevronDown, { class: 'h-3 w-3' }),
            ]),
            h('button', { class: 'composer-round', title: '关联知识库' }, [
              h(AtSign, { class: 'h-4 w-4' }),
            ]),
          ]),
          h('div', { class: 'flex items-center gap-1' }, [
            h('button', { class: 'composer-icon', title: '上传附件' }, [
              h(Paperclip, { class: 'h-[18px] w-[18px]' }),
            ]),
            h('button', {
              class: props.modelValue.trim() ? 'send-button send-button-active' : 'send-button',
              title: '发送',
              disabled: !props.modelValue.trim(),
              onClick: send,
            }, [h(Send, { class: 'h-4 w-4' })]),
          ]),
        ]),
      ]),
    ])
  },
})
</script>

<style scoped>
.icon-button {
  display: inline-flex;
  height: 32px;
  width: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.icon-button:hover {
  background: #f4f4f5;
  color: #27272a;
}

.side-action {
  display: flex;
  height: 39px;
  width: 100%;
  align-items: center;
  gap: 9px;
  border-radius: 7px;
  padding: 0 13px;
  text-align: left;
  font-size: 15px;
  font-weight: 500;
  color: #3f3f46;
  transition: background-color 0.18s ease;
}

.side-action:hover,
.side-action-active {
  background: #e9f5ef;
}

.history-item {
  display: flex;
  height: 39px;
  width: 100%;
  align-items: center;
  gap: 9px;
  border-radius: 7px;
  padding: 0 18px;
  text-align: left;
  font-size: 15px;
  color: #3f3f46;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.history-item:hover {
  background: #f4f8f6;
  color: #18181b;
}

.quick-action {
  display: flex;
  width: 76px;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: color 0.18s ease;
}

.quick-action:hover {
  color: #2f8c6b;
}

.quick-action-icon {
  display: flex;
  height: 58px;
  width: 58px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #f7f7f6;
  color: #3f3f46;
}

:deep(.composer) {
  border: 1px solid #e3e3e3;
  border-radius: 23px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(30, 41, 36, 0.08);
  padding: 14px 16px 12px;
}

:deep(.composer-input) {
  min-height: 58px;
  width: 100%;
  resize: none;
  border: 0;
  background: transparent;
  padding: 0 2px;
  color: #3f3f46;
  font-size: 18px;
  line-height: 28px;
  outline: none;
}

:deep(.composer-input::placeholder) {
  color: #c7c7c7;
}

:deep(.composer-toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
}

:deep(.composer-chip),
:deep(.composer-round),
:deep(.composer-icon) {
  display: inline-flex;
  height: 34px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 999px;
  color: #3f3f46;
  transition: background-color 0.18s ease;
}

:deep(.composer-chip) {
  padding: 0 13px;
  border: 1px solid #ededed;
  background: #fafafa;
  font-size: 15px;
}

:deep(.composer-round),
:deep(.composer-icon) {
  width: 34px;
}

:deep(.composer-round) {
  border: 1px solid #ededed;
  background: #fafafa;
}

:deep(.composer-icon:hover),
:deep(.composer-round:hover),
:deep(.composer-chip:hover) {
  background: #f4f4f5;
}

:deep(.send-button) {
  display: inline-flex;
  height: 34px;
  width: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #d4d4d8;
  color: white;
  transition: background-color 0.18s ease;
}

:deep(.send-button-active) {
  background: #2f8c6b;
}
</style>

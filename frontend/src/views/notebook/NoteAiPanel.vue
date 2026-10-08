<template>
  <aside
    v-show="visible"
    class="relative flex w-[320px] shrink-0 flex-col border-l border-zinc-200 bg-[#fbfbfa]"
    :style="{ width: `${width}px` }"
  >
    <div class="side-panel-resizer right-resizer" title="拖拽调整宽度" @mousedown.prevent="emit('resize-start', $event)" />
    <header class="relative flex h-14 shrink-0 items-center gap-1 border-b border-zinc-200 px-3">
      <button v-for="tab in rightTabs" :key="tab.key" class="side-tab" :class="rightTab === tab.key ? 'side-tab-active' : ''" :title="tab.label" @click="rightTab = tab.key">
        <component :is="tab.icon" class="h-[18px] w-[18px]" />
      </button>
      <button class="icon-button absolute right-3" title="收起上下文面板" @click="emit('update:visible', false)"><PanelRightClose class="h-[18px] w-[18px]" /></button>
    </header>

    <section v-if="rightTab === 'backlinks'" class="min-h-0 flex-1 overflow-y-auto p-4">
      <p class="mb-3 text-xs font-medium tracking-wide text-zinc-400">链接到 {{ noteTitle }} 的笔记</p>
      <div v-if="backlinkNotes.length" class="space-y-1">
        <button v-for="note in backlinkNotes" :key="note.id" class="link-note-row" @click="emit('select-note', note.id)"><FileText class="h-4 w-4" /><span class="truncate">{{ note.title }}</span><ChevronRight class="ml-auto h-4 w-4 text-zinc-300" /></button>
      </div>
      <div v-else class="empty-state">还没有笔记链接到当前笔记。</div>
    </section>

    <section v-else-if="rightTab === 'outgoing'" class="min-h-0 flex-1 overflow-y-auto p-4">
      <p class="mb-3 text-xs font-medium tracking-wide text-zinc-400">当前笔记关联的笔记</p>
      <div v-if="outgoingNotes.length" class="space-y-1">
        <button v-for="note in outgoingNotes" :key="note.id" class="link-note-row" @click="emit('select-note', note.id)"><FileText class="h-4 w-4" /><span class="truncate">{{ note.title }}</span><ChevronRight class="ml-auto h-4 w-4 text-zinc-300" /></button>
      </div>
      <div v-else class="empty-state">在正文中插入关联笔记后，会显示在这里。</div>
    </section>

    <section v-else class="flex min-h-0 flex-1 flex-col">
      <div class="ai-conversation-wrap">
        <div ref="aiConversationRef" class="ai-conversation-scroll">
          <div class="min-w-[350px] space-y-4 px-4 py-5">
            <div class="ai-context-chip"><FileText class="h-3.5 w-3.5" />当前笔记：{{ noteTitle }}</div>
            <div v-for="message in activeConversationMessages" :key="message.id" class="ai-message" :class="message.role === 'user' ? 'ai-message-user' : 'ai-message-assistant'">
              <span class="ai-message-role">{{ message.role === 'user' ? '你' : 'AI' }}</span>
              <p>{{ message.content }}</p>
            </div>
          </div>
        </div>
        <div class="ai-scroll-controls">
          <button title="滚到顶部" @click="scrollAiConversation('top')"><ChevronsUp class="h-4 w-4" /></button>
          <button title="上一条提问" @click="scrollAiConversation('previous')"><ChevronUp class="h-4 w-4" /></button>
          <button title="下一条提问" @click="scrollAiConversation('next')"><ChevronDown class="h-4 w-4" /></button>
          <button title="滚到底部" @click="scrollAiConversation('bottom')"><ChevronsDown class="h-4 w-4" /></button>
        </div>
      </div>

      <div class="ai-session-bar">
        <div class="flex items-center gap-1">
          <button v-for="session in conversationSessions" :key="session.id" class="ai-session-tab" :class="activeSessionId === session.id ? 'ai-session-tab-active' : ''" @click="activeSessionId = session.id">{{ session.label }}</button>
        </div>
        <div class="ml-auto flex items-center gap-1">
          <button class="icon-button" title="新建会话框" @click="addConversationSession"><SquarePlus class="h-4 w-4" /></button>
          <button class="icon-button" title="新对话" @click="startNewConversation"><MessageSquarePlus class="h-4 w-4" /></button>
          <div ref="conversationHistoryRef" class="relative">
            <button class="icon-button" title="对话历史" @click="toggleConversationHistory"><History class="h-4 w-4" /></button>
            <div v-if="showConversationHistory" class="conversation-history-menu">
              <p>对话历史</p>
              <button v-for="item in conversationHistory" :key="item.id" @click="restoreConversation(item)"><MessageSquare class="h-4 w-4" /><span><strong>{{ item.title }}</strong><small>{{ item.time }}</small></span></button>
            </div>
          </div>
        </div>
      </div>

      <footer class="ai-compose">
        <textarea v-model="aiInput" rows="3" class="ai-input" placeholder="基于当前笔记提问..." @keydown.enter.exact.prevent="sendAiMessage" />
        <div class="ai-compose-tools">
          <div ref="modelMenuRef" class="relative">
            <button class="ai-control-text" @click="toggleModelMenu"><Bot class="h-4 w-4" />{{ selectedModel }}<ChevronDown class="h-3.5 w-3.5" /></button>
            <div v-if="showModelMenu" class="ai-popover model-menu"><button v-for="model in models" :key="model" @click="selectModel(model)">{{ model }}</button></div>
          </div>
          <div ref="thinkingMenuRef" class="relative">
            <button class="ai-control-text" @click="toggleThinkingMenu">Thinking: <strong>{{ thinkingMode }}</strong></button>
            <div v-if="showThinkingMenu" class="ai-popover thinking-menu"><button v-for="mode in thinkingModes" :key="mode" @click="selectThinkingMode(mode)">{{ mode }}</button></div>
          </div>
          <span class="ai-context-usage">16%</span>
          <button class="icon-button" title="上传文件"><Upload class="h-4 w-4" /></button>
          <button class="send-button" :class="aiInput.trim() ? 'send-button-active' : ''" :disabled="!aiInput.trim()" title="发送" @click="sendAiMessage"><CircleArrowUp class="h-4 w-4" /></button>
        </div>
      </footer>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import {
  ArrowUpDown, Bot, ChevronDown, ChevronRight, ChevronUp, ChevronsDown, ChevronsUp, CircleArrowUp,
  FileText, History, Link, MessageSquare, MessageSquarePlus, PanelRightClose, Sparkles, SquarePlus, Upload,
} from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import type { Id } from '@/types/common'
import type { ConversationHistoryItem, ConversationMessage, ConversationSession, NotebookNote } from './types'

/**
 * 右侧上下文面板：反向链接 / 出链 / AI 助手三个 Tab。
 *
 * <p>反链与出链由页面容器算好后传入；AI 会话（会话框、消息、模型、思考模式、对话历史）
 * 只在本组件内使用，因此状态全部留在组件内部。
 */
defineProps<{
  visible: boolean
  width: number
  /** 当前笔记标题（AI 上下文提示 + 反链标题都用它） */
  noteTitle: string
  backlinkNotes: NotebookNote[]
  outgoingNotes: NotebookNote[]
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  'resize-start': [event: MouseEvent]
  'select-note': [noteId: Id]
}>()

const rightTab = ref<'backlinks' | 'outgoing' | 'ai'>('ai')

const rightTabs = [
  { key: 'backlinks', icon: Link, label: '反向链接' },
  { key: 'outgoing', icon: ArrowUpDown, label: '出链' },
  { key: 'ai', icon: Sparkles, label: 'AI 助手' },
] as const

const aiInput = ref('')
const aiConversationRef = ref<HTMLElement | null>(null)
const activeSessionId = ref('session-1')
const conversationSessions = ref<ConversationSession[]>([
  { id: 'session-1', label: '1' },
  { id: 'session-2', label: '2' },
])
const conversationMessages = ref<Record<string, ConversationMessage[]>>({
  'session-1': [
    { id: 'intro', role: 'assistant', content: '我已读取当前笔记。可以基于内容帮你总结、解释、润色，或补充结构化内容。' },
    { id: 'question', role: 'user', content: '请帮我梳理这篇笔记的核心链路。' },
    { id: 'answer', role: 'assistant', content: '核心链路是：用低门槛编辑器沉淀 Markdown 笔记，再按需同步进入知识库，经过分块和向量化后用于 RAG 问答。' },
  ],
  'session-2': [
    { id: 'session-two', role: 'assistant', content: '这是第二个会话框。它与第一个会话的上下文独立。' },
  ],
})
const conversationHistory: ConversationHistoryItem[] = [
  { id: 'history-1', title: '梳理 NoteMind 的核心链路', time: '刚刚' },
  { id: 'history-2', title: '生成数据库设计说明', time: '昨天' },
  { id: 'history-3', title: '解释 RAG 文档分块策略', time: '3 天前' },
]
const selectedModel = ref('DeepSeek V4 Flash[1m]')
const models = ['DeepSeek V4 Flash[1m]', 'Opus 1M', 'Sonnet 1M', 'Haiku']
const thinkingMode = ref('High')
const thinkingModes = ['Ultra', 'High', 'Med', 'Low', 'Off']

// 三个下拉：对话历史 / 模型 / 思考模式
const conversationHistoryRef = ref<HTMLElement | null>(null)
const { isOpen: showConversationHistory, toggle: toggleConversationHistory, close: closeConversationHistory } = useDropdown(conversationHistoryRef)
const modelMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showModelMenu, toggle: toggleModelMenu, close: closeModelMenu } = useDropdown(modelMenuRef)
const thinkingMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showThinkingMenu, toggle: toggleThinkingMenu, close: closeThinkingMenu } = useDropdown(thinkingMenuRef)

const activeConversationMessages = computed(() => conversationMessages.value[activeSessionId.value] || [])

function addConversationSession(): void {
  const id = `session-${Date.now()}`
  conversationSessions.value.push({ id, label: String(conversationSessions.value.length + 1) })
  conversationMessages.value[id] = [{ id: `welcome-${id}`, role: 'assistant', content: '这是一个新的会话框，可以基于当前笔记开始交流。' }]
  activeSessionId.value = id
}

function startNewConversation(): void {
  conversationMessages.value[activeSessionId.value] = [{ id: `new-${Date.now()}`, role: 'assistant', content: '已开始新的对话。当前笔记仍会作为上下文提供给 AI。' }]
}

function restoreConversation(item: ConversationHistoryItem): void {
  conversationMessages.value[activeSessionId.value] = [
    { id: `${item.id}-user`, role: 'user', content: item.title },
    { id: `${item.id}-assistant`, role: 'assistant', content: '已恢复这段历史对话。你可以继续围绕当前笔记提问。' },
  ]
  closeConversationHistory()
  nextTick(() => scrollAiConversation('bottom'))
}

function sendAiMessage(): void {
  const message = aiInput.value.trim()
  if (!message) return
  const messages = conversationMessages.value[activeSessionId.value] || []
  messages.push({ id: `user-${Date.now()}`, role: 'user', content: message })
  messages.push({ id: `assistant-${Date.now()}`, role: 'assistant', content: '这是 AI 助手原型回复。后续会通过流式接口，结合当前笔记和所选模型生成真实回答。' })
  conversationMessages.value[activeSessionId.value] = messages
  aiInput.value = ''
  nextTick(() => scrollAiConversation('bottom'))
}

function scrollAiConversation(direction: 'top' | 'previous' | 'next' | 'bottom'): void {
  const container = aiConversationRef.value
  if (!container) return
  if (direction === 'top') container.scrollTo({ top: 0, behavior: 'smooth' })
  else if (direction === 'bottom') container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' })
  else container.scrollBy({ top: direction === 'previous' ? -160 : 160, behavior: 'smooth' })
}

function selectModel(model: string): void {
  selectedModel.value = model
  closeModelMenu()
}

function selectThinkingMode(mode: string): void {
  thinkingMode.value = mode
  closeThinkingMenu()
}
</script>

<style scoped>
.side-tab, .icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: background-color .15s ease, color .15s ease;
}
.side-tab, .icon-button { height: 30px; width: 30px; }
.side-tab:hover, .icon-button:hover { background: #f0f0ee; color: #27272a; }
.side-tab-active { background: var(--mint); color: #247456; }
.side-panel-resizer { position: absolute; z-index: 20; top: 0; right: -3px; height: 100%; width: 6px; cursor: col-resize; }
.right-resizer { right: auto; left: -3px; }
.side-panel-resizer:hover { background: rgba(60, 146, 112, .2); }
.empty-state { border: 1px dashed #e4e4e7; border-radius: 6px; padding: 14px; font-size: 13px; line-height: 1.6; color: #a1a1aa; }
.link-note-row { display: flex; width: 100%; align-items: center; gap: 9px; border-radius: 6px; padding: 9px 8px; text-align: left; color: #52525b; font-size: 13px; }.link-note-row:hover { background: #e8f5ee; color: #247456; }
.ai-input { width: 100%; resize: none; border: 1px solid #e4e4e7; border-radius: 7px; background: #fafafa; padding: 9px 10px; outline: none; font-size: 13px; line-height: 1.5; }
.send-button { display: inline-flex; height: 30px; width: 30px; align-items: center; justify-content: center; border-radius: 6px; background: #d4d4d8; color: white; }
.send-button-active { background: #3c9270; }
.ai-conversation-wrap { position: relative; min-height: 0; flex: 1; }.ai-conversation-scroll { height: 100%; overflow: auto; scrollbar-gutter: stable; }.ai-scroll-controls { position: absolute; top: 50%; right: 8px; display: flex; transform: translateY(-50%); flex-direction: column; gap: 7px; }.ai-scroll-controls button { display: inline-flex; height: 29px; width: 29px; align-items: center; justify-content: center; border: 1px solid #e4e4e7; border-radius: 999px; background: #fff; color: #71717a; box-shadow: 0 2px 5px rgba(24,24,27,.08); }.ai-scroll-controls button:hover { border-color: #9dceb8; background: #f2faf5; color: #247456; }
.ai-context-chip { display: inline-flex; max-width: 100%; align-items: center; gap: 5px; border-radius: 5px; background: #f0f4f2; padding: 5px 7px; color: #71717a; font-size: 11px; }.ai-message { max-width: 93%; border-radius: 8px; padding: 10px 11px; font-size: 13px; line-height: 1.65; }.ai-message p { white-space: pre-wrap; }.ai-message-role { display: block; margin-bottom: 4px; color: #a1a1aa; font-size: 11px; font-weight: 600; }.ai-message-assistant { border: 1px solid #ececec; background: #fff; color: #52525b; }.ai-message-user { margin-left: auto; background: #e8f5ee; color: #245f49; }
.ai-session-bar { position: relative; display: flex; min-height: 39px; align-items: center; border-top: 1px solid #e9e9e9; padding: 4px 9px; }.ai-session-tab { display: inline-flex; height: 25px; min-width: 25px; align-items: center; justify-content: center; border: 1px solid #d8d8dc; border-radius: 4px; color: #71717a; font-size: 12px; }.ai-session-tab-active { border-color: #8b63f6; box-shadow: inset 0 0 0 1px #8b63f6; color: #6944d5; }.conversation-history-menu { position: absolute; z-index: 60; right: -2px; bottom: calc(100% + 7px); width: 286px; max-height: 340px; overflow-y: auto; border-radius: 10px; background: #fff; padding: 8px 0; box-shadow: 0 9px 26px rgba(24,24,27,.16); }.conversation-history-menu > p { padding: 5px 14px 8px; color: #71717a; font-size: 12px; font-weight: 700; text-transform: uppercase; }.conversation-history-menu button { display: flex; width: 100%; align-items: flex-start; gap: 10px; border-top: 1px solid #f0f0f0; padding: 10px 14px; text-align: left; color: #52525b; }.conversation-history-menu button:hover { background: #f4faf6; }.conversation-history-menu strong, .conversation-history-menu small { display: block; }.conversation-history-menu strong { font-size: 13px; font-weight: 500; }.conversation-history-menu small { margin-top: 3px; color: #a1a1aa; font-size: 11px; }
.ai-compose { padding: 8px 9px 40px; border-top: 1px solid #e9e9e9; background: #fff; }.ai-compose-tools { display: flex; align-items: center; gap: 7px; margin-top: 6px; white-space: nowrap; }.ai-control-text { display: inline-flex; min-width: 0; align-items: center; gap: 4px; border-radius: 5px; padding: 4px; color: #ba684f; font-size: 11px; }.ai-control-text:hover { background: #fff3ee; }.ai-context-usage { color: #d08065; font-size: 11px; }.ai-popover { position: absolute; z-index: 62; bottom: calc(100% + 6px); left: 0; min-width: 170px; overflow: hidden; border: 1px solid #e4e4e7; border-radius: 7px; background: #fff; padding: 4px; box-shadow: 0 8px 22px rgba(24,24,27,.14); }.ai-popover button { display: block; width: 100%; border-radius: 5px; padding: 7px 8px; text-align: left; color: #52525b; font-size: 12px; }.ai-popover button:hover { background: #fff0e9; color: #b95e40; }.thinking-menu { min-width: 82px; }
@media (max-width: 1180px) { .right-resizer + header ~ section { min-width: 0; } }
</style>

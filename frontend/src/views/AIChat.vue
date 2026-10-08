<template>
  <div class="flex h-full min-w-0 overflow-hidden bg-[#fdfdfc] text-zinc-800">
    <aside v-if="!sidebarCollapsed" class="chat-sidebar">
      <header class="flex h-[82px] shrink-0 items-center justify-between px-5">
        <span class="sidebar-logo" aria-label="NoteMind AI">
          <Sparkles class="h-7 w-7" stroke-width="2" />
        </span>
        <div class="flex items-center gap-1">
          <button class="sidebar-tool" title="搜索聊天" @click="openSearchDialog">
            <Search class="h-5 w-5" />
          </button>
          <button class="sidebar-tool" title="收起侧栏" @click="sidebarCollapsed = true">
            <PanelLeftClose class="h-5 w-5" />
          </button>
        </div>
      </header>

      <div class="px-3 pb-3">
        <button class="new-chat-button" @click="startNewChat">
          <CirclePlus class="h-5 w-5" />
          <span>开启新对话</span>
        </button>
      </div>

      <div class="sidebar-scroll scrollbar-thin">
        <section class="sidebar-section">
          <div class="section-header">
            <button class="section-heading-button" title="展开或收起已置顶列表" @click="pinnedExpanded = !pinnedExpanded">
              <span>已置顶</span>
              <ChevronDown class="h-4 w-4 transition-transform" :class="pinnedExpanded ? '' : '-rotate-90'" />
            </button>
          </div>
          <template v-if="pinnedExpanded">
            <button
              v-for="item in pinnedConversations"
              :key="item.id"
              class="sidebar-row chat-row group"
              :class="[
                activeConversationId === item.id ? 'sidebar-row-active' : '',
                hoveredConversationId === item.id && titleScrollOffsets[item.id] ? 'chat-row-title-scrolling' : '',
              ]"
              @click="selectConversation(item)"
              @mouseenter="startTitleScroll($event, item.id)"
              @mouseleave="stopTitleScroll"
            >
              <MessageCircle class="h-5 w-5 shrink-0" />
              <span class="conversation-title">
                <span class="conversation-title-static">{{ item.title }}</span>
                <span class="conversation-title-marquee">
                  <span class="conversation-title-marquee-content" :style="{ '--title-scroll-distance': titleScrollOffsets[item.id] || '0px' }">
                    {{ item.title }}
                  </span>
                </span>
              </span>
              <span class="chat-row-actions">
                <button class="chat-row-action" title="取消置顶" @click.stop="unpinConversation(item)">
                  <PinOff class="h-[18px] w-[18px]" />
                </button>
                <button class="chat-row-action" title="更多操作" @click.stop="openConversationMenu($event, item.id)">
                  <MoreHorizontal class="h-[18px] w-[18px]" />
                </button>
              </span>
            </button>
            <p v-if="!pinnedConversations.length" class="px-4 py-3 text-sm text-zinc-400">暂无置顶聊天</p>
          </template>
        </section>

        <section class="sidebar-section pt-7">
          <div class="section-header">
            <button class="section-heading-button" title="展开或收起聊天列表" @click="chatsExpanded = !chatsExpanded">
              <span>聊天</span>
              <ChevronDown class="h-4 w-4 transition-transform" :class="chatsExpanded ? '' : '-rotate-90'" />
            </button>
          </div>

          <template v-if="chatsExpanded">
            <button
              v-for="item in filteredConversations"
              :key="item.id"
              class="sidebar-row chat-row group"
              :class="[
                activeConversationId === item.id ? 'sidebar-row-active' : '',
                hoveredConversationId === item.id && titleScrollOffsets[item.id] ? 'chat-row-title-scrolling' : '',
              ]"
              draggable="true"
              @click="selectConversation(item)"
              @mouseenter="startTitleScroll($event, item.id)"
              @mouseleave="stopTitleScroll"
              @dragstart="startConversationDrag($event, item.id)"
              @dragover.prevent
              @drop.prevent="dropConversation(item.id)"
              @dragend="draggedConversationId = null"
            >
              <MessageCircle class="h-5 w-5 shrink-0" />
              <span class="conversation-title">
                <span class="conversation-title-static">{{ item.title }}</span>
                <span class="conversation-title-marquee">
                  <span class="conversation-title-marquee-content" :style="{ '--title-scroll-distance': titleScrollOffsets[item.id] || '0px' }">
                    {{ item.title }}
                  </span>
                </span>
              </span>
              <span class="chat-row-actions">
                <button class="chat-row-action" title="置顶聊天" @click.stop="pinConversation(item)">
                  <Pin class="h-[18px] w-[18px]" />
                </button>
                <button class="chat-row-action" title="更多操作" @click.stop="openConversationMenu($event, item.id)">
                  <MoreHorizontal class="h-[18px] w-[18px]" />
                </button>
              </span>
            </button>
            <p v-if="!filteredConversations.length" class="px-4 py-4 text-sm text-zinc-400">没有匹配的聊天</p>
          </template>
        </section>
      </div>
    </aside>

    <aside v-else class="collapsed-sidebar">
      <button class="collapsed-logo-toggle" title="展开侧栏" @click="openSidebar">
        <Sparkles class="collapsed-brand-icon h-7 w-7" stroke-width="2" />
        <PanelLeftOpen class="collapsed-open-icon h-6 w-6" />
      </button>
      <div class="mt-5 flex flex-col items-center gap-4">
        <button class="collapsed-tool" title="新聊天" @click="startNewChat">
          <SquarePen class="h-6 w-6" />
        </button>
        <button class="collapsed-tool" title="搜索聊天" @click="openSearch">
          <Search class="h-6 w-6" />
        </button>
        <button class="collapsed-tool" title="查看置顶聊天" @click="openCollapsedList($event, 'pinned')">
          <Pin class="h-6 w-6" />
        </button>
        <button class="collapsed-tool" title="查看聊天" @click="openCollapsedList($event, 'chat')">
          <MessageCircle class="h-6 w-6" />
        </button>
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

  <Teleport to="body">
    <div v-if="collapsedListType" class="fixed inset-0 z-[89]" @click="closeCollapsedList" />
    <section v-if="collapsedListType" class="collapsed-list-popup" :style="collapsedListStyle">
      <h2>{{ collapsedListType === 'pinned' ? '已置顶' : '最近聊天' }}</h2>
      <div class="collapsed-list-content scrollbar-thin">
        <template v-if="collapsedListType === 'pinned'">
          <button
            v-for="item in pinnedConversations"
            :key="item.id"
            class="sidebar-row chat-row group"
            :class="hoveredConversationId === item.id && titleScrollOffsets[item.id] ? 'chat-row-title-scrolling' : ''"
            @click="selectCollapsedConversation(item)"
            @mouseenter="startTitleScroll($event, item.id)"
            @mouseleave="stopTitleScroll"
          >
            <MessageCircle class="h-5 w-5 shrink-0" />
            <span class="conversation-title">
              <span class="conversation-title-static">{{ item.title }}</span>
              <span class="conversation-title-marquee"><span class="conversation-title-marquee-content" :style="{ '--title-scroll-distance': titleScrollOffsets[item.id] || '0px' }">{{ item.title }}</span></span>
            </span>
            <span class="chat-row-actions">
              <button class="chat-row-action" title="取消置顶" @click.stop="unpinConversation(item)"><PinOff class="h-[18px] w-[18px]" /></button>
              <button class="chat-row-action" title="更多操作" @click.stop="openConversationMenu($event, item.id)"><MoreHorizontal class="h-[18px] w-[18px]" /></button>
            </span>
          </button>
          <p v-if="!pinnedConversations.length" class="px-4 py-4 text-sm text-zinc-400">暂无置顶聊天</p>
        </template>

        <template v-else>
          <button
            v-for="item in filteredConversations"
            :key="item.id"
            class="sidebar-row chat-row group"
            :class="hoveredConversationId === item.id && titleScrollOffsets[item.id] ? 'chat-row-title-scrolling' : ''"
            @click="selectCollapsedConversation(item)"
            @mouseenter="startTitleScroll($event, item.id)"
            @mouseleave="stopTitleScroll"
          >
            <MessageCircle class="h-5 w-5 shrink-0" />
            <span class="conversation-title">
              <span class="conversation-title-static">{{ item.title }}</span>
              <span class="conversation-title-marquee"><span class="conversation-title-marquee-content" :style="{ '--title-scroll-distance': titleScrollOffsets[item.id] || '0px' }">{{ item.title }}</span></span>
            </span>
            <span class="chat-row-actions">
              <button class="chat-row-action" title="置顶聊天" @click.stop="pinConversation(item)"><Pin class="h-[18px] w-[18px]" /></button>
              <button class="chat-row-action" title="更多操作" @click.stop="openConversationMenu($event, item.id)"><MoreHorizontal class="h-[18px] w-[18px]" /></button>
            </span>
          </button>
          <p v-if="!filteredConversations.length" class="px-4 py-4 text-sm text-zinc-400">暂无聊天</p>
        </template>
      </div>
    </section>

    <div v-if="searchDialogVisible" class="search-dialog-backdrop" @click.self="closeSearchDialog">
      <section class="search-dialog" :class="searchKeyword ? 'search-dialog-expanded' : ''" role="dialog" aria-modal="true">
        <div class="search-dialog-input">
          <Search class="h-5 w-5 shrink-0" />
          <input
            ref="searchInputRef"
            v-model="searchKeyword"
            autofocus
            placeholder="搜索对话内容..."
            @keydown.esc="closeSearchDialog"
          />
          <button title="关闭搜索" @click="closeSearchDialog"><X class="h-5 w-5" /></button>
        </div>

        <div v-if="searchKeyword" class="search-results scrollbar-thin" @scroll="loadMoreSearchResults">
          <button
            v-for="result in visibleSearchResults"
            :key="result.id"
            class="search-result-item"
            @click="selectSearchResult(result)"
          >
            <span class="search-result-icon"><MessageCircle class="h-4 w-4" /></span>
            <span class="min-w-0 flex-1 text-left">
              <span class="flex items-center justify-between gap-4">
                <strong class="truncate">{{ result.title }}</strong>
                <time>{{ result.date }}</time>
              </span>
              <span class="search-result-preview">{{ result.preview }}</span>
            </span>
          </button>
          <div v-if="visibleSearchResults.length < searchResults.length" class="search-result-loading">继续向下滚动以加载更多结果</div>
          <div v-else class="search-result-loading">已显示全部匹配结果</div>
        </div>
      </section>
    </div>

    <div v-if="conversationMenuId !== null" class="fixed inset-0 z-[89]" @click="closeConversationMenu" />
    <div v-if="conversationMenuId !== null" class="conversation-options-menu" :style="conversationMenuStyle">
      <button class="conversation-menu-option" @click="openRenameDialog"><PenLine class="h-5 w-5" />重命名</button>
      <button class="conversation-menu-option" @click="toggleCurrentConversationPin">
        <component :is="currentConversationIsPinned ? PinOff : Pin" class="h-5 w-5" />
        {{ currentConversationIsPinned ? '取消置顶聊天' : '置顶聊天' }}
      </button>
      <div class="my-1 border-t border-zinc-100" />
      <button class="conversation-menu-option conversation-menu-delete" @click="requestDeleteConversation"><Trash2 class="h-5 w-5" />删除</button>
    </div>

    <div v-if="renameDialogVisible" class="rename-dialog-backdrop" @click.self="closeRenameDialog">
      <form class="rename-dialog" @submit.prevent="renameConversation">
        <h2>重命名聊天</h2>
        <input v-model="renameValue" autofocus maxlength="48" placeholder="输入聊天名称" />
        <footer>
          <button type="button" class="project-dialog-cancel" @click="closeRenameDialog">取消</button>
          <button class="project-dialog-submit" :disabled="!renameValue.trim()">保存</button>
        </footer>
      </form>
    </div>

    <div v-if="deleteDialogVisible" class="rename-dialog-backdrop" @click.self="closeDeleteDialog">
      <section class="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-chat-title">
        <h2 id="delete-chat-title">删除聊天？</h2>
        <p>这会删除“<strong>{{ deleteTargetTitle }}</strong>”。</p>
        <footer>
          <button class="delete-dialog-cancel" @click="closeDeleteDialog">取消</button>
          <button class="delete-dialog-confirm" @click="confirmDeleteConversation">删除</button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, ref, watch } from 'vue'
import type { Component, PropType } from 'vue'
import type { ChatMessage, MessageRole } from '@/types/ai'
import {
  AtSign,
  BarChart3,
  Bot,
  CirclePlus,
  BookOpen,
  Brain,
  Braces,
  BriefcaseBusiness,
  Check,
  ChevronDown,
  FilePenLine,
  FlaskConical,
  Flower2,
  Folder,
  Globe2,
  GraduationCap,
  Heart,
  Lightbulb,
  MessageSquare,
  MoreHorizontal,
  Music2,
  PanelLeftClose,
  PanelLeftOpen,
  Paperclip,
  Palette,
  PawPrint,
  PenLine,
  Pin,
  PinOff,
  Plane,
  Plus,
  Scale,
  Search,
  Send,
  Sparkles,
  SquarePen,
  Terminal,
  Trash2,
  WandSparkles,
  Wrench,
  MessageCircle,
  X,
} from 'lucide-vue-next'

interface ChatConversation { id: string; title: string }
interface SidebarConversation extends ChatConversation { icon: Component; iconClass?: string }
interface SearchResult { id: string; title: string; preview: string; date: string }
type CollapsedListType = 'pinned' | 'chat'
type PrototypeMessage = Required<Pick<ChatMessage, 'id' | 'role' | 'content'>>

const inputText = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const activeConversationId = ref('history-1')
const messages = ref<PrototypeMessage[]>([])
const sidebarCollapsed = ref(false)
const searchDialogVisible = ref(false)
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchKeyword = ref('')
const searchResultLimit = ref(8)
const pinnedExpanded = ref(true)
const chatsExpanded = ref(true)
const collapsedListType = ref<CollapsedListType | null>(null)
const collapsedListStyle = ref<Record<string, string>>({})
const conversationMenuId = ref<string | null>(null)
const conversationMenuStyle = ref<Record<string, string>>({})
const renameDialogVisible = ref(false)
const renameValue = ref('')
const deleteDialogVisible = ref(false)
const deleteTargetId = ref<string | null>(null)
const deleteTargetTitle = ref('')
const draggedConversationId = ref<string | null>(null)
const hoveredConversationId = ref<string | null>(null)
const titleScrollOffsets = ref<Record<string, string>>({})

const conversations = ref<ChatConversation[]>([
  { id: 'history-1', title: '实习协议审核' },
  { id: 'history-2', title: '毕业设计流程咨询' },
  { id: 'history-3', title: 'Steam 域名与 VPN 设置' },
  { id: 'history-4', title: '需求分析后续步骤' },
  { id: 'history-5', title: '崩坏星穹铁道服务器切换' },
  { id: 'history-6', title: '产品定位与需求分析' },
  { id: 'history-7', title: '千问重排模型定位' },
  { id: 'history-8', title: 'Milvus Docker 部署建议' },
])

const pinnedConversations = ref<SidebarConversation[]>([
  { id: 'pinned-1', title: '非遗数字资源上传存储管理', icon: Folder },
  { id: 'pinned-2', title: '图片生成', icon: Palette },
  { id: 'pinned-3', title: '面试聊天回复', icon: Folder },
  { id: 'pinned-4', title: '面试结果分析', icon: Folder },
  { id: 'pinned-5', title: '简历写作与工具选择', icon: MessageCircle },
  { id: 'pinned-6', title: '日报撰写建议', icon: MessageCircle },
])

const filteredConversations = computed(() => {
  return [...conversations.value]
})

const searchResults = computed<SearchResult[]>(() => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return []
  const conversationsResults = [...conversations.value, ...pinnedConversations.value].map((item, index) => ({
    id: item.id,
    title: item.title,
    preview: `与“${keyword}”相关的对话内容、笔记整理和后续讨论。`,
    date: index < 2 ? '今天' : `${index + 1} 天前`,
  }))
  const prototypeResults = Array.from({ length: 24 }, (_, index) => ({
    id: `prototype-search-${index}`,
    title: `${keyword}：学习与实践记录 ${index + 1}`,
    preview: `这里展示与 ${keyword} 相关的模拟对话摘要，用于搜索结果的前端原型。`,
    date: index < 3 ? '今天' : `${Math.min(index + 1, 30)} 天前`,
  }))
  return [...conversationsResults, ...prototypeResults]
})

const visibleSearchResults = computed(() => searchResults.value.slice(0, searchResultLimit.value))

watch(searchKeyword, () => {
  searchResultLimit.value = 8
})

const selectedConversation = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)
  || pinnedConversations.value.find((item) => item.id === activeConversationId.value)
)

const currentConversationIsPinned = computed(() =>
  pinnedConversations.value.some((item) => item.id === conversationMenuId.value)
)

function startNewChat() {
  activeConversationId.value = 'new'
  messages.value = []
  inputText.value = ''
}

function openSidebar() {
  sidebarCollapsed.value = false
}

function openSearch() {
  openSearchDialog()
}

function openCollapsedList(event: MouseEvent, type: CollapsedListType) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const popupHeight = 640
  collapsedListStyle.value = {
    left: `${rect.right + 10}px`,
    top: `${Math.max(16, Math.min(rect.top - 24, window.innerHeight - popupHeight - 16))}px`,
  }
  collapsedListType.value = type
}

function closeCollapsedList() {
  collapsedListType.value = null
}

function selectCollapsedConversation(item: ChatConversation) {
  selectConversation(item)
  closeCollapsedList()
}

function openSearchDialog() {
  searchKeyword.value = ''
  searchResultLimit.value = 8
  searchDialogVisible.value = true
  nextTick(() => searchInputRef.value?.focus())
}

function closeSearchDialog() {
  searchDialogVisible.value = false
  searchKeyword.value = ''
}

function loadMoreSearchResults(event: Event) {
  const container = event.currentTarget as HTMLElement
  const nearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 24
  if (nearBottom && searchResultLimit.value < searchResults.value.length) {
    searchResultLimit.value = Math.min(searchResultLimit.value + 8, searchResults.value.length)
  }
}

function selectSearchResult(result: SearchResult) {
  const conversation = conversations.value.find((item) => item.id === result.id)
    || pinnedConversations.value.find((item) => item.id === result.id)
  if (conversation) selectConversation(conversation)
  closeSearchDialog()
}

function pinConversation(item: ChatConversation) {
  const index = conversations.value.findIndex((conversation) => conversation.id === item.id)
  if (index === -1) return
  const [conversation] = conversations.value.splice(index, 1)
  pinnedConversations.value.unshift({ ...conversation, icon: MessageCircle })
  activeConversationId.value = conversation.id
}

function unpinConversation(item: ChatConversation) {
  const index = pinnedConversations.value.findIndex((conversation) => conversation.id === item.id)
  if (index === -1) return
  const [conversation] = pinnedConversations.value.splice(index, 1)
  conversations.value.unshift({ id: conversation.id, title: conversation.title })
  activeConversationId.value = conversation.id
}

function openConversationMenu(event: MouseEvent, conversationId: string) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const menuHeight = 126
  conversationMenuStyle.value = window.innerHeight - rect.bottom < menuHeight
    ? { left: `${rect.left}px`, bottom: `${window.innerHeight - rect.top}px` }
    : { left: `${rect.left}px`, top: `${rect.bottom}px` }
  activeConversationId.value = conversationId
  conversationMenuId.value = conversationId
}

function closeConversationMenu() {
  conversationMenuId.value = null
}

function currentConversation() {
  return conversations.value.find((item) => item.id === conversationMenuId.value)
    || pinnedConversations.value.find((item) => item.id === conversationMenuId.value)
    || null
}

function toggleCurrentConversationPin() {
  const conversation = currentConversation()
  if (!conversation) return
  if (currentConversationIsPinned.value) unpinConversation(conversation)
  else pinConversation(conversation)
  closeConversationMenu()
}

function requestDeleteConversation() {
  const conversation = currentConversation()
  if (!conversation) return
  deleteTargetId.value = conversation.id
  deleteTargetTitle.value = conversation.title
  deleteDialogVisible.value = true
  closeConversationMenu()
}

function closeDeleteDialog() {
  deleteDialogVisible.value = false
  deleteTargetId.value = null
  deleteTargetTitle.value = ''
}

function confirmDeleteConversation() {
  const target = conversations.value.find((item) => item.id === deleteTargetId.value)
    || pinnedConversations.value.find((item) => item.id === deleteTargetId.value)
  if (target) {
    conversations.value = conversations.value.filter((item) => item.id !== target.id)
    pinnedConversations.value = pinnedConversations.value.filter((item) => item.id !== target.id)
    if (activeConversationId.value === target.id) startNewChat()
  }
  closeDeleteDialog()
}

function openRenameDialog() {
  const conversation = currentConversation()
  if (!conversation) return
  renameValue.value = conversation.title
  renameDialogVisible.value = true
  closeConversationMenu()
}

function closeRenameDialog() {
  renameDialogVisible.value = false
  renameValue.value = ''
}

function renameConversation() {
  const conversation = conversations.value.find((item) => item.id === activeConversationId.value)
    || pinnedConversations.value.find((item) => item.id === activeConversationId.value)
  const title = renameValue.value.trim()
  if (conversation && title) conversation.title = title
  closeRenameDialog()
}

function startTitleScroll(event: MouseEvent, conversationId: string) {
  hoveredConversationId.value = conversationId
  const row = event.currentTarget as HTMLElement
  window.setTimeout(() => {
    if (hoveredConversationId.value !== conversationId) return
    const viewport = row.querySelector<HTMLElement>('.conversation-title-marquee')
    const content = row.querySelector<HTMLElement>('.conversation-title-marquee-content')
    if (!viewport || !content) return
    const overflow = content.scrollWidth - viewport.clientWidth
    titleScrollOffsets.value[conversationId] = overflow > 0 ? `-${overflow}px` : '0px'
  }, 180)
}

function stopTitleScroll() {
  hoveredConversationId.value = null
}

function startConversationDrag(event: DragEvent, conversationId: string) {
  draggedConversationId.value = conversationId
  event.dataTransfer?.setData('text/plain', conversationId)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function dropConversation(targetId: string) {
  const sourceId = draggedConversationId.value
  if (!sourceId || sourceId === targetId) return
  const sourceIndex = conversations.value.findIndex((item) => item.id === sourceId)
  const targetIndex = conversations.value.findIndex((item) => item.id === targetId)
  if (sourceIndex === -1 || targetIndex === -1) return
  const [moved] = conversations.value.splice(sourceIndex, 1)
  const insertIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  conversations.value.splice(insertIndex, 0, moved)
  draggedConversationId.value = null
}

function selectConversation(item: ChatConversation) {
  activeConversationId.value = item.id
  inputText.value = item.title
  messages.value = []
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
.chat-sidebar {
  display: flex;
  width: 300px;
  min-width: 300px;
  flex-direction: column;
  border-right: 1px solid #ececec;
  background: #fdfdfc;
}

.collapsed-sidebar {
  display: flex;
  width: 64px;
  min-width: 64px;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #ececec;
  background: #fdfdfc;
  padding-top: 12px;
}

.sidebar-logo {
  display: inline-flex;
  height: 38px;
  width: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #1f1f1f;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.sidebar-tool:hover {
  background: #f1f1f1;
}

.sidebar-tool {
  display: flex;
  height: 36px;
  width: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #747474;
  transition: background-color 0.18s ease;
}

.collapsed-tool,
.collapsed-logo-toggle {
  display: inline-flex;
  height: 48px;
  width: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #181818;
  transition: background-color 0.18s ease;
}

.collapsed-tool:hover,
.collapsed-logo-toggle:hover {
  background: #eeeeee;
}

.collapsed-brand-icon {
  display: block;
}

.collapsed-open-icon {
  display: none;
}

.collapsed-logo-toggle:hover .collapsed-brand-icon {
  display: none;
}

.collapsed-logo-toggle:hover .collapsed-open-icon {
  display: block;
}

.collapsed-list-popup {
  position: fixed;
  z-index: 90;
  width: min(390px, calc(100vw - 92px));
  max-height: min(640px, calc(100vh - 32px));
  overflow: hidden;
  border: 1px solid #e5e5e5;
  border-radius: 24px;
  background: #ffffff;
  padding: 18px 15px 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}

.collapsed-list-popup h2 {
  margin: 6px 12px 12px;
  color: #1f1f1f;
  font-size: 18px;
  font-weight: 700;
}

.collapsed-list-content {
  max-height: min(560px, calc(100vh - 120px));
  overflow-y: auto;
}

.sidebar-search {
  display: flex;
  height: 36px;
  align-items: center;
  gap: 8px;
  border-radius: 7px;
  background: #f3f3f3;
  padding: 0 10px;
  color: #777777;
}

.sidebar-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  color: #262626;
  font-size: 14px;
  outline: none;
}

.sidebar-search input::placeholder {
  color: #989898;
}

.new-chat-button {
  display: flex;
  height: 52px;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid #ececec;
  border-radius: 999px;
  background: #ffffff;
  padding: 0 16px;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #202020;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: background-color 0.18s ease, box-shadow 0.18s ease;
}

.new-chat-button:hover {
  background: #fafafa;
  box-shadow: 0 3px 9px rgba(0, 0, 0, 0.11);
}

.sidebar-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 7px 12px 24px;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-heading {
  margin: 0;
  padding: 8px 9px 11px;
  font-size: 16px;
  font-weight: 700;
  color: #262626;
}

.project-section-header {
  position: relative;
  display: flex;
  min-height: 43px;
  align-items: center;
  justify-content: space-between;
  padding: 0 3px 7px 9px;
}

.project-heading-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  border-radius: 6px;
  padding: 5px 0;
  font-size: 16px;
  font-weight: 700;
  color: #262626;
}

.project-heading-button:hover {
  color: #555555;
}

.project-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.project-menu-anchor {
  position: relative;
}

.project-action-button {
  display: inline-flex;
  height: 30px;
  width: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: #777777;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.project-action-button:hover {
  background: #eeeeee;
  color: #262626;
}

.project-options-menu {
  position: fixed;
  z-index: 90;
  width: 220px;
  border: 1px solid #dddddd;
  border-radius: 14px;
  background: #ffffff;
  padding: 9px 8px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
}

.project-menu-label {
  margin: 0;
  padding: 4px 9px 7px;
  font-size: 13px;
  color: #999999;
}

.project-menu-option {
  display: flex;
  height: 34px;
  width: 100%;
  align-items: center;
  gap: 9px;
  border-radius: 7px;
  padding: 0 9px;
  text-align: left;
  font-size: 14px;
  color: #383838;
}

.project-menu-option:hover {
  background: #f2f2f2;
}

.section-header {
  position: relative;
  display: flex;
  min-height: 43px;
  align-items: center;
  justify-content: space-between;
  padding: 0 3px 7px 9px;
}

.section-heading-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  border-radius: 6px;
  padding: 5px 0;
  font-size: 16px;
  font-weight: 700;
  color: #262626;
}

.section-heading-button:hover {
  color: #555555;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.section-header:hover .section-actions,
.section-header:focus-within .section-actions {
  opacity: 1;
  pointer-events: auto;
}

.section-menu-anchor {
  position: relative;
}

.section-action-button,
.chat-row-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: #777777;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.section-action-button {
  height: 30px;
  width: 30px;
}

.section-action-button:hover,
.chat-row-action:hover {
  background: #e9e9e9;
  color: #262626;
}

.chat-row {
  gap: 0;
  cursor: grab;
  transition: gap 0.16s ease, background-color 0.18s ease, color 0.18s ease;
}

.chat-row:active {
  cursor: grabbing;
}

.chat-row:hover,
.chat-row:focus-within {
  gap: 8px;
}

.conversation-title {
  min-width: 0;
  flex: 1;
}

.conversation-title-static {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-title-marquee {
  display: none;
  overflow: hidden;
  white-space: nowrap;
}

.conversation-title-marquee-content {
  display: inline-block;
  white-space: nowrap;
}

.chat-row:hover .conversation-title-static,
.chat-row:focus-within .conversation-title-static {
  display: none;
}

.chat-row:hover .conversation-title-marquee,
.chat-row:focus-within .conversation-title-marquee {
  display: block;
}

.chat-row-title-scrolling .conversation-title-marquee-content {
  animation: conversation-title-scroll 1.4s linear 0.1s infinite alternate;
}

@keyframes conversation-title-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(var(--title-scroll-distance)); }
}

.chat-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  width: 0;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.chat-row:hover .chat-row-actions,
.chat-row:focus-within .chat-row-actions {
  width: 55px;
  opacity: 1;
  pointer-events: auto;
}

.chat-row-action {
  height: 27px;
  width: 27px;
}

.conversation-options-menu {
  position: fixed;
  z-index: 91;
  width: 180px;
  border: 1px solid #dedede;
  border-radius: 12px;
  background: #ffffff;
  padding: 7px;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.14);
}

.search-dialog-backdrop {
  position: fixed;
  z-index: 200;
  inset: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: rgba(0, 0, 0, 0.34);
  padding: min(14vh, 150px) 20px 20px;
  backdrop-filter: blur(2px);
}

.search-dialog {
  width: min(100%, 800px);
  overflow: hidden;
  border: 1px solid #e2e2e2;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.23);
}

.search-dialog-input {
  display: flex;
  height: 72px;
  align-items: center;
  gap: 14px;
  padding: 0 19px;
  color: #333333;
}

.search-dialog-input input {
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  color: #242424;
  font-size: 17px;
  outline: none;
}

.search-dialog-input input::placeholder {
  color: #959595;
}

.search-dialog-input button {
  display: inline-flex;
  height: 34px;
  width: 34px;
  align-items: center;
  justify-content: center;
  border-left: 1px solid #eeeeee;
  color: #8b8b8b;
}

.search-dialog-input button:hover {
  color: #333333;
}

.search-results {
  max-height: min(62vh, 600px);
  overflow-y: auto;
  border-top: 1px solid #eeeeee;
  padding: 10px 9px 12px;
}

.search-result-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 14px;
  border-radius: 12px;
  padding: 11px 12px;
  color: #303030;
  transition: background-color 0.16s ease;
}

.search-result-item:hover {
  background: #f2f2f2;
}

.search-result-icon {
  display: inline-flex;
  height: 38px;
  width: 38px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e5e5;
  border-radius: 999px;
  color: #777777;
}

.search-result-item strong {
  font-size: 15px;
  font-weight: 500;
}

.search-result-item time {
  flex: 0 0 auto;
  color: #929292;
  font-size: 13px;
}

.search-result-preview {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #828282;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-loading {
  padding: 12px 0 3px;
  text-align: center;
  color: #a0a0a0;
  font-size: 13px;
}

.conversation-menu-option {
  display: flex;
  height: 38px;
  width: 100%;
  align-items: center;
  gap: 10px;
  border-radius: 8px;
  padding: 0 9px;
  text-align: left;
  font-size: 14px;
  color: #303030;
}

.conversation-menu-option:hover {
  background: #f2f2f2;
}

.conversation-menu-delete {
  color: #e53935;
}

.conversation-menu-delete:hover {
  background: #fff0f0;
  color: #d32f2f;
}

.rename-dialog-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.24);
  padding: 20px;
}

.rename-dialog {
  width: min(100%, 400px);
  border-radius: 14px;
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}

.rename-dialog h2 {
  margin: 0;
  color: #202124;
  font-size: 20px;
  font-weight: 700;
}

.rename-dialog input {
  height: 44px;
  width: 100%;
  margin-top: 18px;
  border: 1px solid #d7d7d7;
  border-radius: 9px;
  padding: 0 12px;
  color: #272727;
  font-size: 15px;
  outline: none;
}

.rename-dialog input:focus {
  border-color: #1681f8;
}

.rename-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.delete-dialog {
  width: min(100%, 650px);
  border: 1px solid #bcbcbc;
  border-radius: 18px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.18);
}

.delete-dialog h2 {
  margin: 0;
  color: #202124;
  font-size: 26px;
  font-weight: 700;
}

.delete-dialog p {
  margin: 24px 0 0;
  color: #303030;
  font-size: 18px;
}

.delete-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 28px;
}

.delete-dialog-cancel,
.delete-dialog-confirm {
  height: 48px;
  border-radius: 999px;
  padding: 0 22px;
  font-size: 16px;
  font-weight: 500;
}

.delete-dialog-cancel {
  border: 1px solid #dddddd;
  background: #ffffff;
  color: #333333;
}

.delete-dialog-cancel:hover {
  background: #f5f5f5;
}

.delete-dialog-confirm {
  background: #f9073e;
  color: #ffffff;
}

.delete-dialog-confirm:hover {
  background: #dc0034;
}

.sidebar-row {
  display: flex;
  height: 43px;
  width: 100%;
  align-items: center;
  gap: 11px;
  overflow: hidden;
  border-radius: 8px;
  padding: 0 10px;
  text-align: left;
  font-size: 16px;
  line-height: 1.25;
  color: #303030;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.sidebar-row:hover,
.sidebar-row-active {
  background: #f2f2f2;
  color: #161616;
}

.project-dialog-cancel,
.project-dialog-submit {
  height: 46px;
  border-radius: 13px;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 500;
}

.project-dialog-cancel {
  color: #858585;
}

.project-dialog-cancel:hover {
  background: #f1f1f1;
  color: #444444;
}

.project-dialog-submit {
  background: #202124;
  color: #ffffff;
}

.project-dialog-submit:hover:not(:disabled) {
  background: #3d3f43;
}

.project-dialog-submit:disabled {
  cursor: not-allowed;
  background: #c9c9c9;
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

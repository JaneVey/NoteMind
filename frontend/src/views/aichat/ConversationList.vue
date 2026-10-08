<template>
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
          <ConversationRow
            v-for="item in pinnedConversations"
            :key="item.id"
            :conversation="item"
            :active="activeConversationId === item.id"
            :pinned="true"
            @select="emit('select', $event)"
            @unpin="emit('unpin', $event)"
            @menu="openConversationMenu"
          />
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
          <ConversationRow
            v-for="item in filteredConversations"
            :key="item.id"
            :conversation="item"
            :active="activeConversationId === item.id"
            :draggable="true"
            @select="emit('select', $event)"
            @pin="emit('pin', $event)"
            @menu="openConversationMenu"
            @drag-start="draggedConversationId = $event"
            @drop-on="dropConversation($event)"
            @drag-end="draggedConversationId = null"
          />
          <p v-if="!filteredConversations.length" class="px-4 py-4 text-sm text-zinc-400">没有匹配的聊天</p>
        </template>
      </section>
    </div>
  </aside>

  <CollapsedSidebar
    v-else
    :conversations="filteredConversations"
    :pinned-conversations="pinnedConversations"
    @expand="openSidebar"
    @new-chat="startNewChat"
    @search="openSearchDialog"
    @select="emit('select', $event)"
    @pin="emit('pin', $event)"
    @unpin="emit('unpin', $event)"
    @menu="openConversationMenu"
  />

  <ConversationSearchDialog
    v-if="searchDialogVisible"
    :conversations="conversations"
    :pinned-conversations="pinnedConversations"
    @close="closeSearchDialog"
    @select="emit('select', $event)"
  />

  <ConversationOptionsMenu
    v-if="conversationMenuId !== null"
    :pinned="currentConversationIsPinned"
    :menu-style="conversationMenuStyle"
    @close="closeConversationMenu"
    @rename="openRenameDialog"
    @toggle-pin="toggleCurrentConversationPin"
    @remove="requestDeleteConversation"
  />

  <ConversationRenameDialog
    v-if="renameDialogVisible"
    :title="renameTargetTitle"
    @close="closeRenameDialog"
    @confirm="renameConversation"
  />

  <ConversationDeleteDialog
    v-if="deleteDialogVisible"
    :title="deleteTargetTitle"
    @close="closeDeleteDialog"
    @confirm="confirmDeleteConversation"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  ChevronDown,
  CirclePlus,
  PanelLeftClose,
  Search,
  Sparkles,
} from 'lucide-vue-next'
import type { Conversation } from '@/types/ai'
import type { Id } from '@/types/common'
import CollapsedSidebar from './CollapsedSidebar.vue'
import ConversationDeleteDialog from './ConversationDeleteDialog.vue'
import ConversationOptionsMenu from './ConversationOptionsMenu.vue'
import ConversationRenameDialog from './ConversationRenameDialog.vue'
import ConversationRow from './ConversationRow.vue'
import ConversationSearchDialog from './ConversationSearchDialog.vue'

type ConversationSummary = Pick<Conversation, 'id' | 'title'>

const props = defineProps<{
  conversations: ConversationSummary[]
  pinnedConversations: ConversationSummary[]
  activeConversationId: Id
}>()

const emit = defineEmits<{
  'new-chat': []
  select: [conversation: ConversationSummary]
  /** 仅切换高亮（右键菜单、置顶/取消置顶的原有副作用），不重置消息与输入框 */
  activate: [id: Id]
  pin: [conversation: ConversationSummary]
  unpin: [conversation: ConversationSummary]
  rename: [payload: { id: Id; title: string }]
  remove: [id: Id]
  reorder: [payload: { sourceId: Id; targetId: Id }]
}>()

const sidebarCollapsed = ref(false)
const pinnedExpanded = ref(true)
const chatsExpanded = ref(true)
const searchDialogVisible = ref(false)
const conversationMenuId = ref<Id | null>(null)
const conversationMenuStyle = ref<Record<string, string>>({})
const renameTargetId = ref<Id | null>(null)
const renameTargetTitle = ref('')
const renameDialogVisible = ref(false)
const deleteTargetId = ref<Id | null>(null)
const deleteTargetTitle = ref('')
const deleteDialogVisible = ref(false)
const draggedConversationId = ref<Id | null>(null)

const filteredConversations = computed(() => {
  return [...props.conversations]
})

const currentConversation = computed(() => props.conversations.find((item) => item.id === conversationMenuId.value)
  || props.pinnedConversations.find((item) => item.id === conversationMenuId.value)
  || null)

const currentConversationIsPinned = computed(() => props.pinnedConversations
  .some((item) => item.id === conversationMenuId.value))

function startNewChat(): void {
  emit('new-chat')
}

function openSidebar(): void {
  sidebarCollapsed.value = false
}

function openSearchDialog(): void {
  searchDialogVisible.value = true
}

function closeSearchDialog(): void {
  searchDialogVisible.value = false
}

function openConversationMenu(payload: { event: MouseEvent; id: Id }): void {
  const rect = (payload.event.currentTarget as HTMLElement).getBoundingClientRect()
  const menuHeight = 126
  conversationMenuStyle.value = window.innerHeight - rect.bottom < menuHeight
    ? { left: `${rect.left}px`, bottom: `${window.innerHeight - rect.top}px` }
    : { left: `${rect.left}px`, top: `${rect.bottom}px` }
  emit('activate', payload.id)
  conversationMenuId.value = payload.id
}

function closeConversationMenu(): void {
  conversationMenuId.value = null
}

function toggleCurrentConversationPin(): void {
  const conversation = currentConversation.value
  if (!conversation) return
  if (currentConversationIsPinned.value) emit('unpin', conversation)
  else emit('pin', conversation)
  closeConversationMenu()
}

function requestDeleteConversation(): void {
  const conversation = currentConversation.value
  if (!conversation) return
  deleteTargetId.value = conversation.id
  deleteTargetTitle.value = conversation.title
  deleteDialogVisible.value = true
  closeConversationMenu()
}

function closeDeleteDialog(): void {
  deleteDialogVisible.value = false
  deleteTargetId.value = null
  deleteTargetTitle.value = ''
}

function confirmDeleteConversation(): void {
  if (deleteTargetId.value !== null) emit('remove', deleteTargetId.value)
  closeDeleteDialog()
}

function openRenameDialog(): void {
  const conversation = currentConversation.value
  if (!conversation) return
  renameTargetId.value = conversation.id
  renameTargetTitle.value = conversation.title
  renameDialogVisible.value = true
  closeConversationMenu()
}

function closeRenameDialog(): void {
  renameDialogVisible.value = false
  renameTargetId.value = null
  renameTargetTitle.value = ''
}

function renameConversation(title: string): void {
  if (renameTargetId.value !== null) emit('rename', { id: renameTargetId.value, title })
  closeRenameDialog()
}

function dropConversation(targetId: Id): void {
  const sourceId = draggedConversationId.value
  if (!sourceId || sourceId === targetId) return
  emit('reorder', { sourceId, targetId })
  draggedConversationId.value = null
}
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
</style>

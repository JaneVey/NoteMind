<template>
  <div
    class="notebook-shell flex h-full min-w-[980px] overflow-hidden bg-white text-zinc-800"
    :class="isSidePanelResizing || isRightPanelResizing ? 'select-none' : ''"
  >
    <NotebookSidebar
      v-model:visible="sidePanelVisible"
      :width="sidePanelWidth"
      :notes="notes"
      :folders="folders"
      :active-note-id="activeNoteId"
      :active-note-folder-id="activeNote.folderId"
      @resize-start="startSidePanelResize"
      @select-note="selectNote"
      @open-search-result="openSearchResult"
      @create-note="createPrototypeNote"
    />

    <NoteEditor
      v-model:title="activeNote.title"
      v-model:content="noteContent"
      v-model:view-mode="viewMode"
      :note-id="activeNoteId"
      :right-panel-visible="rightPanelVisible"
      :wiki-link-target-title="wikiLinkTargetTitle"
      @change="touchActiveNote"
      @update:right-panel-visible="rightPanelVisible = $event"
    />

    <NoteAiPanel
      v-model:visible="rightPanelVisible"
      :width="rightPanelWidth"
      :note-title="activeNote.title"
      :backlink-notes="backlinkNotes"
      :outgoing-notes="outgoingNotes"
      @resize-start="startRightPanelResize"
      @select-note="selectNote"
    />

    <footer class="note-status-bar">
      <span>{{ backlinkCount }} 条反向链接</span>
      <div ref="statusModeMenuRef" class="relative">
        <button class="status-mode-button" title="切换视图模式" @click="toggleStatusModeMenu"><PenLine v-if="viewMode === 'writing'" class="h-4 w-4" /><Code2 v-else-if="viewMode === 'source'" class="h-4 w-4" /><Eye v-else class="h-4 w-4" /></button>
        <div v-if="showStatusModeMenu" class="status-mode-menu">
          <button :class="viewMode === 'reading' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('reading')"><Eye class="h-4 w-4" />阅读视图<Check v-if="viewMode === 'reading'" class="ml-auto h-4 w-4" /></button>
          <button :class="viewMode === 'source' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('source')"><Code2 class="h-4 w-4" />源码模式<Check v-if="viewMode === 'source'" class="ml-auto h-4 w-4" /></button>
          <button :class="viewMode === 'writing' ? 'status-mode-menu-item-active' : ''" @click="selectStatusViewMode('writing')"><PenLine class="h-4 w-4" />实时阅览<Check v-if="viewMode === 'writing'" class="ml-auto h-4 w-4" /></button>
        </div>
      </div>
      <span>{{ wordCount }} 个词</span>
      <span>{{ noteContent.length }} 个字符</span>
      <span class="status-sync" :class="isSynced ? 'status-synced' : 'status-saving'" :title="isSynced ? '已保存' : '正在保存'">
        <Save v-if="isSynced" class="h-4 w-4" />
        <CloudUpload v-else class="h-4 w-4" />
      </span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
// 修复记录（2026-10-08）：原文件漏了 Check 的 import，导致底部状态栏「视图模式」
// 菜单里 3 处对勾从不显示。结构重构时为保证 DOM 逐字节一致而故意保留，
// 已在本提交中补齐。
import { Check, CloudUpload, Code2, Eye, PenLine, Save } from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import type { Id } from '@/types/common'
import NoteAiPanel from './notebook/NoteAiPanel.vue'
import NoteEditor from './notebook/NoteEditor.vue'
import NotebookSidebar from './notebook/NotebookSidebar.vue'
import type { NotebookFolder, NotebookNote, ViewMode } from './notebook/types'

/**
 * 笔记本页容器：左侧栏 + 编辑器 + AI 面板 + 底部状态栏。
 *
 * <p>本组件只负责**数据与状态编排**（笔记数据、当前笔记、视图模式、面板宽度与显隐、
 * 未保存标记与防抖保存），交互与渲染都下沉到 `views/notebook/` 下的子组件，
 * 通过 props / emits 通信。各子组件内部的下拉菜单统一走 `useDropdown`。
 */

const SIDE_PANEL_MIN_WIDTH = 248
const SIDE_PANEL_MAX_WIDTH = 430
const RIGHT_PANEL_MIN_WIDTH = 248
const RIGHT_PANEL_MAX_WIDTH = 430

const sidePanelVisible = ref(true)
const sidePanelWidth = ref(318)
const rightPanelVisible = ref(true)
const rightPanelWidth = ref(320)
const isSidePanelResizing = ref(false)
const isRightPanelResizing = ref(false)
const viewMode = ref<ViewMode>('writing')
const activeNoteId = ref<Id>('architecture')
const isSynced = ref(true)
let syncTimer: ReturnType<typeof setTimeout> | null = null

// 底部状态栏的视图模式菜单
const statusModeMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showStatusModeMenu, toggle: toggleStatusModeMenu, close: closeStatusModeMenu } = useDropdown(statusModeMenuRef)

const folders: NotebookFolder[] = [
  { id: 'plan', name: '开发计划' },
  { id: 'design', name: '系统设计' },
  { id: 'rag', name: 'RAG 资料' },
]

const notes = ref<NotebookNote[]>([
  {
    id: 'architecture', title: 'NoteMind 系统架构', folderId: 'design', updatedLabel: '43 分钟前', recentGroup: '今天', updatedAt: 5, createdAt: 2,
    content: '# NoteMind 系统架构\n\n## 核心定位\n\nNoteMind 是一个基于 RAG 的智能知识管理平台。笔记负责知识创造和沉淀，知识库负责检索，AI 则辅助整理和生成。\n\n## 技术选型\n\n- 前端：Vue 3 + Vite + Tailwind CSS\n- 后端：Spring Boot 3 + Spring AI\n- 数据：PostgreSQL + pgvector\n- 文件：MinIO\n\n## 核心链路\n\n用户在低门槛编辑器中创作内容，底层保存 Markdown。具体表结构见 [[数据库设计文档]]，笔记编辑体验见 [[笔记编辑器方案]]。笔记可以按需加入知识库，经过分块和向量化后，为 RAG 问答提供可靠上下文。',
  },
  {
    id: 'database', title: '数据库设计文档', folderId: 'design', updatedLabel: '今天 09:18', recentGroup: '今天', updatedAt: 4, createdAt: 5,
    content: '# 数据库设计文档\n\n## 笔记表\n\n笔记保存标题、Markdown 内容、所属文件夹和更新时间。\n\n> Markdown 是唯一内容来源，HTML 只作为渲染结果。\n\n## 关联关系\n\n使用 `[[笔记标题]]` 建立双向链接，并由后端解析维护关系图谱。',
  },
  {
    id: 'editor', title: '笔记编辑器方案', folderId: 'plan', updatedLabel: '昨天', recentGroup: '过去 7 天', updatedAt: 3, createdAt: 4,
    content: '# 笔记编辑器方案\n\n默认使用即时渲染编辑模式，让不熟悉 Markdown 的用户也能通过工具栏完成排版。\n\n需要时可以切换到源码模式，直接编辑原始 Markdown。',
  },
  {
    id: 'rag-chunk', title: 'RAG 文档分块策略', folderId: 'rag', updatedLabel: '3 天前', recentGroup: '过去 7 天', updatedAt: 2, createdAt: 3,
    content: '# RAG 文档分块策略\n\n优先按 Markdown 标题与段落切分，每个知识块保留标题路径和来源信息。\n\n- 块大小：512 tokens\n- 重叠：80 tokens\n- 支持引用溯源',
  },
  {
    id: 'welcome', title: '欢迎使用 NoteMind', folderId: null, updatedLabel: '7 月 21 日', recentGroup: '过去 30 天', updatedAt: 1, createdAt: 1,
    content: '# 欢迎使用 NoteMind\n\n从一篇笔记开始，逐步建立自己的知识系统。',
  },
])

const activeNote = computed(() => notes.value.find((note) => note.id === activeNoteId.value) || notes.value[0])
const noteContent = ref(activeNote.value.content)
const outgoingNotes = computed(() => linkedNotesFor(activeNote.value))
const backlinkNotes = computed(() => notes.value.filter((note) => note.id !== activeNote.value.id && note.content.includes(`[[${activeNote.value.title}]]`)))
const wikiLinkTargetTitle = computed(() => notes.value.find((note) => note.id !== activeNoteId.value)?.title ?? null)
const wordCount = computed(() => {
  const text = noteContent.value
  const chineseCount = (text.match(/[\u4e00-\u9fff]/g) || []).length
  const otherWords = text.replace(/[\u4e00-\u9fff]/g, ' ').match(/[A-Za-z0-9_]+/g) || []
  return chineseCount + otherWords.length
})
const backlinkCount = computed(() => backlinkNotes.value.length)

function linkedNotesFor(note: NotebookNote): NotebookNote[] {
  const titles = [...note.content.matchAll(/\[\[([^\]]+)\]\]/g)].map((match) => match[1])
  return titles
    .map((title) => notes.value.find((candidate) => candidate.title === title))
    .filter((candidate): candidate is NotebookNote => Boolean(candidate))
    .filter((candidate, index, list) => list.findIndex((item) => item.id === candidate.id) === index)
}

function selectNote(noteId: Id): void {
  activeNoteId.value = noteId
  noteContent.value = activeNote.value.content
}

function openSearchResult(noteId: Id): void {
  selectNote(noteId)
  viewMode.value = 'writing'
}

function selectStatusViewMode(mode: ViewMode): void {
  viewMode.value = mode
  closeStatusModeMenu()
}

function createPrototypeNote(): void {
  const id = `note-${Date.now()}`
  notes.value.unshift({
    id, title: '无标题笔记', content: '', folderId: null, updatedLabel: '刚刚', recentGroup: '今天', createdAt: Date.now(), updatedAt: Date.now(),
  })
  selectNote(id)
}

function touchActiveNote(): void {
  activeNote.value.content = noteContent.value
  activeNote.value.updatedLabel = '刚刚'
  activeNote.value.recentGroup = '今天'
  activeNote.value.updatedAt = Date.now()
  isSynced.value = false
  if (syncTimer) clearTimeout(syncTimer)
  syncTimer = setTimeout(() => { isSynced.value = true }, 700)
}

let stopSideResize: (() => void) | null = null
let stopRightResize: (() => void) | null = null

function startSidePanelResize(event: MouseEvent): void {
  isSidePanelResizing.value = true
  const startX = event.clientX
  const startWidth = sidePanelWidth.value
  const onMove = (moveEvent: MouseEvent) => {
    sidePanelWidth.value = Math.min(SIDE_PANEL_MAX_WIDTH, Math.max(SIDE_PANEL_MIN_WIDTH, startWidth + moveEvent.clientX - startX))
  }
  const stop = () => {
    isSidePanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stop)
    stopSideResize = null
  }
  stopSideResize = stop
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stop)
}

function startRightPanelResize(event: MouseEvent): void {
  isRightPanelResizing.value = true
  const startX = event.clientX
  const startWidth = rightPanelWidth.value
  const onMove = (moveEvent: MouseEvent) => {
    rightPanelWidth.value = Math.min(RIGHT_PANEL_MAX_WIDTH, Math.max(RIGHT_PANEL_MIN_WIDTH, startWidth - (moveEvent.clientX - startX)))
  }
  const stop = () => {
    isRightPanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stop)
    stopRightResize = null
  }
  stopRightResize = stop
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stop)
}

// 切换笔记时把正文同步到编辑器；富文本 DOM 的重载由 NoteEditor 监听 noteId 完成
watch(activeNoteId, () => {
  noteContent.value = activeNote.value.content
})

onBeforeUnmount(() => {
  stopSideResize?.()
  stopRightResize?.()
  if (syncTimer) clearTimeout(syncTimer)
})
</script>

<style scoped>
.notebook-shell { position: relative; --mint: #e8f5ee; --mint-strong: #3c9270; }

.note-status-bar { position: absolute; z-index: 70; right: 0; bottom: 0; display: flex; height: 31px; align-items: center; gap: 10px; border-top: 1px solid #e4e4e7; border-left: 1px solid #e4e4e7; border-radius: 9px 0 0 0; background: rgba(255, 255, 255, .96); padding: 0 11px 0 14px; color: #71717a; font-size: 12px; box-shadow: -5px -4px 16px rgba(24, 24, 27, .04); }
.status-mode-button { display: inline-flex; height: 24px; width: 24px; align-items: center; justify-content: center; border-radius: 5px; color: #71717a; }.status-mode-button:hover { background: #f0f0ee; color: #27272a; }
.status-mode-menu { position: absolute; right: -3px; bottom: calc(100% + 7px); width: 164px; overflow: hidden; border: 1px solid #e2e2e2; border-radius: 10px; background: #fff; padding: 5px; box-shadow: 0 10px 26px rgba(24, 24, 27, .14); }.status-mode-menu button { display: flex; width: 100%; align-items: center; gap: 9px; border-radius: 6px; padding: 8px; text-align: left; color: #3f3f46; font-size: 13px; }.status-mode-menu button:hover, .status-mode-menu-item-active { background: #e8f5ee; color: #247456; }
.status-sync { display: inline-flex; align-items: center; }.status-synced { color: #3c9270; }.status-saving { color: #d98b32; }
</style>

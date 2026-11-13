<template>
  <div
    class="notebook-shell flex h-full min-w-[980px] overflow-hidden bg-white text-zinc-800"
    :class="isSidePanelResizing || isRightPanelResizing ? 'select-none' : ''"
  >
    <NotebookSidebar
      v-model:visible="sidePanelVisible"
      :width="sidePanelWidth"
      :notes="navNotes"
      :folders="navFolders"
      :notebooks="noteStore.notebooks"
      :current-notebook-id="noteStore.currentNotebookId"
      :active-note-id="activeNoteId ?? ''"
      :active-note-folder-id="activeNoteFolderId"
      :selected-folder-id="selectedFolderId"
      @resize-start="startSidePanelResize"
      @select-note="selectNote"
      @open-search-result="openSearchResult"
      @create-note="createNote"
      @create-folder="openCreateFolder"
      @select-folder="selectFolder"
      @drop-note="handleDropNote"
      @drop-folder="handleDropFolder"
      @note-action="handleNoteAction"
      @folder-action="handleFolderAction"
      @select-notebook="switchNotebook"
      @manage-notebooks="openNotebookManager"
    />

    <div class="flex min-h-0 min-w-0 flex-1 flex-col">
      <NotebookTabs
        :tabs="openTabs"
        :active-id="activeTabId"
        :right-panel-visible="rightPanelVisible"
        @select="switchTab"
        @close="closeTabById"
        @reorder="reorderTabs"
        @create="openNewTab"
        @close-all="closeAllTabs"
        @expand-right="rightPanelVisible = true"
      />

      <NewTabPage
        v-if="activeNewTab"
        @create="createNote"
        @open-picker="showQuickSwitcher = true"
      />

      <NoteEditor
        v-else
        v-model:title="noteTitle"
        v-model:content="noteContent"
        v-model:view-mode="viewMode"
        :note-id="activeNoteId ?? ''"
        :wiki-link-target-title="wikiLinkTargetTitle"
        @change="touchActiveNote"
      />
    </div>

    <NoteAiPanel
      v-model:visible="rightPanelVisible"
      :width="rightPanelWidth"
      :note-title="noteTitle"
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
      <span class="status-sync" :class="isSynced ? 'status-synced' : 'status-saving'" :title="saveError ?? (isSynced ? '已保存' : '正在保存')">
        <Save v-if="isSynced" class="h-4 w-4" />
        <CloudUpload v-else class="h-4 w-4" />
      </span>
    </footer>
  </div>

  <QuickSwitcher
    v-model:visible="showQuickSwitcher"
    :notes="navNotes"
    @open="handleQuickOpen"
    @create="handleQuickCreate"
  />

  <MoveNoteDialog
    v-model:visible="showMoveFolderDialog"
    :folders="folderMoveOptions"
    :current-folder-id="movingFolder?.parentId ?? null"
    @confirm="confirmMoveFolder"
  />

  <MoveNoteDialog
    v-model:visible="showMoveDialog"
    :folders="moveFolderOptions"
    :current-folder-id="movingNote?.folderId ?? null"
    @confirm="confirmMoveNote"
  />

  <NotebookCreateDialog
    v-model:visible="showCreateNotebookDialog"
    :submitting="creatingNotebook"
    @submit="handleCreateNotebook"
  />

  <div v-if="showNotebookManager" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/20" @click.self="showNotebookManager = false">
    <div class="flex h-[440px] w-[680px] overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-2xl">
      <aside class="flex w-[240px] shrink-0 flex-col border-r border-zinc-200 bg-[#f7f7f6]">
        <div class="shrink-0 px-4 pb-3 pt-5">
          <h2 class="text-[15px] font-semibold text-zinc-900">管理笔记本</h2>
          <p class="mt-0.5 text-xs text-zinc-400">双击切换，选中后可管理</p>
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto px-2 pb-2">
          <button
            v-for="notebook in noteStore.notebooks"
            :key="notebook.id"
            class="manager-notebook-row"
            :class="notebook.id === selectedManagerNotebookId ? 'manager-notebook-row-active' : ''"
            @click="selectedManagerNotebookId = notebook.id"
            @dblclick="openNotebookNow(notebook)"
          >
            <span class="flex h-4 w-4 shrink-0 items-center justify-center overflow-hidden rounded">
              <NotebookGlyph :icon="notebook.icon" />
            </span>
            <span class="truncate">{{ notebook.name }}</span>
            <span v-if="notebook.id === noteStore.currentNotebookId" class="manager-badge">当前打开</span>
          </button>
        </div>
        <div class="shrink-0 border-t border-zinc-200 p-2">
          <button class="manager-new-button" @click="openCreateNotebook">
            <Plus class="h-4 w-4" />新建笔记本
          </button>
        </div>
      </aside>

      <section class="relative flex min-w-0 flex-1 flex-col bg-white">
        <button class="manager-icon-button absolute right-3 top-3" title="关闭" @click="showNotebookManager = false">
          <X class="h-5 w-5" />
        </button>

        <div class="min-h-0 flex-1 overflow-y-auto px-6 pb-6 pt-5">
          <template v-if="selectedManagerNotebook">
            <div class="flex items-center gap-3 pr-10">
              <div class="manager-avatar">
                <NotebookGlyph :icon="selectedManagerNotebook.icon" size="lg" />
              </div>
              <div class="min-w-0 flex-1">
                <h3 class="truncate text-lg font-semibold text-zinc-900">{{ selectedManagerNotebook.name }}</h3>
                <p class="text-xs text-zinc-400">创建于 {{ formatDate(selectedManagerNotebook.createdAt) }}</p>
              </div>
              <div class="flex shrink-0 items-center gap-1">
                <button
                  v-if="selectedManagerNotebook.id !== noteStore.currentNotebookId"
                  class="manager-icon-button"
                  title="打开"
                  @click="openSelectedNotebook"
                ><ArrowUpRight class="h-[18px] w-[18px]" /></button>
                <button class="manager-icon-button" title="重命名" @click="openRenameNotebook(selectedManagerNotebook)">
                  <Pencil class="h-[18px] w-[18px]" />
                </button>
                <button class="manager-icon-button manager-icon-danger" title="删除" @click="removeNotebook(selectedManagerNotebook)">
                  <Trash2 class="h-[18px] w-[18px]" />
                </button>
              </div>
            </div>

            <button class="manager-description group" @click="openEditDescription(selectedManagerNotebook)">
              <span class="min-w-0 flex-1 text-left" :class="selectedManagerNotebook.description ? 'text-zinc-600' : 'text-zinc-400'">
                {{ selectedManagerNotebook.description || '添加描述…' }}
              </span>
              <Pencil class="h-3.5 w-3.5 shrink-0 text-zinc-400 opacity-0 transition-opacity group-hover:opacity-100" />
            </button>
          </template>
          <div v-else class="flex h-full flex-col items-center justify-center gap-2 text-sm text-zinc-400">
            <NotebookIcon class="h-8 w-8 text-zinc-300" />
            选择左侧的笔记本
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  ArrowUpRight, Check, CloudUpload, Code2, Eye, Notebook as NotebookIcon, PenLine, Pencil,
  Plus, Save, Trash2, X,
} from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import { useConfirm } from '@/composables/useConfirm'
import { useInputDialog } from '@/composables/useInputDialog'
import { useToast } from '@/composables/useToast'
import { useNotebookSidebar } from '@/composables/useNotebookSidebar'
import { useAuthStore } from '@/stores/authStore'
import { useNoteStore } from '@/stores/noteStore'
import type { Id } from '@/types/common'
import type { NoteListItem, NoteRef, Notebook } from '@/types/note'
import { formatDate, formatRelativeTime } from '@/utils/format'
import NoteAiPanel from './notebook/NoteAiPanel.vue'
import NoteEditor from './notebook/NoteEditor.vue'
import MoveNoteDialog from './notebook/MoveNoteDialog.vue'
import NotebookCreateDialog from './notebook/NotebookCreateDialog.vue'
import NotebookGlyph from './notebook/NotebookGlyph.vue'
import NotebookSidebar from './notebook/NotebookSidebar.vue'
import NotebookTabs from './notebook/NotebookTabs.vue'
import NewTabPage from './notebook/NewTabPage.vue'
import QuickSwitcher from './notebook/QuickSwitcher.vue'
import type { ItemAction, MoveFolderOption, NoteTab } from './notebook/types'
import type {
  NotebookFolder, NotebookNote, RecentGroupLabel, ViewMode,
} from './notebook/types'

/**
 * 笔记本页容器：左侧栏 + 编辑器 + AI 面板 + 底部状态栏。
 *
 * <p>笔记与文件夹来自 `noteStore`，这里做一次 `NoteListItem → NotebookNote` 的派生，
 * 让侧栏三个子组件不必关心后端字段名（`contentMd` / ISO 时间字符串）。
 * 标题与正文用本地 ref 承接输入，防抖后写回后端。
 */

const SIDE_PANEL_MIN_WIDTH = 248
const SIDE_PANEL_MAX_WIDTH = 430
const RIGHT_PANEL_MIN_WIDTH = 248
const RIGHT_PANEL_MAX_WIDTH = 430
const SAVE_DEBOUNCE_MS = 800

const authStore = useAuthStore()
const noteStore = useNoteStore()
const toast = useToast()
const { confirm } = useConfirm()

const { collapsed: sidebarCollapsed } = useNotebookSidebar()
const sidePanelVisible = computed({
  get: () => !sidebarCollapsed.value,
  set: (visible: boolean) => { sidebarCollapsed.value = !visible },
})
const sidePanelWidth = ref(318)
const rightPanelVisible = ref(true)
const rightPanelWidth = ref(320)
const isSidePanelResizing = ref(false)
const isRightPanelResizing = ref(false)
const viewMode = ref<ViewMode>('writing')
const activeNoteId = ref<Id | null>(null)
/** undefined 表示没有选中任何文件夹；null 表示选中了「未分类」根层 */
const selectedFolderId = ref<Id | null | undefined>(undefined)
const noteTitle = ref('')
const noteContent = ref('')
const isSynced = ref(true)
const saveError = ref<string | null>(null)

let syncTimer: ReturnType<typeof setTimeout> | null = null
let saveInFlight = false
let pendingSave = false
let saveFailureNotified = false

const statusModeMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showStatusModeMenu, toggle: toggleStatusModeMenu, close: closeStatusModeMenu } = useDropdown(statusModeMenuRef)

const navNotes = computed<NotebookNote[]>(() => noteStore.notes.map(toNavNote))
const NEW_TAB_ID = '__new__'
const openTabIds = ref<Id[]>([])
const newTabOpen = ref(false)
const activeNewTab = ref(false)
const activeTabId = computed<Id | null>(() => (activeNewTab.value ? NEW_TAB_ID : activeNoteId.value))
const openTabs = computed<NoteTab[]>(() => {
  const tabs: NoteTab[] = openTabIds.value.map((id) => ({
    id,
    title: navNotes.value.find((note) => note.id === id)?.title ?? '无标题笔记',
  }))
  if (newTabOpen.value) {
    tabs.push({ id: NEW_TAB_ID, title: '新标签页' })
  }
  return tabs
})
const navFolders = computed<NotebookFolder[]>(
  () => noteStore.folders.map((folder) => ({
    id: folder.id,
    name: folder.name,
    parentId: folder.parentId,
  })),
)
const activeNavNote = computed(
  () => navNotes.value.find((note) => note.id === activeNoteId.value) ?? null,
)
const activeNoteFolderId = computed(() => activeNavNote.value?.folderId ?? null)
const backlinkNotes = computed<NoteRef[]>(() => noteStore.noteBacklinks)
const outgoingNotes = computed<NoteRef[]>(() => noteStore.noteLinks)
const wikiLinkTargetTitle = computed(
  () => navNotes.value.find((note) => note.id !== activeNoteId.value)?.title ?? null,
)
const backlinkCount = computed(() => backlinkNotes.value.length)
const wordCount = computed(() => {
  const text = noteContent.value
  const chineseCount = (text.match(/[一-鿿]/g) || []).length
  const otherWords = text.replace(/[一-鿿]/g, ' ').match(/[A-Za-z0-9_]+/g) || []
  return chineseCount + otherWords.length
})

function toNavNote(item: NoteListItem): NotebookNote {
  return {
    id: item.id,
    title: item.title,
    content: item.excerpt ?? '',
    folderId: item.folderId,
    updatedLabel: formatRelativeTime(item.updatedAt),
    recentGroup: recentGroupOf(item.updatedAt),
    updatedAt: new Date(item.updatedAt).getTime(),
    createdAt: new Date(item.createdAt).getTime(),
  }
}

function recentGroupOf(iso: string): RecentGroupLabel {
  const days = (Date.now() - new Date(iso).getTime()) / 86_400_000
  if (days < 1) return '今天'
  if (days < 7) return '过去 7 天'
  return '过去 30 天'
}

async function selectNote(noteId: Id): Promise<void> {
  if (noteId === activeNoteId.value && !activeNewTab.value) return
  selectedFolderId.value = undefined
  await flushSave()
  activeNewTab.value = false
  newTabOpen.value = false
  const note = await noteStore.openNote(noteId)
  if (!note) return
  openTab(note.id)
  activeNoteId.value = note.id
  noteTitle.value = note.title
  noteContent.value = note.contentMd
  isSynced.value = true
  saveError.value = null
}

async function openSearchResult(noteId: Id): Promise<void> {
  await selectNote(noteId)
  viewMode.value = 'writing'
}

async function createNote(): Promise<void> {
  await createNoteWithTitle('无标题笔记')
}

async function createNoteWithTitle(title: string): Promise<void> {
  await flushSave()
  activeNewTab.value = false
  newTabOpen.value = false
  const note = await noteStore.createNote(selectedFolderId.value ?? null, title)
  if (!note) {
    toast.error(noteStore.error ?? '创建笔记失败')
    return
  }
  openTab(note.id)
  activeNoteId.value = note.id
  noteTitle.value = note.title
  noteContent.value = note.contentMd
  isSynced.value = true
  saveError.value = null
}

function handleQuickOpen(noteId: Id): void {
  showQuickSwitcher.value = false
  void selectNote(noteId)
}

function handleQuickCreate(title: string): void {
  showQuickSwitcher.value = false
  void createNoteWithTitle(title)
}

function openTab(noteId: Id): void {
  if (!openTabIds.value.includes(noteId)) {
    openTabIds.value.push(noteId)
  }
}

async function closeTab(noteId: Id): Promise<void> {
  const index = openTabIds.value.indexOf(noteId)
  if (index < 0) return

  const wasActive = !activeNewTab.value && activeNoteId.value === noteId
  if (wasActive) await flushSave()
  openTabIds.value = openTabIds.value.filter((id) => id !== noteId)
  if (!wasActive) return

  const next = openTabIds.value[Math.min(index, openTabIds.value.length - 1)] ?? null
  if (next !== null) {
    await selectNote(next)
  } else {
    resetActiveNote()
  }
}

async function openNewTab(): Promise<void> {
  if (activeNewTab.value) return
  await flushSave()
  newTabOpen.value = true
  activeNewTab.value = true
}

async function closeNewTab(): Promise<void> {
  if (!newTabOpen.value) return
  newTabOpen.value = false
  if (!activeNewTab.value) return

  activeNewTab.value = false
  const last = openTabIds.value[openTabIds.value.length - 1] ?? null
  if (last !== null) {
    await selectNote(last)
  } else {
    resetActiveNote()
  }
}

function switchTab(tabId: Id): void {
  if (tabId === NEW_TAB_ID) {
    void openNewTab()
    return
  }
  void selectNote(tabId)
}

function closeTabById(tabId: Id): void {
  if (tabId === NEW_TAB_ID) {
    void closeNewTab()
    return
  }
  void closeTab(tabId)
}

function reorderTabs(noteIds: Id[]): void {
  openTabIds.value = noteIds
}

async function closeAllTabs(): Promise<void> {
  await flushSave()
  openTabIds.value = []
  newTabOpen.value = false
  activeNewTab.value = false
  resetActiveNote()
}

function selectStatusViewMode(mode: ViewMode): void {
  viewMode.value = mode
  closeStatusModeMenu()
}

function touchActiveNote(): void {
  if (activeNoteId.value === null) return
  isSynced.value = false
  if (syncTimer) clearTimeout(syncTimer)
  syncTimer = setTimeout(() => { void flushSave() }, SAVE_DEBOUNCE_MS)
}

async function flushSave(): Promise<void> {
  if (syncTimer) {
    clearTimeout(syncTimer)
    syncTimer = null
  }
  const noteId = activeNoteId.value
  if (noteId === null) return
  if (saveInFlight) {
    pendingSave = true
    return
  }
  saveInFlight = true
  try {
    const ok = await noteStore.saveNote(noteId, {
      title: noteTitle.value,
      contentMd: noteContent.value,
    })
    isSynced.value = ok
    saveError.value = ok ? null : noteStore.error
    if (ok) {
      saveFailureNotified = false
    } else if (!saveFailureNotified) {
      saveFailureNotified = true
      toast.error(noteStore.error ?? '保存失败')
    }
  } finally {
    saveInFlight = false
    if (pendingSave) {
      pendingSave = false
      void flushSave()
    }
  }
}

const showNotebookManager = ref(false)
const showCreateNotebookDialog = ref(false)
const creatingNotebook = ref(false)
const showMoveDialog = ref(false)
const showQuickSwitcher = ref(false)
const movingNoteId = ref<Id | null>(null)
const showMoveFolderDialog = ref(false)
const movingFolderId = ref<Id | null>(null)
const movingNote = computed(
  () => noteStore.notes.find((note) => note.id === movingNoteId.value) ?? null,
)
const movingFolder = computed(
  () => noteStore.folders.find((folder) => folder.id === movingFolderId.value) ?? null,
)
const allFolderOptions = computed<MoveFolderOption[]>(() => {
  const byParent = new Map<Id | null, typeof noteStore.folders>()
  for (const folder of noteStore.folders) {
    const key = folder.parentId ?? null
    const list = byParent.get(key) ?? []
    list.push(folder)
    byParent.set(key, list)
  }
  const options: MoveFolderOption[] = [{ id: null, path: '未分类（根目录）' }]
  const walk = (parentId: Id | null, prefix: string): void => {
    for (const folder of byParent.get(parentId) ?? []) {
      const path = prefix ? `${prefix} / ${folder.name}` : folder.name
      options.push({ id: folder.id, path })
      walk(folder.id, path)
    }
  }
  walk(null, '')
  return options
})
const moveFolderOptions = computed<MoveFolderOption[]>(() => allFolderOptions.value)
const folderMoveOptions = computed<MoveFolderOption[]>(() => {
  const movingId = movingFolderId.value
  if (movingId === null) return allFolderOptions.value

  const excluded = new Set<Id>([movingId])
  let changed = true
  while (changed) {
    changed = false
    for (const folder of noteStore.folders) {
      if (folder.parentId !== null && excluded.has(folder.parentId) && !excluded.has(folder.id)) {
        excluded.add(folder.id)
        changed = true
      }
    }
  }
  return allFolderOptions.value.filter((option) => option.id === null || !excluded.has(option.id))
})
const selectedManagerNotebookId = ref<Id | null>(null)
const selectedManagerNotebook = computed(
  () => noteStore.notebooks.find((notebook) => notebook.id === selectedManagerNotebookId.value) ?? null,
)

function openNotebookManager(): void {
  selectedManagerNotebookId.value = noteStore.currentNotebookId
  showNotebookManager.value = true
}

async function openSelectedNotebook(): Promise<void> {
  const notebook = selectedManagerNotebook.value
  if (!notebook) return
  showNotebookManager.value = false
  await switchNotebook(notebook.id)
}

async function openNotebookNow(notebook: Notebook): Promise<void> {
  showNotebookManager.value = false
  if (notebook.id === noteStore.currentNotebookId) return
  await switchNotebook(notebook.id)
}

const inputDialog = useInputDialog()

function resetActiveNote(): void {
  activeNoteId.value = null
  noteTitle.value = ''
  noteContent.value = ''
  isSynced.value = true
}

async function selectFirstNote(): Promise<void> {
  const first = noteStore.notes[0]
  if (first) {
    await selectNote(first.id)
  } else {
    resetActiveNote()
  }
}

async function switchNotebook(notebookId: Id): Promise<void> {
  await flushSave()
  await noteStore.selectNotebook(notebookId)
  openTabIds.value = []
  newTabOpen.value = false
  activeNewTab.value = false
  selectedFolderId.value = undefined
  resetActiveNote()
  await selectFirstNote()
}

function openCreateNotebook(): void {
  showCreateNotebookDialog.value = true
}

async function handleCreateNotebook(payload: { name: string; icon: string }): Promise<void> {
  creatingNotebook.value = true
  try {
    const notebook = await noteStore.createNotebook(payload)
    if (!notebook) {
      toast.error(noteStore.error ?? '创建笔记本失败')
      return
    }
    toast.success('笔记本已创建')
    showCreateNotebookDialog.value = false
    selectedManagerNotebookId.value = notebook.id
    resetActiveNote()
    await selectFirstNote()
  } finally {
    creatingNotebook.value = false
  }
}

async function openRenameNotebook(notebook: Notebook): Promise<void> {
  const name = await inputDialog.open({
    title: '重命名笔记本',
    placeholder: '笔记本名称',
    initialValue: notebook.name,
  })
  if (name === null) return
  const ok = await noteStore.updateNotebook(notebook.id, { name })
  if (!ok) {
    toast.error(noteStore.error ?? '重命名失败')
    return
  }
  toast.success('笔记本已重命名')
}

async function openEditDescription(notebook: Notebook): Promise<void> {
  const description = await inputDialog.open({
    title: '编辑描述',
    placeholder: '补充这个笔记本用来做什么',
    multiline: true,
    allowEmpty: true,
    initialValue: notebook.description ?? '',
  })
  if (description === null) return
  const ok = await noteStore.updateNotebook(notebook.id, { description })
  if (!ok) {
    toast.error(noteStore.error ?? '保存描述失败')
    return
  }
  toast.success('描述已保存')
}

async function removeNotebook(notebook: Notebook): Promise<void> {
  const confirmed = await confirm({
    title: '删除笔记本',
    message: `确定删除「${notebook.name}」？`,
    confirmText: '删除',
    danger: true,
  })
  if (!confirmed) return

  const wasCurrent = noteStore.currentNotebookId === notebook.id
  let ok = await noteStore.deleteNotebook(notebook.id)
  if (!ok && noteStore.errorCode === 3004) {
    const forceConfirmed = await confirm({
      title: '删除全部内容',
      message: `「${notebook.name}」下还有笔记或文件夹。继续删除会连同里面的全部笔记、文件夹与链接一起删除。`,
      confirmText: '我确认删除全部',
      danger: true,
      severe: true,
    })
    if (!forceConfirmed) return
    ok = await noteStore.deleteNotebook(notebook.id, true)
  }
  if (!ok) {
    toast.error(noteStore.error ?? '删除失败')
    return
  }
  toast.success('笔记本已删除')
  if (selectedManagerNotebookId.value === notebook.id) {
    selectedManagerNotebookId.value = noteStore.currentNotebookId
  }
  if (wasCurrent) {
    openTabIds.value = []
    newTabOpen.value = false
    activeNewTab.value = false
    selectedFolderId.value = undefined
    resetActiveNote()
    await selectFirstNote()
  }
}

async function openCreateFolder(): Promise<void> {
  const name = await inputDialog.open({ title: '新建文件夹', placeholder: '文件夹名称' })
  if (name === null) return
  const folder = await noteStore.createFolder(name, selectedFolderId.value ?? null)
  if (!folder) {
    toast.error(noteStore.error ?? '创建失败')
    return
  }
  toast.success('文件夹已创建')
}

async function removeNote(noteId: Id): Promise<void> {
  const confirmed = await confirm({
    title: '删除笔记',
    message: '删除后这篇笔记会从列表中移除。',
    confirmText: '删除',
    danger: true,
  })
  if (!confirmed) return

  const wasActive = activeNoteId.value === noteId
  const ok = await noteStore.deleteNote(noteId)
  if (!ok) {
    toast.error(noteStore.error ?? '删除失败')
    return
  }
  toast.success('笔记已删除')
  if (wasActive) {
    resetActiveNote()
    await selectFirstNote()
  }
}

async function removeFolder(folderId: Id): Promise<void> {
  const folder = noteStore.folders.find((item) => item.id === folderId)
  if (!folder) return

  const confirmed = await confirm({
    title: '删除文件夹',
    message: `确定删除「${folder.name}」？`,
    confirmText: '删除',
    danger: true,
  })
  if (!confirmed) return

  let ok = await noteStore.deleteFolder(folderId)
  if (!ok && noteStore.errorCode === 3005) {
    const forceConfirmed = await confirm({
      title: '删除全部内容',
      message: `「${folder.name}」下还有笔记或子文件夹。继续删除会连同里面的全部内容一起删除。`,
      confirmText: '我确认删除全部',
      danger: true,
      severe: true,
    })
    if (!forceConfirmed) return
    ok = await noteStore.deleteFolder(folderId, true)
  }
  if (!ok) {
    toast.error(noteStore.error ?? '删除失败')
    return
  }
  toast.success('文件夹已删除')

  if (selectedFolderId.value !== undefined
      && !noteStore.folders.some((item) => item.id === selectedFolderId.value)) {
    selectedFolderId.value = undefined
  }
  if (activeNoteId.value !== null
      && !noteStore.notes.some((item) => item.id === activeNoteId.value)) {
    resetActiveNote()
    await selectFirstNote()
  }
}

function selectFolder(folderId: Id | null): void {
  selectedFolderId.value = folderId
}

async function handleDropFolder(payload: { folderId: Id; parentId: Id | null }): Promise<void> {
  const ok = await noteStore.moveFolder(payload.folderId, payload.parentId)
  if (!ok) {
    toast.error(noteStore.error ?? '移动失败')
    return
  }
  toast.success(payload.parentId === null ? '已移到根目录' : '已移动到文件夹')
}

async function handleDropNote(payload: { noteId: Id; folderId: Id | null }): Promise<void> {
  if (noteStore.currentNotebookId === null) return
  const ok = await noteStore.moveNote(payload.noteId, {
    notebookId: noteStore.currentNotebookId,
    folderId: payload.folderId,
  })
  if (!ok) {
    toast.error(noteStore.error ?? '移动失败')
    return
  }
  toast.success(payload.folderId === null ? '已移到未分类' : '已移动到文件夹')
}

function openMoveDialog(noteId: Id): void {
  movingNoteId.value = noteId
  showMoveDialog.value = true
}

async function confirmMoveNote(folderId: Id | null): Promise<void> {
  const noteId = movingNoteId.value
  showMoveDialog.value = false
  if (noteId === null || noteStore.currentNotebookId === null) return
  const ok = await noteStore.moveNote(noteId, {
    notebookId: noteStore.currentNotebookId,
    folderId,
  })
  movingNoteId.value = null
  if (!ok) {
    toast.error(noteStore.error ?? '移动失败')
    return
  }
  toast.success(folderId === null ? '已移到未分类' : '已移动到文件夹')
}

async function handleNoteAction(action: ItemAction, noteId: Id): Promise<void> {
  if (action === 'move') {
    openMoveDialog(noteId)
    return
  }
  if (action === 'merge') {
    toast.info('合并功能尚未实现')
    return
  }
  if (action === 'rename') {
    await renameNote(noteId)
    return
  }
  if (action === 'delete') {
    await removeNote(noteId)
  }
}

async function handleFolderAction(action: ItemAction, folderId: Id): Promise<void> {
  if (action === 'move') {
    movingFolderId.value = folderId
    showMoveFolderDialog.value = true
    return
  }
  if (action === 'rename') {
    await renameFolder(folderId)
    return
  }
  if (action === 'delete') {
    await removeFolder(folderId)
  }
}

async function confirmMoveFolder(parentId: Id | null): Promise<void> {
  const folderId = movingFolderId.value
  showMoveFolderDialog.value = false
  if (folderId === null) return
  const ok = await noteStore.moveFolder(folderId, parentId)
  movingFolderId.value = null
  if (!ok) {
    toast.error(noteStore.error ?? '移动失败')
    return
  }
  toast.success(parentId === null ? '已移到根目录' : '已移动到文件夹')
}

async function renameNote(noteId: Id): Promise<void> {
  const note = noteStore.notes.find((item) => item.id === noteId)
  if (!note) return
  const title = await inputDialog.open({
    title: '重命名笔记',
    placeholder: '笔记标题',
    initialValue: note.title,
  })
  if (title === null) return

  const ok = await noteStore.saveNote(noteId, { title })
  if (!ok) {
    toast.error(noteStore.error ?? '重命名失败')
    return
  }
  if (activeNoteId.value === noteId) {
    noteTitle.value = title
  }
  toast.success('笔记已重命名')
}

async function renameFolder(folderId: Id): Promise<void> {
  const folder = noteStore.folders.find((item) => item.id === folderId)
  if (!folder) return
  const name = await inputDialog.open({
    title: '重命名文件夹',
    placeholder: '文件夹名称',
    initialValue: folder.name,
  })
  if (name === null) return

  const ok = await noteStore.renameFolder(folderId, {
    notebookId: folder.notebookId,
    name,
  })
  if (!ok) {
    toast.error(noteStore.error ?? '重命名失败')
    return
  }
  toast.success('文件夹已重命名')
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

function clearFolderSelection(event: MouseEvent): void {
  const target = event.target as HTMLElement | null
  if (!target) return
  // 顶部创建按钮作用于当前选中文件夹，点它不取消
  if (target.closest('.file-action-bar')) return
  const folderRow = target.closest('[data-folder-id]') as HTMLElement | null
  if (folderRow && selectedFolderId.value !== undefined
      && String(folderRow.dataset.folderId) === String(selectedFolderId.value)) {
    return
  }
  selectedFolderId.value = undefined
}

watch(navNotes, (notes) => {
  const existing = new Set(notes.map((note) => note.id))
  if (openTabIds.value.some((id) => !existing.has(id))) {
    openTabIds.value = openTabIds.value.filter((id) => existing.has(id))
  }
})

onMounted(async () => {
  document.addEventListener('click', clearFolderSelection)
  if (!authStore.isLoggedIn) return
  await noteStore.bootstrap()
  await selectFirstNote()
})

// 游客在页面上完成登录后，页面不会重新挂载，需要在这里补一次加载
watch(
  () => authStore.isLoggedIn,
  async (loggedIn) => {
    if (loggedIn) {
      await noteStore.bootstrap()
      await selectFirstNote()
    } else {
      noteStore.reset()
      resetActiveNote()
    }
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('click', clearFolderSelection)
  stopSideResize?.()
  stopRightResize?.()
  if (syncTimer) clearTimeout(syncTimer)
  void flushSave()
})
</script>

<style scoped>
.notebook-shell { position: relative; --mint: #e8f5ee; --mint-strong: #3c9270; }

.note-status-bar { position: absolute; z-index: 70; right: 0; bottom: 0; display: flex; height: 31px; align-items: center; gap: 10px; border-top: 1px solid #e4e4e7; border-left: 1px solid #e4e4e7; border-radius: 9px 0 0 0; background: rgba(255, 255, 255, .96); padding: 0 11px 0 14px; color: #71717a; font-size: 12px; box-shadow: -5px -4px 16px rgba(24, 24, 27, .04); }
.status-mode-button { display: inline-flex; height: 24px; width: 24px; align-items: center; justify-content: center; border-radius: 5px; color: #71717a; }.status-mode-button:hover { background: #f0f0ee; color: #27272a; }
.status-mode-menu { position: absolute; right: -3px; bottom: calc(100% + 7px); width: 164px; overflow: hidden; border: 1px solid #e2e2e2; border-radius: 10px; background: #fff; padding: 5px; box-shadow: 0 10px 26px rgba(24, 24, 27, .14); }.status-mode-menu button { display: flex; width: 100%; align-items: center; gap: 9px; border-radius: 6px; padding: 8px; text-align: left; color: #3f3f46; font-size: 13px; }.status-mode-menu button:hover, .status-mode-menu-item-active { background: #e8f5ee; color: #247456; }
.status-sync { display: inline-flex; align-items: center; }.status-synced { color: #3c9270; }.status-saving { color: #d98b32; }

.manager-icon-button { display: inline-flex; height: 30px; width: 30px; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; }
.manager-icon-button:hover { background: #f0f0ee; color: #27272a; }
.manager-notebook-row { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 7px; padding: 9px 10px; text-align: left; font-size: 13px; color: #3f3f46; }
.manager-notebook-row:hover { background: #ececea; }
.manager-notebook-row-active { background: #eaf5ef; color: #1f7253; font-weight: 600; }
.manager-badge { margin-left: auto; border-radius: 4px; background: #dff0e7; padding: 1px 6px; font-size: 10px; font-weight: 500; color: #2f7d5e; white-space: nowrap; }
.manager-avatar { display: flex; height: 44px; width: 44px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 10px; background: #eaf5ef; color: #2f7d5e; }
.manager-new-button { display: flex; width: 100%; align-items: center; justify-content: center; gap: 6px; height: 34px; border: 1px dashed #d4d4d8; border-radius: 7px; font-size: 13px; color: #52525b; }
.manager-new-button:hover { background: #fff; border-color: #a1a1aa; color: #27272a; }
.manager-icon-danger:hover { background: #fef2f2; color: #dc2626; }
.manager-description { display: flex; width: 100%; align-items: center; gap: 8px; margin-top: 18px; border-radius: 7px; padding: 9px 10px; font-size: 14px; line-height: 1.6; }
.manager-description:hover { background: #f7f7f6; }
</style>

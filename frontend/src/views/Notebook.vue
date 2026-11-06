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
      @resize-start="startSidePanelResize"
      @select-note="selectNote"
      @open-search-result="openSearchResult"
      @create-note="createNote"
      @create-folder="openCreateFolder"
      @delete-note="removeNote"
      @delete-folder="removeFolder"
      @select-notebook="switchNotebook"
      @manage-notebooks="openNotebookManager"
    />

    <NoteEditor
      v-model:title="noteTitle"
      v-model:content="noteContent"
      v-model:view-mode="viewMode"
      :note-id="activeNoteId ?? ''"
      :right-panel-visible="rightPanelVisible"
      :wiki-link-target-title="wikiLinkTargetTitle"
      @change="touchActiveNote"
      @update:right-panel-visible="rightPanelVisible = $event"
    />

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
import { useAuthStore } from '@/stores/authStore'
import { useNoteStore } from '@/stores/noteStore'
import type { Id } from '@/types/common'
import type { NoteListItem, Notebook } from '@/types/note'
import { formatDate, formatRelativeTime } from '@/utils/format'
import NoteAiPanel from './notebook/NoteAiPanel.vue'
import NoteEditor from './notebook/NoteEditor.vue'
import NotebookCreateDialog from './notebook/NotebookCreateDialog.vue'
import NotebookGlyph from './notebook/NotebookGlyph.vue'
import NotebookSidebar from './notebook/NotebookSidebar.vue'
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

const sidePanelVisible = ref(true)
const sidePanelWidth = ref(318)
const rightPanelVisible = ref(true)
const rightPanelWidth = ref(320)
const isSidePanelResizing = ref(false)
const isRightPanelResizing = ref(false)
const viewMode = ref<ViewMode>('writing')
const activeNoteId = ref<Id | null>(null)
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
const navFolders = computed<NotebookFolder[]>(
  () => noteStore.folders.map((folder) => ({ id: folder.id, name: folder.name })),
)
const activeNavNote = computed(
  () => navNotes.value.find((note) => note.id === activeNoteId.value) ?? null,
)
const activeNoteFolderId = computed(() => activeNavNote.value?.folderId ?? null)
const backlinkNotes = computed<NotebookNote[]>(() => [])
const outgoingNotes = computed<NotebookNote[]>(() => [])
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
  if (noteId === activeNoteId.value) return
  await flushSave()
  const note = await noteStore.openNote(noteId)
  if (!note) return
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
  await flushSave()
  const note = await noteStore.createNote(null)
  if (!note) {
    toast.error(noteStore.error ?? '创建笔记失败')
    return
  }
  activeNoteId.value = note.id
  noteTitle.value = note.title
  noteContent.value = note.contentMd
  isSynced.value = true
  saveError.value = null
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
    resetActiveNote()
    await selectFirstNote()
  }
}

async function openCreateFolder(): Promise<void> {
  const name = await inputDialog.open({ title: '新建文件夹', placeholder: '文件夹名称' })
  if (name === null) return
  const folder = await noteStore.createFolder(name)
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
  const confirmed = await confirm({
    title: '删除文件夹',
    message: '文件夹中的笔记会回到未分类。',
    confirmText: '删除',
    danger: true,
  })
  if (!confirmed) return

  const ok = await noteStore.deleteFolder(folderId)
  if (!ok) {
    toast.error(noteStore.error ?? '删除失败')
    return
  }
  toast.success('文件夹已删除')
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

onMounted(async () => {
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

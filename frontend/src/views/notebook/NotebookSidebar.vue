<template>
  <aside
    v-show="visible"
    class="relative flex w-[318px] shrink-0 flex-col border-r border-zinc-200 bg-[#fbfbfa]"
    :style="{ width: `${width}px` }"
  >
    <header class="relative flex h-14 shrink-0 items-center justify-center border-b border-zinc-200 px-3">
      <button class="icon-button absolute left-3" title="收起笔记侧栏" @click="emit('update:visible', false)">
        <PanelLeftClose class="h-[18px] w-[18px]" />
      </button>
      <nav class="flex items-center gap-1" aria-label="笔记视图">
        <button
          v-for="tab in sideTabs"
          :key="tab.key"
          class="side-tab"
          :class="sideTab === tab.key ? 'side-tab-active' : ''"
          :title="tab.label"
          :aria-label="tab.label"
          @click="sideTab = tab.key"
        >
          <component :is="tab.icon" class="h-[18px] w-[18px]" />
        </button>
      </nav>
    </header>

    <div v-if="sideTab === 'folders'" class="file-action-bar">
      <button class="icon-button" title="新建笔记" @click="emit('create-note')">
        <SquarePen class="h-[19px] w-[19px]" />
      </button>
      <button class="icon-button" title="新建文件夹">
        <FolderPlus class="h-[19px] w-[19px]" />
      </button>
      <div ref="sortMenuRef" class="relative">
        <button class="icon-button" :class="showSortMenu ? 'file-action-active' : ''" title="排序" @click="toggleSortMenu">
          <ArrowUpDown class="h-[19px] w-[19px]" />
        </button>
        <div v-if="showSortMenu" class="sort-menu">
          <template v-for="option in sortOptions" :key="option.value">
            <button class="sort-menu-item" :class="sortOption === option.value ? 'sort-menu-item-active' : ''" @click="selectSortOption(option.value)">
              <span>{{ option.label }}</span>
              <Check v-if="sortOption === option.value" class="h-4 w-4" />
            </button>
            <div v-if="option.dividerAfter" class="sort-menu-divider" />
          </template>
        </div>
      </div>
      <button class="icon-button" title="自动显示当前文件" @click="revealActiveNote">
        <PanelTop class="h-[19px] w-[19px]" />
      </button>
      <button class="icon-button" :title="allFoldersCollapsed ? '展开全部文件夹' : '折叠全部文件夹'" @click="toggleAllFolders">
        <span class="flex flex-col items-center -space-y-1.5" aria-hidden="true">
          <ChevronUp v-if="allFoldersCollapsed" class="h-3.5 w-3.5" />
          <ChevronDown v-else class="h-3.5 w-3.5" />
          <ChevronDown v-if="allFoldersCollapsed" class="h-3.5 w-3.5" />
          <ChevronUp v-else class="h-3.5 w-3.5" />
        </span>
      </button>
    </div>

    <section class="min-h-0 flex-1 overflow-y-auto px-3 py-4">
      <template v-if="sideTab === 'folders'">
        <div class="mb-3 flex items-center justify-between px-2">
          <span class="text-xs font-medium tracking-wide text-zinc-400">目录</span>
          <span class="text-xs text-zinc-400">{{ notes.length }} 篇</span>
        </div>

        <NotebookTree
          :folder-groups="folderGroups"
          :uncategorized-notes="uncategorizedNotes"
          :active-note-id="activeNoteId"
          :collapsed-folder-ids="collapsedFolders"
          @select-note="emit('select-note', $event)"
          @toggle-folder="toggleFolder"
        />
      </template>

      <template v-else-if="sideTab === 'recent'">
        <div class="recent-view-toolbar">
          <div ref="recentFilterMenuRef" class="relative inline-block">
            <button class="recent-filter" :class="showRecentFilterMenu ? 'recent-filter-active' : ''" title="选择文件夹" @click="toggleRecentFilterMenu">
              {{ recentFilterLabel }}
              <ChevronDown class="h-4 w-4" />
            </button>
            <div v-if="showRecentFilterMenu" class="recent-filter-menu">
              <button class="recent-filter-menu-item" :class="recentFolderFilter === 'all' ? 'recent-filter-menu-item-active' : ''" @click="selectRecentFolder('all')">
                <NotepadText class="h-5 w-5" />
                <span>全部笔记</span>
              </button>
              <div class="recent-filter-menu-divider" />
              <button
                v-for="folder in folders"
                :key="folder.id"
                class="recent-filter-menu-item recent-folder-menu-item"
                :class="recentFolderFilter === folder.id ? 'recent-filter-menu-item-active' : ''"
                @click="selectRecentFolder(folder.id)"
              >
                <Folder class="h-5 w-5" />
                <span class="min-w-0 flex-1">
                  <span class="block truncate">{{ folder.name }}</span>
                  <span class="mt-0.5 block text-xs font-normal text-zinc-400">{{ notesInFolder(folder.id).length }} 篇笔记</span>
                </span>
              </button>
            </div>
          </div>
          <div ref="recentSortMenuRef" class="relative">
            <button class="icon-button" :class="showRecentSortMenu ? 'file-action-active' : ''" title="排序" @click="toggleRecentSortMenu">
              <ArrowUpDown class="h-[18px] w-[18px]" />
            </button>
            <div v-if="showRecentSortMenu" class="sort-menu recent-sort-menu">
              <template v-for="option in sortOptions" :key="option.value">
                <button class="sort-menu-item" :class="recentSortOption === option.value ? 'sort-menu-item-active' : ''" @click="selectRecentSortOption(option.value)">
                  <span>{{ option.label }}</span>
                  <Check v-if="recentSortOption === option.value" class="h-4 w-4" />
                </button>
                <div v-if="option.dividerAfter" class="sort-menu-divider" />
              </template>
            </div>
          </div>
        </div>
        <section v-for="group in recentGroups" :key="group.label" class="mb-7">
          <h2 class="mb-2 px-1 text-xs font-medium text-zinc-400">{{ group.label }}</h2>
          <div class="space-y-1">
            <button
              v-for="note in group.notes"
              :key="note.id"
              class="recent-note-row"
              :class="activeNoteId === note.id ? 'recent-note-row-active' : ''"
              @click="emit('select-note', note.id)"
            >
              <span class="flex items-start justify-between gap-3">
                <span class="truncate text-sm font-semibold text-zinc-800">{{ note.title }}</span>
                <span class="shrink-0 text-xs text-zinc-400">{{ note.updatedLabel }}</span>
              </span>
              <span class="mt-1.5 line-clamp-2 text-left text-xs leading-5 text-zinc-500">{{ excerptFor(note) }}</span>
              <button
                v-if="recentFolderFilter === 'all' && note.folderId"
                class="recent-folder-link"
                :title="`筛选 ${folderName(note.folderId)} 的笔记`"
                @click.stop="selectRecentFolder(note.folderId)"
              >
                <Folder class="h-3.5 w-3.5" />
                {{ folderName(note.folderId) }}
              </button>
            </button>
          </div>
        </section>
      </template>

      <NotebookSearchPanel
        v-else-if="sideTab === 'search'"
        :notes="notes"
        :folders="folders"
        @open-search-result="emit('open-search-result', $event)"
      />
    </section>

    <footer v-if="sideTab !== 'search'" class="flex h-11 shrink-0 items-center border-t border-zinc-200 px-4 text-xs text-zinc-400">
      {{ notes.length }} 篇笔记 · {{ folders.length }} 个文件夹
    </footer>
    <div class="side-panel-resizer" title="拖拽调整宽度" @mousedown.prevent="emit('resize-start', $event)" />
  </aside>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import {
  ArrowUpDown, Check, ChevronDown, ChevronUp, Clock3, Folder, FolderPlus,
  ListTree, NotepadText, PanelLeftClose, PanelTop, Search, SquarePen,
} from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import type { Id } from '@/types/common'
import NotebookSearchPanel from './NotebookSearchPanel.vue'
import NotebookTree from './NotebookTree.vue'
import { compareNotesBy, filterFolderNotes, findFolderName, sortOptions } from './noteList'
import type {
  FolderGroup, NotebookFolder, NotebookNote, RecentGroupLabel, SideTab, SortOption,
} from './types'

/**
 * 笔记本页最左侧栏：笔记本切换按钮 + 文件夹 / 最近编辑 / 搜索三个 Tab。
 *
 * <p>笔记与文件夹数据由页面容器（`Notebook.vue`）持有并传入，本组件只持有**只属于侧栏**的状态
 * （当前 Tab、折叠的文件夹、排序方式、最近编辑的文件夹过滤）。需要改动共享数据时
 * （新建笔记、选中笔记）通过事件上抛；「搜索」Tab 的内容在同级 `NotebookSearchPanel.vue`。
 */
const props = defineProps<{
  visible: boolean
  /** 面板宽度（拖拽调整时由容器更新） */
  width: number
  notes: NotebookNote[]
  folders: NotebookFolder[]
  activeNoteId: Id
  activeNoteFolderId: Id | null
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  'resize-start': [event: MouseEvent]
  'select-note': [noteId: Id]
  'open-search-result': [noteId: Id]
  'create-note': []
}>()

const sideTab = ref<SideTab>('folders')
const collapsedFolders = ref<Id[]>([])
const sortOption = ref<SortOption>('titleAsc')
const recentFolderFilter = ref<'all' | Id>('all')
const recentSortOption = ref<SortOption>('updatedDesc')

// 三个下拉菜单：统一用 useDropdown 处理开关与点击外部关闭
const sortMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showSortMenu, toggle: toggleSortMenu, close: closeSortMenu } = useDropdown(sortMenuRef)
const recentFilterMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showRecentFilterMenu, toggle: toggleRecentFilterMenu, close: closeRecentFilterMenu } = useDropdown(recentFilterMenuRef)
const recentSortMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showRecentSortMenu, toggle: toggleRecentSortMenu, close: closeRecentSortMenu } = useDropdown(recentSortMenuRef)

const sideTabs = [
  { key: 'folders', icon: ListTree, label: '文件夹' },
  { key: 'recent', icon: Clock3, label: '最近编辑' },
  { key: 'search', icon: Search, label: '搜索' },
] as const

const recentGroupLabels: RecentGroupLabel[] = ['今天', '过去 7 天', '过去 30 天']

const allFoldersCollapsed = computed(() => props.folders.length > 0 && props.folders.every((folder) => collapsedFolders.value.includes(folder.id)))
const folderGroups = computed<FolderGroup[]>(() => props.folders.map((folder) => ({ folder, notes: notesInFolder(folder.id) })))
const uncategorizedNotes = computed(() => notesInFolder(null))
const recentFilterLabel = computed(() => recentFolderFilter.value === 'all'
  ? '全部'
  : folderName(recentFolderFilter.value))
const recentGroups = computed(() => recentGroupLabels.map((label) => ({
  label,
  notes: props.notes
    .filter((note) => note.recentGroup === label && (recentFolderFilter.value === 'all' || note.folderId === recentFolderFilter.value))
    .sort((left, right) => compareNotesBy(recentSortOption.value, left, right)),
})).filter((group) => group.notes.length))

function notesInFolder(folderId: Id | null): NotebookNote[] {
  return filterFolderNotes(props.notes, folderId, sortOption.value)
}

function folderName(folderId: Id | null): string {
  return findFolderName(props.folders, folderId)
}

function excerptFor(note: NotebookNote): string {
  return note.content
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[>*_`#[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 88)
}

function toggleFolder(folderId: Id): void {
  collapsedFolders.value = collapsedFolders.value.includes(folderId)
    ? collapsedFolders.value.filter((id) => id !== folderId)
    : [...collapsedFolders.value, folderId]
}

function toggleAllFolders(): void {
  collapsedFolders.value = allFoldersCollapsed.value ? [] : props.folders.map((folder) => folder.id)
}

function revealActiveNote(): void {
  if (props.activeNoteFolderId) {
    collapsedFolders.value = collapsedFolders.value.filter((id) => id !== props.activeNoteFolderId)
  }
  nextTick(() => {
    const selector = `[data-note-id="${props.activeNoteId}"]`
    document.querySelector<HTMLElement>(selector)?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  })
}

function selectSortOption(option: SortOption): void {
  sortOption.value = option
  closeSortMenu()
}

function selectRecentFolder(folderId: Id): void {
  recentFolderFilter.value = folderId
  closeRecentFilterMenu()
}

function selectRecentSortOption(option: SortOption): void {
  recentSortOption.value = option
  closeRecentSortMenu()
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
.file-action-bar { display: flex; height: 54px; flex-shrink: 0; align-items: center; justify-content: center; gap: 6px; border-bottom: 1px solid #f0f0ee; }
.file-action-active { background: #e9e9e7; color: #27272a; }
.sort-menu { position: absolute; z-index: 45; top: calc(100% + 7px); left: -10px; width: 224px; overflow: hidden; border: 1px solid #e2e2e2; border-radius: 8px; background: #fff; padding: 5px 0; box-shadow: 0 10px 26px rgba(24, 24, 27, .14); }
.sort-menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: 8px 14px; text-align: left; font-size: 14px; color: #3f3f46; }
.sort-menu-item:hover, .sort-menu-item-active { background: #f3f3f1; }.sort-menu-item-active { color: #18181b; }
.sort-menu-divider { height: 1px; margin: 5px 0; background: #e9e9e7; }
.side-panel-resizer { position: absolute; z-index: 20; top: 0; right: -3px; height: 100%; width: 6px; cursor: col-resize; }
.side-panel-resizer:hover { background: rgba(60, 146, 112, .2); }
.recent-note-row { display: flex; width: 100%; flex-direction: column; border-radius: 7px; padding: 10px 11px; text-align: left; transition: background-color .15s ease; }
.recent-note-row:hover, .recent-note-row-active { background: var(--mint); }
.recent-folder-link { display: inline-flex; width: fit-content; align-items: center; gap: 6px; margin-top: 8px; border-radius: 4px; padding: 2px 4px; color: #a1a1aa; font-size: 12px; line-height: 1.25; transition: color .15s ease, background-color .15s ease; }
.recent-folder-link:hover { background: rgba(255, 255, 255, .7); color: #287756; }
.recent-view-toolbar { display: flex; align-items: center; justify-content: space-between; margin: 1px 0 20px 2px; }
.recent-filter { display: inline-flex; align-items: center; gap: 4px; border-radius: 6px; padding: 7px 9px; font-size: 16px; font-weight: 700; color: #18181b; }
.recent-filter:hover, .recent-filter-active { background: #f1f1ef; color: #18181b; }
.recent-filter-menu { position: absolute; z-index: 45; top: 38px; left: -4px; width: 246px; overflow: hidden; border-radius: 12px; background: #fff; padding: 6px 0; box-shadow: 0 12px 26px rgba(24, 24, 27, .14); }
.recent-filter-menu-item { display: flex; width: 100%; align-items: center; gap: 11px; padding: 9px 14px; text-align: left; font-size: 14px; font-weight: 500; color: #27272a; }
.recent-filter-menu-item:hover, .recent-filter-menu-item-active { background: #f5faf7; }.recent-filter-menu-item-active { color: #1f7253; }
.recent-filter-menu-divider { height: 1px; margin: 4px 0; background: #eeeeec; }.recent-folder-menu-item { align-items: flex-start; }
.recent-sort-menu { top: calc(100% + 6px); right: 0; left: auto; }
</style>

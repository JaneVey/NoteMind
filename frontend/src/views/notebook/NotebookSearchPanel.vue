<template>
  <label class="relative block">
    <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
    <input v-model="searchKeyword" class="search-input" placeholder="搜索标题和正文" autofocus />
    <button v-if="searchKeyword" class="absolute right-2 top-1/2 inline-flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700" title="清空搜索" @click.prevent="searchKeyword = ''">
      <X class="h-4 w-4" />
    </button>
  </label>
  <template v-if="searchKeyword.trim()">
    <div class="mb-3 mt-4 flex items-center justify-between px-1">
      <span class="text-xs text-zinc-400">{{ searchResults.length }} 项结果</span>
      <div ref="searchSortMenuRef" class="relative">
        <button class="search-sort-button" @click="toggleSearchSortMenu">
          {{ searchSortLabel }}
          <ChevronDown class="h-3.5 w-3.5" />
        </button>
        <div v-if="showSearchSortMenu" class="search-sort-menu">
          <template v-for="option in sortOptions" :key="option.value">
            <button class="search-sort-menu-item" :class="searchSort === option.value ? 'search-sort-menu-item-active' : ''" @click="selectSearchSort(option.value)">
              {{ option.label }} <Check v-if="searchSort === option.value" class="h-4 w-4" />
            </button>
            <div v-if="option.dividerAfter" class="search-sort-menu-divider" />
          </template>
        </div>
      </div>
    </div>
    <div v-if="searchResults.length" class="space-y-1">
      <button v-for="note in searchResults" :key="note.id" class="search-result-row" @click="emit('open-search-result', note.id)">
        <span class="flex items-start justify-between gap-3">
          <span class="line-clamp-1 text-left text-sm font-semibold text-zinc-800" v-html="highlightSearchText(note.title)" />
          <span class="shrink-0 text-xs text-zinc-400">{{ note.updatedLabel }}</span>
        </span>
        <span class="mt-1.5 line-clamp-2 text-left text-xs leading-5 text-zinc-500" v-html="highlightSearchText(searchExcerptFor(note))" />
        <span class="mt-2 flex items-center justify-between text-xs text-zinc-400">
          <span class="flex items-center gap-1.5"><Folder class="h-3.5 w-3.5" />{{ folderName(note.folderId) }}</span>
          <span>{{ searchMatchCount(note) }} 处匹配</span>
        </span>
      </button>
    </div>
    <div v-else class="search-empty-state">没有找到“{{ searchKeyword }}”相关的笔记。</div>
  </template>
  <div v-else class="search-start-state">
    <Search class="h-5 w-5" />
    输入关键词，搜索笔记标题和正文
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Check, ChevronDown, Folder, Search, X } from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import type { Id } from '@/types/common'
import { compareNotesBy, findFolderName, sortOptions } from './noteList'
import type { NotebookFolder, NotebookNote, SortOption } from './types'

/**
 * 侧栏「搜索」Tab：关键词搜索标题与正文，命中处高亮。
 *
 * <p>关键词与排序只在本组件内使用，所以状态留在组件里；笔记数据由页面容器传入。
 */
const props = defineProps<{
  notes: NotebookNote[]
  folders: NotebookFolder[]
}>()

const emit = defineEmits<{
  'open-search-result': [noteId: Id]
}>()

const searchKeyword = ref('')
const searchSort = ref<SortOption>('updatedDesc')

const searchSortMenuRef = ref<HTMLElement | null>(null)
const { isOpen: showSearchSortMenu, toggle: toggleSearchSortMenu, close: closeSearchSortMenu } = useDropdown(searchSortMenuRef)

const searchSortLabel = computed(() => sortOptions.find((option) => option.value === searchSort.value)?.label || '')
const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  const matchedNotes = props.notes.filter((note) => `${note.title}\n${note.content}`.toLowerCase().includes(keyword))
  return matchedNotes.sort((left, right) => compareNotesBy(searchSort.value, left, right))
})

function folderName(folderId: Id | null): string {
  return findFolderName(props.folders, folderId)
}

function escapeHtml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function searchExcerptFor(note: NotebookNote): string {
  const content = note.content
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/[>*_`#[\]()]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  const keyword = searchKeyword.value.trim().toLowerCase()
  const index = content.toLowerCase().indexOf(keyword)
  if (index < 0) return content.slice(0, 96)
  const start = Math.max(0, index - 32)
  const end = Math.min(content.length, index + keyword.length + 64)
  return `${start ? '...' : ''}${content.slice(start, end)}${end < content.length ? '...' : ''}`
}

function searchMatchCount(note: NotebookNote): number {
  const text = `${note.title}\n${note.content}`.toLowerCase()
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return 0
  let count = 0
  let index = text.indexOf(keyword)
  while (index !== -1) {
    count += 1
    index = text.indexOf(keyword, index + keyword.length)
  }
  return count
}

function highlightSearchText(value: string): string {
  const escapedValue = escapeHtml(value)
  const keyword = searchKeyword.value.trim()
  if (!keyword) return escapedValue
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapedValue.replace(new RegExp(`(${escapedKeyword})`, 'gi'), '<mark>$1</mark>')
}

function selectSearchSort(option: SortOption): void {
  searchSort.value = option
  closeSearchSortMenu()
}
</script>

<style scoped>
.search-input { width: 100%; height: 38px; border: 1px solid #d4d4d8; border-radius: 7px; background: #fff; padding: 0 38px 0 34px; outline: none; font-size: 14px; color: #3f3f46; }
.search-input:focus { border-color: #8ccbb1; box-shadow: 0 0 0 3px rgba(120, 192, 156, .13); }
.search-sort-button { display: inline-flex; min-width: 130px; align-items: center; justify-content: space-between; gap: 6px; border: 1px solid #dedee0; border-radius: 6px; background: #fff; padding: 5px 7px; font-size: 12px; color: #52525b; }.search-sort-button:hover { border-color: #bcbcc1; color: #27272a; }
.search-sort-menu { position: absolute; z-index: 45; top: calc(100% + 4px); right: 0; width: 206px; overflow: hidden; border: 1px solid #a1a1aa; background: #fff; padding: 3px 0; box-shadow: 0 8px 22px rgba(24,24,27,.12); }.search-sort-menu-item { display: flex; width: 100%; align-items: center; justify-content: space-between; padding: 7px 10px; text-align: left; font-size: 13px; color: #27272a; }.search-sort-menu-item:hover, .search-sort-menu-item-active { background: #e8f5ee; color: #1f7253; }.search-sort-menu-divider { height: 1px; margin: 3px 0; background: #e5e5e5; }
.search-empty-state, .search-start-state { display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 28px; padding: 28px 12px; color: #a1a1aa; font-size: 13px; line-height: 1.6; text-align: center; }.search-empty-state { display: block; }
.search-result-row { display: flex; width: 100%; flex-direction: column; border-radius: 7px; padding: 10px 11px; text-align: left; transition: background-color .15s ease; }
.search-result-row:hover { background: var(--mint); }
.search-result-row :deep(mark) { border-radius: 2px; background: #fff0ae; padding: 0 1px; color: inherit; }
</style>

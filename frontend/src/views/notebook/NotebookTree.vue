<template>
  <div v-for="group in folderGroups" :key="group.folder.id" class="mb-4">
    <button class="folder-heading" @click="emit('toggle-folder', group.folder.id)">
      <ChevronDown class="h-4 w-4 transition-transform" :class="collapsedFolderIds.includes(group.folder.id) ? '-rotate-90' : ''" />
      <Folder class="h-[17px] w-[17px] text-[#58a889]" />
      <span class="truncate">{{ group.folder.name }}</span>
      <span class="ml-auto text-xs font-normal text-zinc-400">{{ group.notes.length }}</span>
    </button>
    <div v-show="!collapsedFolderIds.includes(group.folder.id)" class="mt-1 space-y-0.5">
      <button
        v-for="note in group.notes"
        :key="note.id"
        class="tree-note-row"
        :class="activeNoteId === note.id ? 'tree-note-row-active' : ''"
        :data-note-id="note.id"
        @click="emit('select-note', note.id)"
      >
        <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
        <span class="truncate">{{ note.title }}</span>
      </button>
    </div>
  </div>

  <div class="mb-2 mt-6 px-2 text-xs font-medium tracking-wide text-zinc-400">未分类</div>
  <div class="space-y-0.5">
    <button
      v-for="note in uncategorizedNotes"
      :key="note.id"
      class="tree-note-row"
      :class="activeNoteId === note.id ? 'tree-note-row-active' : ''"
      :data-note-id="note.id"
      @click="emit('select-note', note.id)"
    >
      <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
      <span class="truncate">{{ note.title }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ChevronDown, FileText, Folder } from 'lucide-vue-next'
import type { Id } from '@/types/common'
import type { FolderGroup, NotebookNote } from './types'

/**
 * 目录树：文件夹 → 笔记的层级展开与选中。
 *
 * <p>只负责展示：分组与排序由侧栏算好（`folderGroups`），折叠状态由侧栏持有，
 * 所以本组件没有自己的状态，选笔记 / 折叠文件夹都通过事件上抛。
 */
defineProps<{
  /** 文件夹分组（每组内的笔记已按当前排序方式排好） */
  folderGroups: FolderGroup[]
  /** 不属于任何文件夹的笔记 */
  uncategorizedNotes: NotebookNote[]
  activeNoteId: Id
  collapsedFolderIds: Id[]
}>()

const emit = defineEmits<{
  'select-note': [noteId: Id]
  'toggle-folder': [folderId: Id]
}>()
</script>

<style scoped>
.folder-heading { display: flex; width: 100%; align-items: center; gap: 7px; border-radius: 6px; padding: 7px 8px; text-align: left; font-size: 14px; font-weight: 500; color: #52525b; }
.folder-heading:hover { background: #f2f2f0; }
.tree-note-row { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 6px; padding: 7px 10px 7px 30px; text-align: left; font-size: 14px; color: #52525b; }
.tree-note-row:hover, .tree-note-row-active { background: var(--mint); color: #235f49; }
</style>

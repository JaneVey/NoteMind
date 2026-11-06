<template>
  <div v-for="row in rows" :key="row.key">
    <div
      v-if="row.type === 'folder' && row.folder"
      class="tree-row-wrap"
      :class="[
        isDropSubtree(row) ? 'drop-subtree' : '',
        selectedFolderId === row.folder.folder.id ? 'folder-row-selected' : '',
      ]"
      :style="{ paddingLeft: `${row.depth * 14}px` }"
    >
      <button
        v-if="hasChildren(row.folder)"
        class="tree-chevron"
        :title="collapsedFolderIds.includes(row.folder.folder.id) ? '展开' : '折叠'"
        @click.stop="emit('toggle-folder', row.folder.folder.id)"
      >
        <ChevronDown
          class="h-3.5 w-3.5 transition-transform"
          :class="collapsedFolderIds.includes(row.folder.folder.id) ? '-rotate-90' : ''"
        />
      </button>
      <span v-else class="tree-chevron-placeholder" />

      <button
        class="folder-heading"
        :class="selectedFolderId === row.folder.folder.id ? 'folder-heading-active' : ''"
        @click="emit('select-folder', row.folder.folder.id)"
        @dragover.prevent="onFolderDragOver(row.folder.folder.id)"
        @dragleave="onFolderDragLeave(row.folder.folder.id)"
        @drop.prevent="onDropToFolder($event, row.folder.folder.id)"
      >
        <Folder class="h-[16px] w-[16px] shrink-0 text-[#58a889]" />
        <span class="truncate">{{ row.folder.folder.name }}</span>
        <span class="ml-auto text-xs font-normal text-zinc-400">{{ row.folder.notes.length }}</span>
      </button>

      <button class="tree-row-more" title="更多操作" @click.stop="openMenu($event, 'folder', row.folder.folder.id)">
        <MoreHorizontal class="h-4 w-4" />
      </button>
    </div>

    <div
      v-else-if="row.note"
      class="tree-row-wrap"
      :class="[
        isDropSubtree(row) ? 'drop-subtree' : '',
        activeNoteId === row.note.id ? 'note-row-active' : '',
        draggingNoteId === row.note.id ? 'dragging' : '',
      ]"
      :style="{ paddingLeft: `${row.depth * 14 + 10}px` }"
      draggable="true"
      @click="emit('select-note', row.note.id)"
      @dragstart="onNoteDragStart($event, row.note)"
      @dragend="onNoteDragEnd"
    >
      <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
      <span class="min-w-0 flex-1 truncate">{{ row.note.title }}</span>
      <button class="tree-row-more" title="更多操作" @click.stop="openMenu($event, 'note', row.note.id)">
        <MoreHorizontal class="h-4 w-4" />
      </button>
    </div>
  </div>

  <button
    class="uncategorized-heading"
    :class="selectedFolderId === null ? 'uncategorized-heading-active' : ''"
    @click="emit('select-folder', null)"
  >未分类</button>
  <div
    class="root-drop-zone"
    :class="dragOverRoot ? 'root-drop-zone-active' : ''"
    @dragover.prevent="dragOverRoot = true"
    @dragleave="dragOverRoot = false"
    @drop.prevent="onDropToRoot($event)"
  >
    <div
      v-for="note in uncategorizedNotes"
      :key="note.id"
      class="tree-row-wrap tree-note-row"
      :class="[
        activeNoteId === note.id ? 'note-row-active' : '',
        draggingNoteId === note.id ? 'dragging' : '',
      ]"
      draggable="true"
      @click="emit('select-note', note.id)"
      @dragstart="onNoteDragStart($event, note)"
      @dragend="onNoteDragEnd"
    >
      <FileText class="h-[16px] w-[16px] shrink-0 text-zinc-400" />
      <span class="min-w-0 flex-1 truncate">{{ note.title }}</span>
      <button class="tree-row-more" title="更多操作" @click.stop="openMenu($event, 'note', note.id)">
        <MoreHorizontal class="h-4 w-4" />
      </button>
    </div>
  </div>

  <div v-if="menu" class="fixed inset-0 z-[290]" @click="closeMenu">
    <div
      class="absolute z-[300] w-[160px] overflow-hidden rounded-lg border border-zinc-200 bg-white py-1 shadow-xl"
      :style="{ left: `${menu.x}px`, top: `${menu.y}px` }"
      @click.stop
    >
      <button v-if="menu.type === 'note'" class="menu-item" @click="trigger('move')">
        <Move class="h-4 w-4" />移动
      </button>
      <button v-if="menu.type === 'folder'" class="menu-item" @click="trigger('move')">
        <Move class="h-4 w-4" />移动
      </button>
      <button
        v-if="menu.type === 'note'"
        class="menu-item"
        disabled
        title="合并功能待实现"
      >
        <Merge class="h-4 w-4" />合并
      </button>
      <button class="menu-item" @click="trigger('rename')">
        <Pencil class="h-4 w-4" />重命名
      </button>
      <button class="menu-item menu-item-danger" @click="trigger('delete')">
        <Trash2 class="h-4 w-4" />删除
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ChevronDown, FileText, Folder, Merge, MoreHorizontal, Move, Pencil, Trash2,
} from 'lucide-vue-next'
import type { Id } from '@/types/common'
import type { FolderNode, ItemAction, NotebookNote } from './types'

interface TreeRow {
  key: string
  type: 'folder' | 'note'
  depth: number
  folder?: FolderNode
  note?: NotebookNote
  pathIds: Id[]
}

const props = defineProps<{
  nodes: FolderNode[]
  uncategorizedNotes: NotebookNote[]
  activeNoteId: Id
  selectedFolderId: Id | null
  collapsedFolderIds: Id[]
}>()

const emit = defineEmits<{
  'select-note': [noteId: Id]
  'select-folder': [folderId: Id | null]
  'toggle-folder': [folderId: Id]
  'drop-note': [payload: { noteId: Id; folderId: Id | null }]
  'note-action': [action: ItemAction, noteId: Id]
  'folder-action': [action: ItemAction, folderId: Id]
}>()

const rows = computed<TreeRow[]>(() => {
  const result: TreeRow[] = []
  const walk = (nodes: FolderNode[], depth: number, parentPath: Id[]): void => {
    for (const node of nodes) {
      const path = [...parentPath, node.folder.id]
      result.push({ key: `f-${node.folder.id}`, type: 'folder', depth, folder: node, pathIds: path })
      if (props.collapsedFolderIds.includes(node.folder.id)) continue
      for (const note of node.notes) {
        result.push({ key: `n-${note.id}`, type: 'note', depth: depth + 1, note, pathIds: path })
      }
      walk(node.children, depth + 1, path)
    }
  }
  walk(props.nodes, 0, [])
  return result
})

const menu = ref<{ type: 'note' | 'folder'; id: Id; x: number; y: number } | null>(null)
const draggingNoteId = ref<Id | null>(null)
const dragOverFolderId = ref<Id | null>(null)
const dragOverRoot = ref(false)

function hasChildren(node: FolderNode): boolean {
  return node.children.length > 0 || node.notes.length > 0
}

function isDropSubtree(row: TreeRow): boolean {
  return dragOverFolderId.value !== null && row.pathIds.includes(dragOverFolderId.value)
}

function openMenu(event: MouseEvent, type: 'note' | 'folder', id: Id): void {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  menu.value = {
    type,
    id,
    x: Math.min(rect.left, window.innerWidth - 180),
    y: Math.min(rect.bottom + 4, window.innerHeight - 190),
  }
}

function closeMenu(): void {
  menu.value = null
}

function trigger(action: ItemAction): void {
  const current = menu.value
  if (!current) return
  if (current.type === 'note') {
    emit('note-action', action, current.id)
  } else {
    emit('folder-action', action, current.id)
  }
  closeMenu()
}

function onNoteDragStart(event: DragEvent, note: NotebookNote): void {
  draggingNoteId.value = note.id
  event.dataTransfer?.setData('text/plain', String(note.id))
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onNoteDragEnd(): void {
  draggingNoteId.value = null
  dragOverFolderId.value = null
  dragOverRoot.value = false
}

function onFolderDragOver(folderId: Id): void {
  dragOverFolderId.value = folderId
  dragOverRoot.value = false
}

function onFolderDragLeave(folderId: Id): void {
  if (dragOverFolderId.value === folderId) {
    dragOverFolderId.value = null
  }
}

function dropTarget(event: DragEvent): Id | null {
  const raw = draggingNoteId.value ?? event.dataTransfer?.getData('text/plain')
  if (raw === null || raw === '') return null
  const noteId = typeof raw === 'number' ? raw : Number(raw)
  return Number.isNaN(noteId) ? null : noteId
}

function onDropToFolder(event: DragEvent, folderId: Id): void {
  const noteId = dropTarget(event)
  if (noteId !== null) {
    emit('drop-note', { noteId, folderId })
  }
  onNoteDragEnd()
}

function onDropToRoot(event: DragEvent): void {
  const noteId = dropTarget(event)
  if (noteId !== null) {
    emit('drop-note', { noteId, folderId: null })
  }
  onNoteDragEnd()
}

onMounted(() => document.addEventListener('click', closeMenu))
onBeforeUnmount(() => document.removeEventListener('click', closeMenu))
</script>

<style scoped>
.tree-row-wrap { display: flex; min-height: 30px; align-items: center; gap: 6px; border-radius: 6px; padding-right: 4px; font-size: 14px; color: #52525b; transition: background-color .12s ease, opacity .12s ease; }
.tree-row-wrap:hover { background: #f2f2f0; }
.tree-row-wrap:hover .tree-row-more { opacity: 1; }
.folder-row-selected .tree-row-more { opacity: 1; }
.tree-chevron { display: inline-flex; height: 20px; width: 18px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 4px; color: #a1a1aa; }
.tree-chevron:hover { background: #e9e9e7; color: #52525b; }
.tree-chevron-placeholder { display: inline-block; width: 18px; flex-shrink: 0; }
.folder-heading { display: flex; min-width: 0; flex: 1; align-items: center; gap: 6px; border-radius: 6px; padding: 6px 4px; text-align: left; font-weight: 500; color: #52525b; }
.folder-heading-active { background: #eaf5ef; color: #1f7253; }
.tree-row-more { display: inline-flex; height: 22px; width: 22px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 4px; color: #a1a1aa; opacity: 0; transition: opacity .12s ease; }
.tree-row-more:hover { background: #e9e9e7; color: #3f3f46; }
.note-row-active { background: #eaf5ef; color: #1f7253; }
.note-row-active .tree-row-more { opacity: 1; }
.tree-note-row { padding-left: 26px; }
.dragging { opacity: .45; }
.drop-subtree { background: #f2f9f5; box-shadow: inset 2px 0 0 #58a889; }
.root-drop-zone { min-height: 36px; border-radius: 6px; }
.uncategorized-heading { display: block; width: 100%; margin: 18px 0 6px; border-radius: 6px; padding: 6px 8px; text-align: left; font-size: 12px; font-weight: 500; letter-spacing: .03em; color: #a1a1aa; }
.uncategorized-heading:hover { background: #f2f2f0; color: #52525b; }
.uncategorized-heading-active { background: #eaf5ef; color: #1f7253; }
.root-drop-zone-active { background: #f2f9f5; box-shadow: inset 0 0 0 1px #b7ddc9; }
.menu-item { display: flex; width: 100%; align-items: center; gap: 8px; padding: 8px 12px; text-align: left; font-size: 13px; color: #3f3f46; }
.menu-item:hover { background: #f4f4f5; }
.menu-item:disabled { cursor: not-allowed; color: #c4c4c8; }
.menu-item:disabled:hover { background: transparent; }
.menu-item-danger { color: #dc2626; }
.menu-item-danger:hover { background: #fef2f2; }
</style>

<template>
  <div
    v-if="visible"
    class="fixed inset-0 z-[300] flex items-start justify-center bg-black/20 pt-[12vh]"
    @click.self="close"
  >
    <div class="w-[620px] overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-2xl">
      <input
        ref="inputRef"
        v-model="keyword"
        class="h-14 w-full border-b border-zinc-100 px-5 text-base outline-none placeholder:text-zinc-400"
        placeholder="输入以切换或创建文件..."
        @keydown.down.prevent="moveSelection(1)"
        @keydown.up.prevent="moveSelection(-1)"
        @keydown.enter.prevent="confirm"
        @keydown.esc="close"
      />

      <div class="max-h-[320px] overflow-y-auto p-2">
        <button
          v-for="(item, index) in items"
          :key="item.key"
          type="button"
          class="quick-item"
          :class="index === selectedIndex ? 'quick-item-active' : ''"
          @mouseenter="selectedIndex = index"
          @click="choose(item)"
        >
          <Plus v-if="item.type === 'create'" class="h-4 w-4 shrink-0 text-zinc-400" />
          <FileText v-else class="h-4 w-4 shrink-0 text-zinc-400" />
          <span class="min-w-0 flex-1 truncate text-left">{{ item.title }}</span>
        </button>
        <p v-if="!items.length" class="py-8 text-center text-sm text-zinc-400">没有匹配的笔记</p>
      </div>

      <footer class="flex items-center justify-center gap-5 border-t border-zinc-100 px-4 py-3 text-xs text-zinc-400">
        <span><kbd>↵</kbd> 打开</span>
        <span><kbd>shift</kbd> + <kbd>↵</kbd> 创建</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { FileText, Plus } from 'lucide-vue-next'
import type { Id } from '@/types/common'
import type { NotebookNote } from './types'

interface QuickItem {
  key: string
  type: 'note' | 'create'
  title: string
  noteId?: Id
}

const props = defineProps<{
  visible: boolean
  notes: NotebookNote[]
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  open: [noteId: Id]
  create: [title: string]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const keyword = ref('')
const selectedIndex = ref(0)

const items = computed<QuickItem[]>(() => {
  const search = keyword.value.trim().toLowerCase()
  const matches = (search
    ? props.notes.filter((note) => note.title.toLowerCase().includes(search))
    : props.notes
  ).slice(0, 50)

  const result: QuickItem[] = matches.map((note) => ({
    key: `note-${note.id}`,
    type: 'note',
    title: note.title,
    noteId: note.id,
  }))

  const exact = props.notes.some((note) => note.title.toLowerCase() === search)
  if (search && !exact) {
    result.unshift({ key: 'create', type: 'create', title: `创建「${keyword.value.trim()}」` })
  }
  return result
})

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      keyword.value = ''
      selectedIndex.value = 0
      void nextTick(() => inputRef.value?.focus())
    }
  },
)

watch(keyword, () => {
  selectedIndex.value = 0
})

function moveSelection(step: number): void {
  if (!items.value.length) return
  selectedIndex.value = (selectedIndex.value + step + items.value.length) % items.value.length
}

function confirm(event: KeyboardEvent): void {
  const selected = items.value[selectedIndex.value]
  if (event.shiftKey && keyword.value.trim()) {
    emit('create', keyword.value.trim())
    return
  }
  if (selected) choose(selected, event)
}

function choose(item: QuickItem, event?: KeyboardEvent): void {
  if (event?.shiftKey && keyword.value.trim()) {
    emit('create', keyword.value.trim())
    return
  }
  if (item.type === 'create') {
    emit('create', keyword.value.trim())
    return
  }
  if (item.noteId !== undefined) {
    emit('open', item.noteId)
  }
}

function close(): void {
  emit('update:visible', false)
}
</script>

<style scoped>
.quick-item { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 6px; padding: 9px 10px; font-size: 14px; color: #3f3f46; }
.quick-item:hover, .quick-item-active { background: #f2f2f0; color: #18181b; }
kbd { border-radius: 4px; background: #f4f4f5; padding: 1px 5px; font-family: inherit; font-size: 11px; color: #52525b; }
</style>

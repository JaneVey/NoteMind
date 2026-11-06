<template>
  <div v-if="visible" class="fixed inset-0 z-[300] flex items-center justify-center bg-black/25" @click.self="close">
    <div class="flex w-[420px] flex-col overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-2xl">
      <div class="flex items-center gap-2 border-b border-zinc-100 px-4 py-3">
        <input
          ref="searchRef"
          v-model="keyword"
          class="h-9 flex-1 rounded-md bg-zinc-100 px-3 text-sm outline-none placeholder:text-zinc-400"
          placeholder="搜索文件夹"
          @keydown.down.prevent="moveSelection(1)"
          @keydown.up.prevent="moveSelection(-1)"
          @keydown.enter.prevent="confirmSelection"
        />
        <button class="dialog-icon-button" title="关闭" @click="close">
          <X class="h-5 w-5" />
        </button>
      </div>

      <div class="max-h-[320px] min-h-[120px] overflow-y-auto p-2">
        <button
          v-for="(option, index) in filtered"
          :key="option.id ?? 'root'"
          type="button"
          class="move-row"
          :class="index === activeIndex ? 'move-row-active' : ''"
          @mouseenter="activeIndex = index"
          @click="activeIndex = index"
          @dblclick="choose(option.id)"
        >
          <Folder class="h-4 w-4 shrink-0 text-zinc-400" />
          <span class="min-w-0 flex-1 truncate text-left">{{ option.path }}</span>
          <Check v-if="option.id === currentFolderId" class="h-4 w-4 shrink-0 text-emerald-600" />
        </button>
        <p v-if="!filtered.length" class="py-8 text-center text-sm text-zinc-400">没有匹配的文件夹</p>
      </div>

      <footer class="flex items-center justify-between border-t border-zinc-100 px-4 py-3">
        <span class="text-xs text-zinc-400">↵ 移动，双击也可</span>
        <div class="flex gap-2">
          <button class="dialog-secondary" @click="close">取消</button>
          <button class="dialog-primary" :disabled="!activeOption" @click="confirmSelection">移动</button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { Check, Folder, X } from 'lucide-vue-next'
import type { Id } from '@/types/common'
import type { MoveFolderOption } from './types'

const props = defineProps<{
  visible: boolean
  folders: MoveFolderOption[]
  currentFolderId?: Id | null
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  confirm: [folderId: Id | null]
}>()

const searchRef = ref<HTMLInputElement | null>(null)
const keyword = ref('')
const activeIndex = ref(0)

const filtered = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) return props.folders
  return props.folders.filter((option) => option.path.toLowerCase().includes(search))
})

const activeOption = computed(() => filtered.value[activeIndex.value] ?? null)

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      keyword.value = ''
      activeIndex.value = 0
      void nextTick(() => searchRef.value?.focus())
    }
  },
)

watch(filtered, () => {
  activeIndex.value = 0
})

function moveSelection(step: number): void {
  if (!filtered.value.length) return
  activeIndex.value = (activeIndex.value + step + filtered.value.length) % filtered.value.length
}

function confirmSelection(): void {
  if (activeOption.value) choose(activeOption.value.id)
}

function choose(folderId: Id | null): void {
  emit('confirm', folderId)
}

function close(): void {
  emit('update:visible', false)
}
</script>

<style scoped>
.dialog-icon-button { display: inline-flex; height: 30px; width: 30px; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; }
.dialog-icon-button:hover { background: #f0f0ee; color: #27272a; }
.move-row { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 6px; padding: 8px 10px; font-size: 13px; color: #3f3f46; }
.move-row:hover, .move-row-active { background: #eaf5ef; color: #1f7253; }
.dialog-secondary { height: 34px; border-radius: 7px; padding: 0 14px; font-size: 13px; color: #3f3f46; }
.dialog-secondary:hover { background: #f4f4f5; }
.dialog-primary { height: 34px; border-radius: 7px; background: #18181b; padding: 0 16px; font-size: 13px; font-weight: 500; color: #fff; }
.dialog-primary:hover { background: #27272a; }
.dialog-primary:disabled { opacity: .4; }
</style>

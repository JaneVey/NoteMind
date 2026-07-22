<template>
  <div class="px-3 pb-3">
    <!-- 列表头部 -->
    <div class="flex items-center justify-between mb-1 px-1">
      <span class="text-xs text-gray-400 font-medium">笔记</span>
      <div class="flex items-center gap-2">
        <span v-if="notes.length" class="text-xs text-gray-300">{{ notes.length }}</span>
        <button class="text-gray-400 hover:text-gray-600 transition-colors" @click="emit('createNote')">
          <Plus class="w-3.5 h-3.5" />
        </button>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="space-y-0.5">
      <div
        v-for="note in notes"
        :key="note.id"
        class="group flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer text-sm hover:bg-gray-100 transition-colors"
        :class="{ 'bg-gray-100': note.id === currentNoteId }"
        @click="emit('selectNote', note.id)"
      >
        <FileText class="w-4 h-4 text-gray-400 shrink-0" />
        <div class="flex-1 min-w-0">
          <div class="truncate text-gray-700">{{ note.title || '无标题' }}</div>
          <div class="text-[11px] text-gray-400 mt-0.5">{{ formatTime(note.updatedAt) }}</div>
        </div>
        <!-- hover 操作按钮 -->
        <div class="hidden group-hover:flex items-center gap-0.5 shrink-0">
          <button class="h-6 w-6 flex items-center justify-center text-gray-400 hover:text-gray-600 rounded" @click.stop="handleRename(note)">
            <Edit class="h-3.5 w-3.5" />
          </button>
          <button class="h-6 w-6 flex items-center justify-center text-gray-400 hover:text-red-500 rounded" @click.stop="handleDelete(note)">
            <Trash2 class="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!notes.length" class="text-center py-6 text-xs text-gray-400">
      <FileText class="w-5 h-5 mx-auto mb-1 text-gray-300" />
      暂无笔记
    </div>
  </div>
</template>

<script setup>
import { FileText, Edit, Trash2, Plus } from 'lucide-vue-next'

const props = defineProps({
  notes: { type: Array, default: () => [] },
  currentNoteId: { type: [Number, String], default: null },
})

const emit = defineEmits(['selectNote', 'deleteNote', 'renameNote', 'createNote'])

function handleRename(note) {
  const title = prompt('请输入新名称', note.title || '')
  if (title && title.trim()) {
    emit('renameNote', { id: note.id, title: title.trim() })
  }
}

function handleDelete(note) {
  if (confirm(`确定删除笔记「${note.title || '无标题'}」吗？`)) {
    emit('deleteNote', note.id)
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="px-3 py-2">
    <!-- 仓库选择 -->
    <div class="mb-2">
      <select
        v-model="localNotebookId"
        class="w-full h-8 rounded border border-gray-200 bg-white px-2 text-sm text-gray-600 outline-none focus:border-purple-400"
        @change="onSelectNotebook"
      >
        <option value="" disabled>选择仓库</option>
        <option v-for="nb in notebooks" :key="nb.id" :value="nb.id">{{ nb.name }}</option>
      </select>
    </div>

    <!-- 文件夹树（无文件夹时隐藏） -->
    <div v-if="localNotebookId && displayedFolders.length > 0" class="text-sm text-gray-600">
      <div
        v-for="folder in displayedFolders"
        :key="folder.id"
        :style="{ paddingLeft: (folder.level || 0) * 16 + 8 + 'px' }"
      >
        <button
          class="w-full flex items-center gap-1 py-1 rounded hover:bg-gray-200/60 transition-colors group"
          :class="{ 'text-gray-900 font-medium': currentFolderId === folder.id }"
          @click="emit('selectFolder', folder.id)"
        >
          <ChevronRight
            class="w-3.5 h-3.5 text-gray-400 shrink-0 transition-transform duration-150"
            :class="{ 'rotate-90': expandedFolders.has(folder.id) }"
          />
          <Folder class="w-4 h-4 text-gray-400 shrink-0" />
          <span class="truncate flex-1 text-left">{{ folder.name }}</span>
          <button
            class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-gray-600 transition-opacity"
            @click.stop="handleDeleteFolder(folder)"
          >
            <Trash2 class="w-3.5 h-3.5" />
          </button>
        </button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="localNotebookId" class="text-center py-6 text-xs text-gray-400">
      暂无文件夹
    </div>
    <div v-else class="text-center py-6 text-xs text-gray-400">
      选择仓库开始查看
    </div>

    <!-- 新建文件夹弹窗 -->
    <Dialog :open="showCreateDialog" @update:open="showCreateDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>新建文件夹</DialogTitle>
        </DialogHeader>
        <Input v-model="newFolderName" placeholder="输入文件夹名称" @keydown.enter="handleCreate" />
        <div class="flex justify-end gap-2 mt-4">
          <Button variant="outline" size="sm" @click="showCreateDialog = false">取消</Button>
          <Button size="sm" :disabled="!newFolderName.trim()" @click="handleCreate">确定</Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ChevronRight, Folder, Trash2 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Dialog from '@/components/ui/Dialog.vue'
import DialogContent from '@/components/ui/DialogContent.vue'
import DialogHeader from '@/components/ui/DialogHeader.vue'
import DialogTitle from '@/components/ui/DialogTitle.vue'

const props = defineProps({
  notebooks: { type: Array, default: () => [] },
  currentNotebookId: { type: [Number, String], default: null },
  currentFolderId: { type: [Number, String], default: null },
  folders: { type: Array, default: () => [] },
})

const emit = defineEmits(['selectFolder', 'selectNotebook', 'createFolder', 'deleteFolder'])

const localNotebookId = ref(props.currentNotebookId)
const showCreateDialog = ref(false)
const newFolderName = ref('')
const expandedFolders = ref(new Set())

watch(
  () => props.currentNotebookId,
  (notebookId) => {
    if (notebookId !== null && notebookId !== undefined) localNotebookId.value = notebookId
  },
  { immediate: true }
)

const displayedFolders = computed(() => {
  if (props.folders.length) return props.folders
  if (!localNotebookId.value || !props.notebooks.length) return []
  const nb = props.notebooks.find((n) => n.id === localNotebookId.value)
  return (nb && nb.folders) || []
})

function onSelectNotebook() {
  emit('selectNotebook', localNotebookId.value)
}

function handleCreate() {
  const name = newFolderName.value.trim()
  if (!name) return
  emit('createFolder', { name, notebookId: localNotebookId.value })
  showCreateDialog.value = false
  newFolderName.value = ''
}

function handleDeleteFolder(folder) {
  if (confirm(`删除文件夹「${folder.name}」？`)) {
    emit('deleteFolder', folder.id)
  }
}
</script>

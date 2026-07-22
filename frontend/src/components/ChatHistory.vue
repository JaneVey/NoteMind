<template>
  <div class="px-2">
    <!-- Favorited conversations pinned -->
    <template v-if="favoritedConversations.length">
      <div class="flex items-center gap-1 text-xs text-muted-foreground px-3 pt-2 pb-1 font-medium">
        <Star class="h-3 w-3 fill-yellow-500 text-yellow-500" />
        收藏
      </div>
      <div
        v-for="conv in favoritedConversations"
        :key="conv.id"
        class="group flex items-center justify-between px-3 py-2.5 rounded-md cursor-pointer transition-colors mb-0.5"
        :class="conv.id === currentId ? 'bg-accent' : 'hover:bg-muted'"
        @click="emit('select', conv.id)"
        @contextmenu.prevent="handleContextMenu($event, conv)"
      >
        <div class="flex-1 min-w-0">
          <div class="text-sm text-foreground truncate">{{ conv.title || '新对话' }}</div>
          <div class="text-[11px] text-muted-foreground mt-0.5">{{ formatTime(conv.updatedAt) }}</div>
        </div>
        <div class="flex items-center gap-0.5 flex-shrink-0 ml-2">
          <Star class="h-3.5 w-3.5 text-yellow-500 fill-yellow-500" />
          <div @click.stop>
            <DropdownMenu>
              <DropdownMenuTrigger
                as="button"
                class="opacity-0 group-hover:opacity-100 flex items-center justify-center h-7 w-7 rounded-md hover:bg-background transition-opacity focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <MoreHorizontal class="h-4 w-4 text-muted-foreground" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem @click="openRenameDialog(conv)">
                  <Pencil class="h-3.5 w-3.5 mr-2" />
                  重命名
                </DropdownMenuItem>
                <DropdownMenuItem @click="toggleFavorite(conv)">
                  <Star class="h-3.5 w-3.5 mr-2" />
                  {{ conv.isFavorite ? '取消收藏' : '收藏' }}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  class="text-destructive focus:text-destructive focus:bg-destructive/10"
                  @click="confirmDelete(conv)"
                >
                  <Trash2 class="h-3.5 w-3.5 mr-2" />
                  删除
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>
    </template>

    <!-- Regular conversations -->
    <template v-if="regularConversations.length">
      <div class="flex items-center gap-1 text-xs text-muted-foreground px-3 pt-2 pb-1 font-medium">
        <MessageSquare class="h-3 w-3" />
        最近对话
      </div>
      <div
        v-for="conv in regularConversations"
        :key="conv.id"
        class="group flex items-center justify-between px-3 py-2.5 rounded-md cursor-pointer transition-colors mb-0.5"
        :class="conv.id === currentId ? 'bg-accent' : 'hover:bg-muted'"
        @click="emit('select', conv.id)"
        @contextmenu.prevent="handleContextMenu($event, conv)"
      >
        <div class="flex-1 min-w-0">
          <div class="text-sm text-foreground truncate">{{ conv.title || '新对话' }}</div>
          <div class="text-[11px] text-muted-foreground mt-0.5">{{ formatTime(conv.updatedAt) }}</div>
        </div>
        <div @click.stop>
          <DropdownMenu>
            <DropdownMenuTrigger
              as="button"
              class="opacity-0 group-hover:opacity-100 flex items-center justify-center h-7 w-7 rounded-md hover:bg-background transition-opacity flex-shrink-0 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              <MoreHorizontal class="h-4 w-4 text-muted-foreground" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem @click="openRenameDialog(conv)">
                <Pencil class="h-3.5 w-3.5 mr-2" />
                重命名
              </DropdownMenuItem>
              <DropdownMenuItem @click="toggleFavorite(conv)">
                <Star class="h-3.5 w-3.5 mr-2" />
                收藏
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                class="text-destructive focus:text-destructive focus:bg-destructive/10"
                @click="confirmDelete(conv)"
              >
                <Trash2 class="h-3.5 w-3.5 mr-2" />
                删除
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </template>

    <!-- Empty state -->
    <div v-if="!conversations.length" class="text-center py-10 text-sm text-muted-foreground">
      暂无对话
    </div>

    <!-- Right-click context menu -->
    <teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="fixed z-[9999] min-w-[130px] bg-popover rounded-md shadow-lg border border-border py-1"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      >
        <button
          class="flex items-center gap-2 w-full px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors text-left"
          @click="openRenameDialog(contextMenu.conv)"
        >
          <Pencil class="h-3.5 w-3.5" />
          重命名
        </button>
        <button
          class="flex items-center gap-2 w-full px-4 py-2 text-sm text-foreground hover:bg-muted transition-colors text-left"
          @click="toggleFavorite(contextMenu.conv)"
        >
          <Star class="h-3.5 w-3.5" />
          {{ contextMenu.conv?.isFavorite ? '取消收藏' : '收藏' }}
        </button>
        <div class="h-px bg-border mx-2 my-1" />
        <button
          class="flex items-center gap-2 w-full px-4 py-2 text-sm text-destructive hover:bg-muted transition-colors text-left"
          @click="confirmDelete(contextMenu.conv)"
        >
          <Trash2 class="h-3.5 w-3.5" />
          删除
        </button>
      </div>
    </teleport>
    <div
      v-if="contextMenu.visible"
      class="fixed inset-0 z-[9998]"
      @click="closeContextMenu"
    />

    <!-- Rename Dialog -->
    <Dialog v-model:open="renameDialogVisible">
      <DialogContent class="sm:max-w-[360px]">
        <DialogHeader>
          <DialogTitle>重命名</DialogTitle>
        </DialogHeader>
        <div class="py-2">
          <Input v-model="renameValue" placeholder="输入新名称" />
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="renameDialogVisible = false">取消</Button>
          <Button @click="confirmRename">确定</Button>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog v-model:open="deleteDialogVisible">
      <DialogContent class="sm:max-w-[360px]">
        <DialogHeader>
          <DialogTitle>提示</DialogTitle>
        </DialogHeader>
        <p class="text-sm text-muted-foreground py-2">
          确定删除对话「{{ deleteTarget?.title || '新对话' }}」吗？
        </p>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="deleteDialogVisible = false">取消</Button>
          <Button variant="destructive" @click="confirmDeleteAction">确定</Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { Star, MessageSquare, MoreHorizontal, Pencil, Trash2 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import Dialog from '@/components/ui/Dialog.vue'
import DialogContent from '@/components/ui/DialogContent.vue'
import DialogHeader from '@/components/ui/DialogHeader.vue'
import DialogTitle from '@/components/ui/DialogTitle.vue'
import DropdownMenu from '@/components/ui/DropdownMenu.vue'
import DropdownMenuTrigger from '@/components/ui/DropdownMenuTrigger.vue'
import DropdownMenuContent from '@/components/ui/DropdownMenuContent.vue'
import DropdownMenuItem from '@/components/ui/DropdownMenuItem.vue'
import DropdownMenuSeparator from '@/components/ui/DropdownMenuSeparator.vue'

const props = defineProps({
  conversations: { type: Array, default: () => [] },
  currentId: { type: [Number, String], default: null },
})

const emit = defineEmits(['select', 'create', 'rename', 'delete', 'favorite'])

// Right-click context menu state
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  conv: null,
})

// Rename dialog state
const renameDialogVisible = ref(false)
const renameValue = ref('')
const renameTarget = ref(null)

// Delete dialog state
const deleteDialogVisible = ref(false)
const deleteTarget = ref(null)

// Computed: separate favorited and regular conversations
const favoritedConversations = computed(() =>
  props.conversations.filter((c) => c.isFavorite)
)

const regularConversations = computed(() =>
  props.conversations.filter((c) => !c.isFavorite)
)

function handleContextMenu(event, conv) {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.conv = conv
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.conv = null
}

function openRenameDialog(conv) {
  const target = conv
  closeContextMenu()
  if (!target) return
  renameTarget.value = target
  renameValue.value = target.title || ''
  renameDialogVisible.value = true
}

function confirmRename() {
  if (renameTarget.value && renameValue.value.trim()) {
    emit('rename', renameTarget.value.id, renameValue.value.trim())
  }
  renameDialogVisible.value = false
  renameTarget.value = null
}

function toggleFavorite(conv) {
  const target = conv
  closeContextMenu()
  if (target) {
    emit('favorite', target.id, !target.isFavorite)
  }
}

function confirmDelete(conv) {
  const target = conv
  closeContextMenu()
  if (!target) return
  deleteTarget.value = target
  deleteDialogVisible.value = true
}

function confirmDeleteAction() {
  if (deleteTarget.value) {
    emit('delete', deleteTarget.value.id)
  }
  deleteDialogVisible.value = false
  deleteTarget.value = null
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const day = 24 * 60 * 60 * 1000
  if (diff < day) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="notebook-tabs flex h-11 shrink-0 items-end gap-1 bg-[#f0f0ef] px-2">
    <div class="flex h-full min-w-0 flex-1 items-end">
      <div ref="scrollRef" class="tab-scroll flex min-w-0 items-end gap-1 overflow-x-auto" @wheel.prevent="onTabWheel">
        <div
          v-for="(tab, index) in tabs"
          :key="tab.id"
          class="tab"
          :class="[
            tab.id === activeId ? 'tab-active' : '',
            draggingId === tab.id ? 'tab-dragging' : '',
            dragOverIndex === index && draggingId !== null && draggingId !== tab.id ? 'tab-drop-target' : '',
          ]"
          draggable="true"
          @click="emit('select', tab.id)"
          @dragstart="onDragStart($event, tab.id)"
          @dragover.prevent="dragOverIndex = index"
          @dragleave="onDragLeave(index)"
          @drop.prevent="onDrop(index)"
          @dragend="onDragEnd"
        >
          <span class="min-w-0 flex-1 truncate">{{ tab.title }}</span>
          <button class="tab-close" title="关闭标签页" @click.stop="emit('close', tab.id)">
            <X class="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      <button
        class="tab-add"
        :class="showAddDivider ? 'tab-add-divided' : ''"
        title="新建标签页"
        @click="emit('create')"
      >
        <Plus class="h-4 w-4" />
      </button>
    </div>

    <div ref="menuRef" class="relative shrink-0 self-center">
      <button class="tab-menu-button" title="标签页列表" @click="toggleMenu">
        <ChevronDown class="h-4 w-4" />
      </button>
      <div v-if="menuOpen" class="tab-menu">
        <button class="tab-menu-item" :disabled="!tabs.length" @click="closeAll">
          <X class="h-4 w-4" />全部关闭
        </button>
        <button class="tab-menu-item" @click="createTab">
          <Plus class="h-4 w-4" />新建标签页
        </button>
        <div class="tab-menu-divider" />
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-menu-item"
          @click="selectFromMenu(tab.id)"
        >
          <FileText class="h-4 w-4" />
          <span class="min-w-0 flex-1 truncate text-left">{{ tab.title }}</span>
          <Check v-if="tab.id === activeId" class="h-4 w-4 shrink-0 text-emerald-600" />
        </button>
        <p v-if="!tabs.length" class="tab-menu-empty">没有打开的标签页</p>
      </div>
    </div>

    <button
      v-if="!rightPanelVisible"
      class="tab-menu-button self-center"
      title="展开上下文面板"
      @click="emit('expand-right')"
    >
      <PanelRightOpen class="h-4 w-4" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Check, ChevronDown, FileText, PanelRightOpen, Plus, X } from 'lucide-vue-next'
import { useDropdown } from '@/composables/useDropdown'
import type { Id } from '@/types/common'
import type { NoteTab } from './types'

const props = defineProps<{
  tabs: NoteTab[]
  activeId: Id | null
  rightPanelVisible: boolean
}>()

const emit = defineEmits<{
  select: [noteId: Id]
  close: [noteId: Id]
  reorder: [noteIds: Id[]]
  create: []
  'close-all': []
  'expand-right': []
}>()

const menuRef = ref<HTMLElement | null>(null)
const { isOpen: menuOpen, toggle: toggleMenu, close: closeMenu } = useDropdown(menuRef)

const draggingId = ref<Id | null>(null)
const dragOverIndex = ref(-1)
const scrollRef = ref<HTMLElement | null>(null)

/** 加号与最后一个标签之间也按标签间的分隔规则处理：前一个未选中才画线 */
const showAddDivider = computed(() => {
  if (!props.tabs.length) return false
  const last = props.tabs[props.tabs.length - 1]
  return last.id !== props.activeId
})

function ensureActiveVisible(): void {
  void nextTick(() => {
    const container = scrollRef.value
    const active = container?.querySelector<HTMLElement>('.tab-active')
    if (!container || !active) return
    const box = active.getBoundingClientRect()
    const viewport = container.getBoundingClientRect()
    if (box.left < viewport.left) {
      container.scrollLeft -= viewport.left - box.left + 8
    } else if (box.right > viewport.right) {
      container.scrollLeft += box.right - viewport.right + 8
    }
  })
}

watch(() => props.activeId, ensureActiveVisible)
watch(() => props.tabs.length, ensureActiveVisible)
onMounted(ensureActiveVisible)

function onDragStart(event: DragEvent, noteId: Id): void {
  draggingId.value = noteId
  event.dataTransfer?.setData('text/plain', String(noteId))
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragLeave(index: number): void {
  if (dragOverIndex.value === index) {
    dragOverIndex.value = -1
  }
}

function onDragEnd(): void {
  draggingId.value = null
  dragOverIndex.value = -1
}

function onDrop(targetIndex: number): void {
  const sourceId = draggingId.value
  if (sourceId === null) return
  const ids = props.tabs.map((tab) => tab.id)
  const from = ids.findIndex((id) => id === sourceId)
  if (from < 0 || from === targetIndex) {
    onDragEnd()
    return
  }
  ids.splice(from, 1)
  ids.splice(targetIndex, 0, sourceId)
  emit('reorder', ids)
  onDragEnd()
}

function closeAll(): void {
  closeMenu()
  emit('close-all')
}

function createTab(): void {
  closeMenu()
  emit('create')
}

function selectFromMenu(noteId: Id): void {
  closeMenu()
  emit('select', noteId)
}

function onTabWheel(event: WheelEvent): void {
  const el = event.currentTarget as HTMLElement
  el.scrollLeft += event.deltaY
}
</script>

<style scoped>
.tab-scroll { scrollbar-width: none; }
.tab-scroll::-webkit-scrollbar { display: none; }
.tab { position: relative; display: flex; min-width: 84px; max-width: 200px; height: 40px; flex-shrink: 1; align-items: center; gap: 4px; border-radius: 8px 8px 0 0; padding: 0 6px 0 12px; font-size: 13px; color: #71717a; cursor: pointer; transition: background-color .12s ease; }
.tab:hover { background: #e8e8e6; color: #27272a; }
.tab-active { border: 1px solid #e4e4e7; border-bottom-color: #fff; background: #fff; color: #18181b; }
.tab-active:hover { background: #fff; }
.tab-dragging { opacity: .45; }
.tab-drop-target { box-shadow: inset 2px 0 0 #58a889; }
.tab:not(.tab-active) + .tab:not(.tab-active)::before { content: ''; position: absolute; left: -3px; top: 12px; bottom: 12px; width: 1px; background: #d8d8d6; }
.tab-close { display: inline-flex; height: 20px; width: 20px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 4px; color: #a1a1aa; opacity: 0; transition: opacity .12s ease; }
.tab:hover .tab-close, .tab-active .tab-close { opacity: 1; }
.tab-close:hover { background: #e4e4e2; color: #27272a; }
.tab-menu-button { display: inline-flex; height: 28px; width: 28px; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; }
.tab-menu-button:hover { background: #e4e4e2; color: #27272a; }
.tab-add { position: relative; align-self: center; display: inline-flex; height: 28px; width: 28px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; margin-left: 4px; }
.tab-add:hover { background: #e4e4e2; color: #27272a; }
.tab-add-divided::before { content: ''; position: absolute; left: -3px; top: 6px; bottom: 6px; width: 1px; background: #d8d8d6; }
.tab-menu { position: absolute; z-index: 80; top: calc(100% + 6px); right: 0; width: 220px; overflow: hidden; border: 1px solid #e4e4e7; border-radius: 8px; background: #fff; padding: 4px; box-shadow: 0 10px 24px rgba(24, 24, 27, .12); }
.tab-menu-item { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 5px; padding: 8px; text-align: left; font-size: 13px; color: #3f3f46; }
.tab-menu-item:hover { background: #f4f4f5; }
.tab-menu-item:disabled { cursor: not-allowed; color: #c4c4c8; }
.tab-menu-item:disabled:hover { background: transparent; }
.tab-menu-divider { height: 1px; margin: 4px 2px; background: #e9e9e7; }
.tab-menu-empty { padding: 10px; text-align: center; font-size: 12px; color: #a1a1aa; }
</style>

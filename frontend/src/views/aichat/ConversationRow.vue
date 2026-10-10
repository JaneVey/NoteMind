<template>
  <button
    :class="[
      active ? 'sidebar-row-active' : '',
      isTitleScrolling ? 'chat-row-title-scrolling' : '',
    ]"
    class="sidebar-row chat-row group"
    :draggable="draggable ? 'true' : undefined"
    v-on="dragListeners"
    @click="emit('select', conversation)"
    @mouseenter="startTitleScroll"
    @mouseleave="stopTitleScroll"
  >
    <MessageCircle class="h-5 w-5 shrink-0" />
    <span class="conversation-title">
      <span class="conversation-title-static">{{ conversation.title }}</span>
      <span class="conversation-title-marquee">
        <span class="conversation-title-marquee-content" :style="{ '--title-scroll-distance': scrollOffset || '0px' }">
          {{ conversation.title }}
        </span>
      </span>
    </span>
    <span class="chat-row-actions">
      <button class="chat-row-action" :title="pinned ? '取消置顶' : '置顶聊天'" @click.stop="togglePin">
        <component :is="pinned ? PinOff : Pin" class="h-[18px] w-[18px]" />
      </button>
      <button class="chat-row-action" title="更多操作" @click.stop="emit('menu', { event: $event, id: conversation.id })">
        <MoreHorizontal class="h-[18px] w-[18px]" />
      </button>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { MessageCircle, MoreHorizontal, Pin, PinOff } from 'lucide-vue-next'
import type { Conversation } from '@/types/ai'
import type { Id } from '@/types/common'

type ConversationSummary = Pick<Conversation, 'id' | 'title'>
type DragListeners = Partial<Record<'dragstart' | 'dragover' | 'drop' | 'dragend', (event: DragEvent) => void>>

const props = withDefaults(defineProps<{
  conversation: ConversationSummary
  /** 是否为当前会话（仅展开态侧栏高亮；折叠弹层不传） */
  active?: boolean
  /** 是否已置顶（决定操作按钮是「置顶」还是「取消置顶」） */
  pinned?: boolean
  /** 是否参与拖拽排序（仅「聊天」分组可拖拽） */
  draggable?: boolean
}>(), {
  active: false,
  pinned: false,
  draggable: false,
})

const emit = defineEmits<{
  select: [conversation: ConversationSummary]
  pin: [conversation: ConversationSummary]
  unpin: [conversation: ConversationSummary]
  menu: [payload: { event: MouseEvent; id: Id }]
  'drag-start': [id: Id]
  'drop-on': [id: Id]
  'drag-end': []
}>()

/**
 * 标题跑马灯：悬停 180ms 后测量标题溢出量，溢出才播放滚动动画。
 * 每行只管自己的悬停状态与滚动距离。
 */
const hovering = ref(false)
const scrollOffset = ref<string | null>(null)
let measureTimer: number | null = null

const isTitleScrolling = computed(() => hovering.value && scrollOffset.value !== null)

function startTitleScroll(event: MouseEvent): void {
  hovering.value = true
  const row = event.currentTarget as HTMLElement
  measureTimer = window.setTimeout(() => {
    if (!hovering.value) return
    const viewport = row.querySelector<HTMLElement>('.conversation-title-marquee')
    const content = row.querySelector<HTMLElement>('.conversation-title-marquee-content')
    if (!viewport || !content) return
    const overflow = content.scrollWidth - viewport.clientWidth
    scrollOffset.value = overflow > 0 ? `-${overflow}px` : '0px'
  }, 180)
}

function stopTitleScroll(): void {
  // 与共享状态版（hoveredConversationId）语义一致：只置为未悬停，
  // 已排队的测量在回调里自行判断是否还需要测量，不主动取消
  hovering.value = false
}

function togglePin(): void {
  if (props.pinned) emit('unpin', props.conversation)
  else emit('pin', props.conversation)
}

function onDragStart(event: DragEvent): void {
  emit('drag-start', props.conversation.id)
  event.dataTransfer?.setData('text/plain', String(props.conversation.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function onDragOver(event: DragEvent): void {
  event.preventDefault()
}

function onDrop(event: DragEvent): void {
  event.preventDefault()
  emit('drop-on', props.conversation.id)
}

function onDragEnd(): void {
  emit('drag-end')
}

/** 不可拖拽的行不挂拖拽监听 —— 悬挂标记的「已置顶」分组没有 drop 目标 */
const dragListeners = computed<DragListeners>(() => props.draggable
  ? { dragstart: onDragStart, dragover: onDragOver, drop: onDrop, dragend: onDragEnd }
  : {})

onBeforeUnmount(() => {
  if (measureTimer !== null) window.clearTimeout(measureTimer)
})
</script>

<style scoped>
.sidebar-row {
  display: flex;
  height: 43px;
  width: 100%;
  align-items: center;
  gap: 11px;
  overflow: hidden;
  border-radius: 8px;
  padding: 0 10px;
  text-align: left;
  font-size: 16px;
  line-height: 1.25;
  color: #303030;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.sidebar-row:hover,
.sidebar-row-active {
  background: #f2f2f2;
  color: #161616;
}

.chat-row {
  gap: 0;
  cursor: grab;
  transition: gap 0.16s ease, background-color 0.18s ease, color 0.18s ease;
}

.chat-row:active {
  cursor: grabbing;
}

.chat-row:hover,
.chat-row:focus-within {
  gap: 8px;
}

.conversation-title {
  min-width: 0;
  flex: 1;
}

.conversation-title-static {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-title-marquee {
  display: none;
  overflow: hidden;
  white-space: nowrap;
}

.conversation-title-marquee-content {
  display: inline-block;
  white-space: nowrap;
}

.chat-row:hover .conversation-title-static,
.chat-row:focus-within .conversation-title-static {
  display: none;
}

.chat-row:hover .conversation-title-marquee,
.chat-row:focus-within .conversation-title-marquee {
  display: block;
}

.chat-row-title-scrolling .conversation-title-marquee-content {
  animation: conversation-title-scroll 1.4s linear 0.1s infinite alternate;
}

@keyframes conversation-title-scroll {
  from { transform: translateX(0); }
  to { transform: translateX(var(--title-scroll-distance)); }
}

.chat-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 1px;
  width: 0;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.chat-row:hover .chat-row-actions,
.chat-row:focus-within .chat-row-actions {
  width: 55px;
  opacity: 1;
  pointer-events: auto;
}

.chat-row-action {
  display: inline-flex;
  height: 27px;
  width: 27px;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: #777777;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.chat-row-action:hover {
  background: #e9e9e9;
  color: #262626;
}
</style>

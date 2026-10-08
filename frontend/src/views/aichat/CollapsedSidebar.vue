<template>
  <aside class="collapsed-sidebar">
    <button class="collapsed-logo-toggle" title="展开侧栏" @click="emit('expand')">
      <Sparkles class="collapsed-brand-icon h-7 w-7" stroke-width="2" />
      <PanelLeftOpen class="collapsed-open-icon h-6 w-6" />
    </button>
    <div class="mt-5 flex flex-col items-center gap-4">
      <button class="collapsed-tool" title="新聊天" @click="emit('new-chat')">
        <SquarePen class="h-6 w-6" />
      </button>
      <button class="collapsed-tool" title="搜索聊天" @click="emit('search')">
        <Search class="h-6 w-6" />
      </button>
      <button class="collapsed-tool" title="查看置顶聊天" @click="openList($event, 'pinned')">
        <Pin class="h-6 w-6" />
      </button>
      <button class="collapsed-tool" title="查看聊天" @click="openList($event, 'chat')">
        <MessageCircle class="h-6 w-6" />
      </button>
    </div>
  </aside>

  <Teleport to="body">
    <div v-if="listType" class="fixed inset-0 z-[89]" @click="closeList" />
    <section v-if="listType" class="collapsed-list-popup" :style="listStyle">
      <h2>{{ listType === 'pinned' ? '已置顶' : '最近聊天' }}</h2>
      <div class="collapsed-list-content scrollbar-thin">
        <template v-if="listType === 'pinned'">
          <ConversationRow
            v-for="item in pinnedConversations"
            :key="item.id"
            :conversation="item"
            :pinned="true"
            @select="selectListConversation"
            @unpin="emit('unpin', $event)"
            @menu="emit('menu', $event)"
          />
          <p v-if="!pinnedConversations.length" class="px-4 py-4 text-sm text-zinc-400">暂无置顶聊天</p>
        </template>

        <template v-else>
          <ConversationRow
            v-for="item in conversations"
            :key="item.id"
            :conversation="item"
            @select="selectListConversation"
            @pin="emit('pin', $event)"
            @menu="emit('menu', $event)"
          />
          <p v-if="!conversations.length" class="px-4 py-4 text-sm text-zinc-400">暂无聊天</p>
        </template>
      </div>
    </section>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MessageCircle, PanelLeftOpen, Pin, Search, Sparkles, SquarePen } from 'lucide-vue-next'
import type { Conversation } from '@/types/ai'
import type { Id } from '@/types/common'
import ConversationRow from './ConversationRow.vue'

type ConversationSummary = Pick<Conversation, 'id' | 'title'>
type ListType = 'pinned' | 'chat'

defineProps<{
  /** 「聊天」分组的会话（与展开态同一份列表，由父组件传入，本组件不另存一份） */
  conversations: ConversationSummary[]
  /** 「已置顶」分组的会话 */
  pinnedConversations: ConversationSummary[]
}>()

const emit = defineEmits<{
  expand: []
  'new-chat': []
  search: []
  select: [conversation: ConversationSummary]
  pin: [conversation: ConversationSummary]
  unpin: [conversation: ConversationSummary]
  menu: [payload: { event: MouseEvent; id: Id }]
}>()

/** 弹出层类型与定位只属于折叠态，因此状态留在这里；菜单/重命名/删除仍由 ConversationList 编排 */
const listType = ref<ListType | null>(null)
const listStyle = ref<Record<string, string>>({})

function openList(event: MouseEvent, type: ListType): void {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const popupHeight = 640
  listStyle.value = {
    left: `${rect.right + 10}px`,
    top: `${Math.max(16, Math.min(rect.top - 24, window.innerHeight - popupHeight - 16))}px`,
  }
  listType.value = type
}

function closeList(): void {
  listType.value = null
}

function selectListConversation(item: ConversationSummary): void {
  emit('select', item)
  closeList()
}
</script>

<style scoped>
.collapsed-sidebar {
  display: flex;
  width: 64px;
  min-width: 64px;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #ececec;
  background: #fdfdfc;
  padding-top: 12px;
}

.collapsed-tool,
.collapsed-logo-toggle {
  display: inline-flex;
  height: 48px;
  width: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #181818;
  transition: background-color 0.18s ease;
}

.collapsed-tool:hover,
.collapsed-logo-toggle:hover {
  background: #eeeeee;
}

.collapsed-brand-icon {
  display: block;
}

.collapsed-open-icon {
  display: none;
}

.collapsed-logo-toggle:hover .collapsed-brand-icon {
  display: none;
}

.collapsed-logo-toggle:hover .collapsed-open-icon {
  display: block;
}

.collapsed-list-popup {
  position: fixed;
  z-index: 90;
  width: min(390px, calc(100vw - 92px));
  max-height: min(640px, calc(100vh - 32px));
  overflow: hidden;
  border: 1px solid #e5e5e5;
  border-radius: 24px;
  background: #ffffff;
  padding: 18px 15px 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}

.collapsed-list-popup h2 {
  margin: 6px 12px 12px;
  color: #1f1f1f;
  font-size: 18px;
  font-weight: 700;
}

.collapsed-list-content {
  max-height: min(560px, calc(100vh - 120px));
  overflow-y: auto;
}
</style>

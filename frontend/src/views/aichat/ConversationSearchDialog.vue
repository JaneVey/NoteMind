<template>
  <Teleport to="body">
    <div class="search-dialog-backdrop" @click.self="emit('close')">
      <section class="search-dialog" :class="searchKeyword ? 'search-dialog-expanded' : ''" role="dialog" aria-modal="true">
        <div class="search-dialog-input">
          <Search class="h-5 w-5 shrink-0" />
          <input
            ref="searchInputRef"
            v-model="searchKeyword"
            autofocus
            placeholder="搜索对话内容..."
            @keydown.esc="emit('close')"
          />
          <button title="关闭搜索" @click="emit('close')"><X class="h-5 w-5" /></button>
        </div>

        <div v-if="searchKeyword" class="search-results scrollbar-thin" @scroll="loadMoreSearchResults">
          <button
            v-for="result in visibleSearchResults"
            :key="result.id"
            class="search-result-item"
            @click="selectSearchResult(result)"
          >
            <span class="search-result-icon"><MessageCircle class="h-4 w-4" /></span>
            <span class="min-w-0 flex-1 text-left">
              <span class="flex items-center justify-between gap-4">
                <strong class="truncate">{{ result.title }}</strong>
                <time>{{ result.date }}</time>
              </span>
              <span class="search-result-preview">{{ result.preview }}</span>
            </span>
          </button>
          <div v-if="visibleSearchResults.length < searchResults.length" class="search-result-loading">继续向下滚动以加载更多结果</div>
          <div v-else class="search-result-loading">已显示全部匹配结果</div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { MessageCircle, Search, X } from 'lucide-vue-next'
import type { Conversation } from '@/types/ai'
import type { Id } from '@/types/common'

type ConversationSummary = Pick<Conversation, 'id' | 'title'>

interface SearchResult {
  id: Id
  title: string
  preview: string
  date: string
}

const props = defineProps<{
  conversations: ConversationSummary[]
  pinnedConversations: ConversationSummary[]
}>()

const emit = defineEmits<{
  close: []
  select: [conversation: ConversationSummary]
}>()

const searchInputRef = ref<HTMLInputElement | null>(null)
const searchKeyword = ref('')
const searchResultLimit = ref(8)

const searchResults = computed<SearchResult[]>(() => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return []
  const conversationsResults = [...props.conversations, ...props.pinnedConversations].map((item, index) => ({
    id: item.id,
    title: item.title,
    preview: `与“${keyword}”相关的对话内容、笔记整理和后续讨论。`,
    date: index < 2 ? '今天' : `${index + 1} 天前`,
  }))
  const prototypeResults = Array.from({ length: 24 }, (_, index) => ({
    id: `prototype-search-${index}`,
    title: `${keyword}：学习与实践记录 ${index + 1}`,
    preview: `这里展示与 ${keyword} 相关的模拟对话摘要，用于搜索结果的前端原型。`,
    date: index < 3 ? '今天' : `${Math.min(index + 1, 30)} 天前`,
  }))
  return [...conversationsResults, ...prototypeResults]
})

const visibleSearchResults = computed(() => searchResults.value.slice(0, searchResultLimit.value))

watch(searchKeyword, () => {
  searchResultLimit.value = 8
})

onMounted(() => {
  searchInputRef.value?.focus()
})

function loadMoreSearchResults(event: Event): void {
  const container = event.currentTarget as HTMLElement
  const nearBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 24
  if (nearBottom && searchResultLimit.value < searchResults.value.length) {
    searchResultLimit.value = Math.min(searchResultLimit.value + 8, searchResults.value.length)
  }
}

function selectSearchResult(result: SearchResult): void {
  const conversation = props.conversations.find((item) => item.id === result.id)
    || props.pinnedConversations.find((item) => item.id === result.id)
  if (conversation) emit('select', conversation)
  emit('close')
}
</script>

<style scoped>
.search-dialog-backdrop {
  position: fixed;
  z-index: 200;
  inset: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: rgba(0, 0, 0, 0.34);
  padding: min(14vh, 150px) 20px 20px;
  backdrop-filter: blur(2px);
}

.search-dialog {
  width: min(100%, 800px);
  overflow: hidden;
  border: 1px solid #e2e2e2;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.23);
}

.search-dialog-input {
  display: flex;
  height: 72px;
  align-items: center;
  gap: 14px;
  padding: 0 19px;
  color: #333333;
}

.search-dialog-input input {
  min-width: 0;
  flex: 1;
  border: 0;
  background: transparent;
  color: #242424;
  font-size: 17px;
  outline: none;
}

.search-dialog-input input::placeholder {
  color: #959595;
}

.search-dialog-input button {
  display: inline-flex;
  height: 34px;
  width: 34px;
  align-items: center;
  justify-content: center;
  border-left: 1px solid #eeeeee;
  color: #8b8b8b;
}

.search-dialog-input button:hover {
  color: #333333;
}

.search-results {
  max-height: min(62vh, 600px);
  overflow-y: auto;
  border-top: 1px solid #eeeeee;
  padding: 10px 9px 12px;
}

.search-result-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 14px;
  border-radius: 12px;
  padding: 11px 12px;
  color: #303030;
  transition: background-color 0.16s ease;
}

.search-result-item:hover {
  background: #f2f2f2;
}

.search-result-icon {
  display: inline-flex;
  height: 38px;
  width: 38px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e5e5;
  border-radius: 999px;
  color: #777777;
}

.search-result-item strong {
  font-size: 15px;
  font-weight: 500;
}

.search-result-item time {
  flex: 0 0 auto;
  color: #929292;
  font-size: 13px;
}

.search-result-preview {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #828282;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-loading {
  padding: 12px 0 3px;
  text-align: center;
  color: #a0a0a0;
  font-size: 13px;
}
</style>

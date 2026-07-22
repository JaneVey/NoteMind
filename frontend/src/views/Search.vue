<script setup lang="ts">
import { ref } from 'vue'
import { Search } from 'lucide-vue-next'

const keyword = ref('')
interface SearchResult { id: string | number; title: string; excerpt: string }
const results = ref<SearchResult[]>([])

function doSearch() {
  if (keyword.value.trim()) {
    console.log('Search:', keyword.value)
    // TODO: call search API
  }
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-6 py-12">
    <div class="relative mb-8">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
      <input
        v-model="keyword"
        placeholder="搜索笔记内容..."
        class="w-full h-10 pl-10 pr-4 rounded-lg border bg-background text-sm placeholder:text-muted-foreground outline-none focus:border-ring focus:ring-1 focus:ring-ring transition-colors"
        @keydown.enter="doSearch"
      />
    </div>

    <div v-if="!keyword" class="text-center py-16">
      <Search class="mx-auto w-8 h-8 text-muted-foreground/40 mb-3" />
      <p class="text-sm text-muted-foreground">输入关键词开始搜索</p>
    </div>

    <div v-else-if="results.length === 0" class="text-center py-16">
      <p class="text-sm text-muted-foreground">未找到相关内容</p>
    </div>

    <div v-else class="space-y-4">
      <div v-for="item in results" :key="item.id" class="p-4 rounded-lg border hover:bg-accent/50 cursor-pointer transition-colors">
        <h3 class="font-medium text-sm">{{ item.title }}</h3>
        <p class="text-xs text-muted-foreground mt-1">{{ item.excerpt }}</p>
      </div>
    </div>
  </div>
</template>

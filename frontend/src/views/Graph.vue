<template>
  <div class="max-w-4xl mx-auto px-6 py-8">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-xl font-semibold">知识图谱</h2>
      <select
        v-model="selectedRepo"
        class="h-9 rounded-md border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
      >
        <option :value="null" disabled>选择仓库</option>
        <option v-for="repo in repos" :key="repo.id" :value="repo.id">
          {{ repo.name }}
        </option>
      </select>
    </div>

    <div class="rounded-xl border bg-card text-card-foreground shadow min-h-[400px] flex items-center justify-center">
      <div class="text-center px-6">
        <svg class="mx-auto w-16 h-16 text-muted-foreground/30 mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="5" cy="6" r="2" /><circle cx="19" cy="6" r="2" /><circle cx="12" cy="18" r="2" />
          <line x1="7" y1="7" x2="10" y2="16" /><line x1="14" y1="16" x2="17" y2="7" />
          <line x1="7.5" y1="7" x2="16.5" y2="7" />
        </svg>
        <p class="text-base text-muted-foreground mb-2">知识图谱功能正在开发中，敬请期待</p>
        <p class="text-sm text-muted-foreground/60">您将能够以图谱形式可视化笔记与文档之间的关联</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const selectedRepo = ref(null)
const repos = ref([
  { id: 1, name: '默认仓库' },
])
</script>

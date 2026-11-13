<template>
  <main class="flex min-w-0 flex-1 flex-col bg-white">
    <div class="flex flex-1 flex-col items-center justify-center px-6">
      <div class="flex w-[240px] flex-col gap-2">
        <button class="new-tab-action" @click="emit('create')">
          <SquarePen class="h-4 w-4" />新建笔记
        </button>
        <button class="new-tab-action" @click="emit('open-picker')">
          <Search class="h-4 w-4" />打开笔记
        </button>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { Search, SquarePen } from 'lucide-vue-next'

const emit = defineEmits<{
  create: []
  'open-picker': []
}>()
</script>

<style scoped>
.new-tab-action { display: flex; align-items: center; gap: 10px; border-radius: 8px; border: 1px solid #e4e4e7; background: #fafafa; padding: 10px 12px; font-size: 14px; color: #3f3f46; }
.new-tab-action:hover { border-color: #d4d4d8; background: #f4f4f5; color: #18181b; }
</style>

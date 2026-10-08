<template>
  <div class="find-panel">
    <Search class="h-4 w-4 text-zinc-400" /><input placeholder="查找当前笔记" /><button class="find-panel-arrow" title="上一个匹配"><ChevronUp class="h-4 w-4" /></button><button class="find-panel-arrow" title="下一个匹配"><ChevronDown class="h-4 w-4" /></button><button class="icon-button" title="关闭查找" @click="emit('close')"><X class="h-4 w-4" /></button>
  </div>
</template>

<script setup lang="ts">
import { ChevronDown, ChevronUp, Search, X } from 'lucide-vue-next'

/** 查找/替换面板。是否显示由父组件用 `v-if` 控制。 */
const emit = defineEmits<{
  close: []
}>()
</script>

<style scoped>
.icon-button { display: inline-flex; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; transition: background-color .15s ease, color .15s ease; }
.icon-button { height: 30px; width: 30px; }
.icon-button:hover { background: #f0f0ee; color: #27272a; }
.find-panel { display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #e8e8e8; background: #fafafa; padding: 8px 20px; }.find-panel input { width: 260px; border: 0; background: transparent; outline: none; color: #3f3f46; font-size: 13px; }.find-panel-arrow { display: inline-flex; height: 26px; width: 26px; align-items: center; justify-content: center; border-radius: 5px; color: #71717a; }.find-panel-arrow:hover { background: #eee; color: #27272a; }
</style>

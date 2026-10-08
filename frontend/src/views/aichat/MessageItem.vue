<template>
  <div :class="message.role === 'user' ? 'justify-end' : 'justify-start'" class="flex">
    <div
      class="max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-7 shadow-sm"
      :class="message.role === 'user'
        ? 'bg-[#e8f4ee] text-zinc-800'
        : 'border border-zinc-100 bg-white text-zinc-600'"
    >
      {{ message.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessage } from '@/types/ai'

/**
 * 单条消息气泡。
 *
 * <p>根元素上把 `:class` 写在 `class` **之前**是有意的：这个顺序会让编译器
 * 序列化成 `justify-end flex`，与拆分前内联在 AIChat.vue 里的属性顺序逐字节一致。
 * 调换两者会得到 `flex justify-end`（渲染效果相同，但不再是零差异）。
 */
defineProps<{
  message: ChatMessage
}>()
</script>

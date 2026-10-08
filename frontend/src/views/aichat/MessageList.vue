<template>
  <section v-if="messages.length" ref="scrollRef" class="flex-1 overflow-y-auto px-8 py-8">
    <div class="mx-auto flex max-w-3xl flex-col gap-5">
      <MessageItem v-for="message in messages" :key="message.id" :message="message" />
    </div>
  </section>

  <section v-else class="flex flex-1 flex-col items-center justify-center px-6 pb-8">
    <div class="flex flex-col items-center">
      <div class="mb-2 rounded-md border border-[#bcebd5] bg-[#effaf4] px-2.5 py-1 text-xs font-medium text-[#4f9c78]">
        笔记 · 知识库 · AI
      </div>
      <div class="font-black leading-none tracking-normal text-zinc-900 text-[58px]">NoteMind</div>
      <div class="mt-2 text-[15px] font-semibold tracking-[0.35em] text-zinc-600">AI ASSISTANT</div>
    </div>

    <slot name="composer" />
  </section>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import type { ChatMessage } from '@/types/ai'
import MessageItem from './MessageItem.vue'

defineProps<{
  messages: ChatMessage[]
}>()

/** 滚动容器归本组件所有；父组件通过 ref 调用 scrollToBottom() */
const scrollRef = ref<HTMLElement | null>(null)

function scrollToBottom(): void {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

defineExpose({ scrollToBottom })
</script>

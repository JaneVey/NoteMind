<template>
  <div class="h-full">
    <button v-if="!visible" class="h-full w-9 flex flex-col items-center justify-center border-l bg-background cursor-pointer hover:bg-accent transition-colors" @click="emit('close')"><ChevronLeft class="h-4 w-4 text-muted-foreground" /><span class="text-[10px] text-muted-foreground mt-1 [writing-mode:vertical-rl]">AI</span></button>
    <div v-else class="h-full w-80 flex flex-col border-l bg-background">
      <div class="flex items-center justify-between px-4 py-3 border-b"><span class="font-semibold text-sm text-foreground">AI 助手</span><button class="flex items-center justify-center h-7 w-7 rounded-md hover:bg-muted" @click="emit('close')"><X class="h-4 w-4" /></button></div>
      <div class="flex gap-1.5 px-4 py-2.5 border-b flex-wrap"><Button v-for="command in shortcuts" :key="command.label" size="sm" variant="outline" @click="applyShortcut(command.prompt)">{{ command.label }}</Button></div>
      <div ref="messageListRef" class="flex-1 overflow-y-auto px-4 py-3 flex flex-col gap-3"><div v-for="(message, index) in messages" :key="index" class="flex" :class="message.role === 'user' ? 'justify-end' : 'justify-start'"><div class="max-w-[85%] px-3 py-2 rounded-lg text-sm leading-relaxed whitespace-pre-wrap break-words" :class="message.role === 'user' ? 'bg-primary text-primary-foreground rounded-br-sm' : 'bg-background text-foreground border border-border rounded-bl-sm'">{{ message.content }}</div></div><div v-if="!messages.length" class="flex-1 flex items-center justify-center"><span class="text-sm text-muted-foreground">向 AI 提问</span></div></div>
      <div class="px-4 py-3 border-t bg-background"><textarea v-model="inputText" rows="3" placeholder="输入消息..." class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm resize-none" @keydown.enter.exact="sendMessage" /><Button class="w-full mt-2" @click="sendMessage"><Send class="h-4 w-4 mr-1.5" />发送</Button></div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ChevronLeft, Send, X } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import type { ChatMessage } from '@/types/domain'
const props = withDefaults(defineProps<{ noteContent?: string; visible?: boolean }>(), { noteContent: '', visible: true })
const emit = defineEmits<{ close: []; sendMessage: [message: string] }>()
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const messageListRef = ref<HTMLDivElement | null>(null)
const shortcuts = [{ label: '总结', prompt: '请总结这段内容的核心要点：\n' }, { label: '解释', prompt: '请用通俗易懂的方式解释以下内容：\n' }, { label: '润色', prompt: '请润色以下文字，使其更通顺专业：\n' }]
function applyShortcut(prompt: string): void { inputText.value = prompt + props.noteContent }
function sendMessage(): void { const text = inputText.value.trim(); if (!text) return; emit('sendMessage', text); messages.value.push({ role: 'user', content: text }); inputText.value = ''; window.setTimeout(() => { messages.value.push({ role: 'assistant', content: '这是一个模拟回复。后续将接入真实 AI 接口。' }); scrollToBottom() }, 500); scrollToBottom() }
function scrollToBottom(): void { void nextTick(() => { const list = messageListRef.value; if (list) list.scrollTop = list.scrollHeight }) }
watch(() => messages.value.length, scrollToBottom)
</script>

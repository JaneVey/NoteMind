<template>
  <div>
    <div class="composer">
      <textarea
        :value="modelValue"
        rows="2"
        :placeholder="placeholder"
        class="composer-input"
        @input="onInput"
        @keydown="onKeydown"
      />
      <div class="composer-toolbar">
        <div class="flex items-center gap-2">
          <button class="composer-chip" title="选择模型"><Globe2 class="h-4 w-4" />DS 快速<ChevronDown class="h-3 w-3" /></button>
          <button class="composer-round" title="关联知识库">
            <AtSign class="h-4 w-4" />
          </button>
        </div>
        <div class="flex items-center gap-1">
          <button class="composer-icon" title="上传附件">
            <Paperclip class="h-[18px] w-[18px]" />
          </button>
          <button
            :class="modelValue.trim() ? 'send-button send-button-active' : 'send-button'"
            title="发送"
            :disabled="!modelValue.trim()"
            @click="emit('send')"
          >
            <Send class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AtSign, ChevronDown, Globe2, Paperclip, Send } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
}>(), {
  modelValue: '',
  placeholder: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
}>()

function onInput(event: Event): void {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}

/** Enter 发送，Shift+Enter 换行 */
function onKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter') return
  if (!event.shiftKey) {
    event.preventDefault()
    emit('send')
  }
}
</script>

<style scoped>
.composer {
  border: 1px solid #e3e3e3;
  border-radius: 23px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(30, 41, 36, 0.08);
  padding: 14px 16px 12px;
}

.composer-input {
  min-height: 58px;
  width: 100%;
  resize: none;
  border: 0;
  background: transparent;
  padding: 0 2px;
  color: #3f3f46;
  font-size: 18px;
  line-height: 28px;
  outline: none;
}

.composer-input::placeholder {
  color: #c7c7c7;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
}

.composer-chip,
.composer-round,
.composer-icon {
  display: inline-flex;
  height: 34px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 999px;
  color: #3f3f46;
  transition: background-color 0.18s ease;
}

.composer-chip {
  padding: 0 13px;
  border: 1px solid #ededed;
  background: #fafafa;
  font-size: 15px;
}

.composer-round,
.composer-icon {
  width: 34px;
}

.composer-round {
  border: 1px solid #ededed;
  background: #fafafa;
}

.composer-icon:hover,
.composer-round:hover,
.composer-chip:hover {
  background: #f4f4f5;
}

.send-button {
  display: inline-flex;
  height: 34px;
  width: 34px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #d4d4d8;
  color: white;
  transition: background-color 0.18s ease;
}

.send-button-active {
  background: #2f8c6b;
}
</style>

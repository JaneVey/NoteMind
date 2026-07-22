<template>
  <div class="flex flex-col h-full">
    <!-- 顶部工具栏 -->
    <div class="flex items-center justify-between px-4 py-2 border-b bg-muted/30">
      <span class="text-xs text-muted-foreground">{{ wordCount }} 字</span>
      <Button size="sm" @click="handleSave">
        <Save class="h-4 w-4 mr-1" />
        保存
      </Button>
    </div>

    <!-- 编辑区域 -->
    <textarea
      v-if="!readonly"
      ref="textareaRef"
      class="flex-1 w-full p-5 border-none outline-none resize-none font-mono text-sm leading-relaxed bg-background text-foreground placeholder:text-muted-foreground/50"
      :value="modelValue"
      :placeholder="placeholder"
      @input="handleInput"
      @keydown="handleKeydown"
    />
    <div v-else class="flex-1 p-5 overflow-y-auto bg-background">
      <pre class="font-mono text-sm leading-relaxed text-foreground whitespace-pre-wrap break-words m-0">{{ modelValue || '暂无内容' }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { Save } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, default: '' },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'save'])

const textareaRef = ref(null)
const placeholder = '在此输入 Markdown 内容...'

const wordCount = computed(() => {
  const text = props.modelValue || ''
  // 中文字数 + 英文单词数
  const chinese = (text.match(/[一-龥]/g) || []).length
  const words = text.replace(/[一-龥]/g, ' ').split(/\s+/).filter(Boolean).length
  return chinese + words
})

function handleInput(event) {
  emit('update:modelValue', event.target.value)
}

function handleKeydown(event) {
  // Ctrl+S 触发保存
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault()
    handleSave()
  }
}

function handleSave() {
  emit('save')
}

onMounted(() => {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.focus()
    }
  })
})
</script>

<template>
  <div
    class="flex gap-2.5 mb-4 max-w-[85%]"
    :class="message.role === 'user' ? 'flex-row-reverse ml-auto' : 'flex-row mr-auto'"
  >
    <!-- Avatar -->
    <div
      class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold flex-shrink-0"
      :class="message.role === 'user'
        ? 'bg-primary text-primary-foreground'
        : 'bg-muted text-muted-foreground border border-border'"
    >
      <span>{{ message.role === 'user' ? 'U' : 'AI' }}</span>
    </div>

    <!-- Bubble wrapper -->
    <div class="max-w-[calc(100%-42px)]">
      <!-- Bubble content -->
      <div class="relative">
        <div
          class="px-3.5 py-2.5 rounded-lg text-sm leading-relaxed whitespace-pre-wrap break-words"
          :class="message.role === 'user'
            ? 'bg-primary text-primary-foreground rounded-br-sm'
            : 'bg-background text-foreground border border-border rounded-bl-sm'"
        >
          {{ message.content }}
        </div>

        <!-- Streaming indicator -->
        <Loader2
          v-if="isStreaming"
          class="absolute -bottom-5 right-0 h-4 w-4 text-primary animate-spin"
        />
      </div>

      <!-- Sources -->
      <div v-if="sources.length" class="mt-1.5 flex flex-wrap gap-1 items-center">
        <span class="text-[11px] text-muted-foreground">引用来源：</span>
        <Badge
          v-for="(src, idx) in sources"
          :key="idx"
          variant="outline"
          class="text-[11px] cursor-pointer hover:opacity-80 transition-opacity"
        >
          {{ src.title || src.name || `来源 ${idx + 1}` }}
        </Badge>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import Badge from '@/components/ui/Badge.vue'

const props = defineProps({
  message: {
    type: Object,
    required: true,
    default: () => ({ role: 'user', content: '', metadata: null }),
  },
  isStreaming: { type: Boolean, default: false },
})

const sources = computed(() => {
  if (!props.message.metadata) return []
  const meta = props.message.metadata
  if (Array.isArray(meta.sources)) return meta.sources
  if (Array.isArray(meta.citations)) return meta.citations
  return []
})
</script>

<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <!-- 说明：本页是「AI 最小纵切验证」用的临时页面，验证通过后可删除 -->
    <div class="mb-6">
      <h2 class="text-xl font-semibold">AI 流式输出验证</h2>
      <p class="mt-1 text-sm text-muted-foreground">
        验证链路：接收输入 → 调用模型（开启思考模式）→
        <code class="rounded bg-muted px-1">reasoning_content</code> 与
        <code class="rounded bg-muted px-1">content</code> 分区块 SSE 推送 → 前端分区域渲染。
      </p>
      <p class="mt-1 text-xs text-muted-foreground/70">
        这是开发阶段的技术验证页，不属于产品功能，验证完成后可删除。
      </p>
    </div>

    <!-- 输入 -->
    <div class="rounded-xl border bg-card p-4 shadow">
      <textarea
        v-model="input"
        rows="2"
        class="w-full resize-none rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus:border-ring focus:ring-1 focus:ring-ring"
        placeholder="输入内容，例如：用一句话解释什么是 RAG"
        :disabled="status === 'streaming'"
        @keydown.ctrl.enter="send"
      />
      <div class="mt-3 flex flex-wrap items-center gap-2">
        <Button size="sm" :disabled="status === 'streaming' || !input.trim()" @click="send">
          {{ status === 'streaming' ? '生成中…' : '发送（Ctrl+Enter）' }}
        </Button>
        <Button size="sm" variant="outline" :disabled="status !== 'streaming'" @click="stop">
          停止
        </Button>
        <Button size="sm" variant="ghost" @click="reset">清空</Button>

        <Badge :variant="statusBadgeVariant">{{ statusText }}</Badge>
        <span v-if="status !== 'idle'" class="text-xs text-muted-foreground">
          耗时 {{ elapsed.toFixed(1) }}s
        </span>
      </div>
    </div>

    <!-- 错误 -->
    <div
      v-if="errorMessage"
      class="mt-4 rounded-xl border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive"
    >
      {{ errorMessage }}
    </div>

    <!-- 思考过程 -->
    <div v-if="reasoning || status === 'streaming'" class="mt-4 rounded-xl border bg-card shadow">
      <button
        class="flex w-full items-center justify-between px-4 py-3 text-left"
        @click="showReasoning = !showReasoning"
      >
        <span class="flex items-center gap-2 text-sm font-medium">
          <span class="text-muted-foreground">{{ showReasoning ? '▾' : '▸' }}</span>
          思考过程
          <span class="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
            {{ reasoningCount }} 个分片 / {{ reasoning.length }} 字
          </span>
        </span>
        <span v-if="!showReasoning" class="max-w-[50%] truncate text-xs text-muted-foreground">
          {{ reasoning.slice(0, 60) }}…
        </span>
      </button>
      <div
        v-show="showReasoning"
        class="border-t px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap text-muted-foreground"
      >
        {{ reasoning || '（等待思考内容…）' }}
      </div>
    </div>

    <!-- 正文 -->
    <div v-if="content || status === 'streaming'" class="mt-4 rounded-xl border bg-card shadow">
      <div class="flex items-center gap-2 border-b px-4 py-3 text-sm font-medium">
        回答
        <span class="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">
          {{ contentCount }} 个分片 / {{ content.length }} 字
        </span>
      </div>
      <div class="px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap">
        {{ content }}<span v-if="status === 'streaming'" class="animate-pulse">▍</span>
      </div>
    </div>

    <!-- 事件时间线（便于确认两类事件是否真的分开到达） -->
    <div v-if="timeline.length" class="mt-4 rounded-xl border bg-card shadow">
      <div class="border-b px-4 py-3 text-sm font-medium">事件时间线（最近 20 条）</div>
      <div class="max-h-64 overflow-y-auto px-4 py-2 font-mono text-xs">
        <div v-for="(e, i) in timeline" :key="i" class="py-0.5">
          <span :class="e.type === 'reasoning' ? 'text-amber-600' : 'text-emerald-600'">
            [{{ e.type }}]
          </span>
          <span class="text-muted-foreground"> +{{ e.text.length }} </span>
          <span>{{ e.text.length > 24 ? e.text.slice(0, 24) + '…' : e.text }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { URL_SEND_CHAT } from '@/api/ai'
import { streamChat } from '@/utils/sse'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'

type Status = 'idle' | 'streaming' | 'done' | 'error'

const input = ref('用一句话解释什么是 RAG')
const reasoning = ref('')
const content = ref('')
const status = ref<Status>('idle')
const errorMessage = ref('')
const elapsed = ref(0)
const reasoningCount = ref(0)
const contentCount = ref(0)
const showReasoning = ref(true)
const timeline = ref<{ type: string; text: string }[]>([])

let controller: AbortController | null = null
let timer: number | null = null

const statusText = computed(() => {
  switch (status.value) {
    case 'streaming':
      return '生成中'
    case 'done':
      return '已完成'
    case 'error':
      return '失败'
    default:
      return '待发送'
  }
})

const statusBadgeVariant = computed(() => {
  switch (status.value) {
    case 'streaming':
      return 'default' as const
    case 'done':
      return 'secondary' as const
    case 'error':
      return 'destructive' as const
    default:
      return 'outline' as const
  }
})

function stopTimer(): void {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

function reset(): void {
  reasoning.value = ''
  content.value = ''
  errorMessage.value = ''
  elapsed.value = 0
  reasoningCount.value = 0
  contentCount.value = 0
  timeline.value = []
  status.value = 'idle'
}

function send(): void {
  if (!input.value.trim() || status.value === 'streaming') return
  reset()
  status.value = 'streaming'
  controller = new AbortController()

  const startedAt = performance.now()
  timer = window.setInterval(() => {
    elapsed.value = (performance.now() - startedAt) / 1000
  }, 100)

  // URL_SEND_CHAT 是相对 axios baseURL('/api') 的路径，
  // 这里用原生 fetch 直连，因此需要补上前缀；开发环境由 Vite 代理到 8080
  streamChat(
    `/api${URL_SEND_CHAT}`,
    { content: input.value },
    {
      onReasoning(text) {
        reasoning.value += text
        reasoningCount.value += 1
        timeline.value.unshift({ type: 'reasoning', text })
        if (timeline.value.length > 20) timeline.value.pop()
      },
      onContent(text) {
        content.value += text
        contentCount.value += 1
        timeline.value.unshift({ type: 'content', text })
        if (timeline.value.length > 20) timeline.value.pop()
      },
      onDone() {
        status.value = 'done'
        stopTimer()
        controller = null
      },
      onError(message) {
        errorMessage.value = message
        status.value = 'error'
        stopTimer()
        controller = null
      },
    },
    { signal: controller.signal },
  )
}

function stop(): void {
  controller?.abort()
  controller = null
  stopTimer()
  if (status.value === 'streaming') status.value = 'done'
}

onBeforeUnmount(() => {
  controller?.abort()
  stopTimer()
})
</script>

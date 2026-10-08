<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { DocumentStatus, KnowledgeDocumentDetail } from '@/types/knowledge'
import { formatDateTime, formatFileSize } from '@/utils/format'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FileText, Trash2, RotateCw } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reparsing = ref(false)
const deleting = ref(false)
const doc = ref<KnowledgeDocumentDetail | null>(null)

/**
 * 状态展示配置。
 *
 * <p><b>变更记录</b>：此前这里有 **7 个条目**，把 `completed`/`done`/`parsed`
 * 三个同义词和 `failed`/`error` 两个同义词全列上了，理由是"以防万一"。
 * 但后端只认 4 个状态，多出来的分支永远走不到，反而让 `Record` 类型失去约束力
 * （真实值写错时编译器不会报错）。
 */
const STATUS_CONFIG: Record<DocumentStatus, { label: string; cls: string }> = {
  pending:    { label: '待解析',   cls: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300' },
  processing: { label: '解析中',   cls: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
  done:       { label: '已完成',   cls: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
  failed:     { label: '解析失败', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' },
}

const FALLBACK_STATUS = { label: '未知', cls: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300' }

const statusConfig = computed(() => {
  const status = doc.value?.chunkStatus
  return (status ? STATUS_CONFIG[status] : undefined) ?? FALLBACK_STATUS
})

const statusLabel = computed(() => statusConfig.value.label)
const statusClass = computed(() => statusConfig.value.cls)

function goBack(): void {
  void router.push('/knowledge')
}

async function fetchDetail(): Promise<void> {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    // TODO 待知识库模块后端实现后接入
    // doc.value = await getDocumentDetail(id)
    console.log('Fetch detail for:', id)
  } catch {
    console.error('获取文档详情失败')
  } finally {
    loading.value = false
  }
}

async function handleReparse(): Promise<void> {
  reparsing.value = true
  try {
    // TODO 待知识库模块后端实现后接入
    console.log('已开始重新解析')
  } catch {
    console.error('重新解析失败')
  } finally {
    reparsing.value = false
  }
}

async function handleDelete(): Promise<void> {
  if (!window.confirm('确定要删除该文档吗？此操作不可恢复。')) return
  deleting.value = true
  try {
    // TODO 待知识库模块后端实现后接入
    console.log('删除成功')
    void router.push('/knowledge')
  } catch {
    console.error('删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  void fetchDetail()
})
</script>

<template>
  <div class="max-w-3xl mx-auto px-6 py-8">
    <!-- 返回 -->
    <div class="flex items-center gap-3 mb-6">
      <button
        class="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        @click="goBack"
      >
        <ArrowLeft class="w-4 h-4" />
        返回
      </button>
      <h2 class="text-lg font-semibold">文档详情</h2>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="space-y-4 animate-pulse">
      <div class="rounded-lg border p-6">
        <div class="grid grid-cols-2 gap-4">
          <div v-for="i in 4" :key="i" class="space-y-1.5">
            <div class="h-3 w-16 bg-muted rounded" />
            <div class="h-5 w-32 bg-muted rounded" />
          </div>
        </div>
      </div>
      <div class="rounded-lg border p-6">
        <div class="h-4 w-28 bg-muted rounded mb-4" />
        <div class="h-32 bg-muted rounded" />
      </div>
    </div>

    <template v-if="!loading && doc">
      <!-- 基本信息 -->
      <div class="rounded-lg border p-6 mb-5">
        <div class="grid grid-cols-2 gap-5">
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">文件名称</span>
            <span class="text-sm text-foreground">{{ doc.fileName }}</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">文件类型</span>
            <span class="text-sm text-foreground">{{ doc.fileType || '-' }}</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">文件大小</span>
            <span class="text-sm text-foreground">{{ formatFileSize(doc.fileSize) || '-' }}</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">上传时间</span>
            <span class="text-sm text-foreground">{{ formatDateTime(doc.createdAt) || '-' }}</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">解析状态</span>
            <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium" :class="statusClass">
              {{ statusLabel }}
            </span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-muted-foreground">知识块</span>
            <span class="text-sm text-foreground">{{ doc.chunkCount }} 块</span>
          </div>
        </div>

        <!-- 解析失败时展示原因，否则用户只知道"失败了"却不知道为什么 -->
        <p
          v-if="doc.chunkStatus === 'failed' && doc.errorMessage"
          class="mt-4 text-xs text-destructive bg-destructive/10 rounded px-3 py-2"
        >
          {{ doc.errorMessage }}
        </p>
      </div>

      <!-- 解析结果预览 -->
      <div class="rounded-lg border p-6 mb-5">
        <div class="flex items-center gap-2 pb-4 mb-4 border-b">
          <FileText class="w-4 h-4 text-muted-foreground" />
          <span class="text-sm font-medium">解析结果预览</span>
        </div>
        <pre class="m-0 text-sm leading-relaxed text-foreground whitespace-pre-wrap break-words max-h-[500px] overflow-y-auto">{{ doc.rawContent || '暂无解析内容' }}</pre>
      </div>

      <!-- 操作 -->
      <div class="flex gap-3">
        <button
          class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors"
          :disabled="reparsing"
          @click="handleReparse"
        >
          <RotateCw class="w-4 h-4" :class="{ 'animate-spin': reparsing }" />
          重新解析
        </button>
        <button
          class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium bg-destructive text-destructive-foreground hover:bg-destructive/90 disabled:opacity-50 transition-colors"
          :disabled="deleting"
          @click="handleDelete"
        >
          <Trash2 class="w-4 h-4" />
          删除
        </button>
      </div>
    </template>
  </div>
</template>

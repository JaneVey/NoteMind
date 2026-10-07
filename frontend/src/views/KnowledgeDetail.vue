<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { KnowledgeDocument } from '@/types/domain'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FileText, Trash2, RotateCw } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reparsing = ref(false)
const deleting = ref(false)
const doc = ref<KnowledgeDocument | null>(null)

const statusConfig = computed(() => {
  const map: Record<KnowledgeDocument['status'], { label: string; cls: string }> = {
    pending:   { label: '待解析',   cls: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300' },
    processing:{ label: '解析中',   cls: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
    completed: { label: '已完成',   cls: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
    done:      { label: '已完成',   cls: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
    parsed:    { label: '已完成',   cls: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
    failed:    { label: '解析失败', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' },
    error:     { label: '解析失败', cls: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' },
  }
  const status = doc.value?.status
  return (status ? map[status] : undefined) || { label: status || '未知', cls: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300' }
})

const statusLabel = computed(() => statusConfig.value.label)
const statusClass = computed(() => statusConfig.value.cls)

function formatSize(bytes: number | undefined) {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}

function goBack() {
  router.push('/knowledge')
}

async function fetchDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    // TODO: 调用 API 获取文档详情
    // const res = await getDocumentDetail(id)
    // doc.value = res
    console.log('Fetch detail for:', id)
  } catch (err) {
    console.error('获取文档详情失败')
  } finally {
    loading.value = false
  }
}

async function handleReparse() {
  reparsing.value = true
  try {
    // TODO: 调用重新解析 API
    // await reparseDocument(route.params.id)
    console.log('已开始重新解析')
  } catch (err) {
    console.error('重新解析失败')
  } finally {
    reparsing.value = false
  }
}

async function handleDelete() {
  const confirmed = window.confirm('确定要删除该文档吗？此操作不可恢复。')
  if (!confirmed) return
  deleting.value = true
  try {
    // TODO: 调用删除 API
    // await deleteDocument(route.params.id)
    console.log('删除成功')
    router.push('/knowledge')
  } catch (err) {
    console.error('删除失败')
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="max-w-3xl mx-auto px-6 py-8">
    <!-- Header -->
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

    <!-- Skeleton loading -->
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

    <!-- Info card -->
    <div v-if="!loading && doc" class="rounded-lg border p-6 mb-5">
      <div class="grid grid-cols-2 gap-5">
        <div class="flex flex-col gap-1">
          <span class="text-xs text-muted-foreground">文件名称</span>
          <span class="text-sm text-foreground">{{ doc.name }}</span>
        </div>
        <div class="flex flex-col gap-1">
          <span class="text-xs text-muted-foreground">文件类型</span>
          <span class="text-sm text-foreground">{{ doc.type || '-' }}</span>
        </div>
        <div class="flex flex-col gap-1">
          <span class="text-xs text-muted-foreground">文件大小</span>
          <span class="text-sm text-foreground">{{ formatSize(doc.size) }}</span>
        </div>
        <div class="flex flex-col gap-1">
          <span class="text-xs text-muted-foreground">上传时间</span>
          <span class="text-sm text-foreground">{{ doc.createdAt || '-' }}</span>
        </div>
        <div class="flex flex-col gap-1">
          <span class="text-xs text-muted-foreground">解析状态</span>
          <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium" :class="statusClass">
            {{ statusLabel }}
          </span>
        </div>
      </div>
    </div>

    <!-- Content card -->
    <div v-if="!loading && doc" class="rounded-lg border p-6 mb-5">
      <div class="flex items-center gap-2 pb-4 mb-4 border-b">
        <FileText class="w-4 h-4 text-muted-foreground" />
        <span class="text-sm font-medium">解析结果预览</span>
      </div>
      <pre class="m-0 text-sm leading-relaxed text-foreground whitespace-pre-wrap break-words max-h-[500px] overflow-y-auto">{{ doc.content || '暂无解析内容' }}</pre>
    </div>

    <!-- Actions -->
    <div v-if="!loading && doc" class="flex gap-3">
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
  </div>
</template>

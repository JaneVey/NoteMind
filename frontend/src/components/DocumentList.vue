<template>
  <div>
    <!-- 表头 -->
    <div
      v-if="documents.length"
      class="grid grid-cols-[1fr_70px_80px_140px_90px_120px] gap-2 px-4 py-2 text-xs text-muted-foreground font-medium border-b"
    >
      <span>文件名</span>
      <span class="text-center">类型</span>
      <span class="text-center">大小</span>
      <span class="text-center">上传时间</span>
      <span class="text-center">状态</span>
      <span class="text-center">操作</span>
    </div>

    <!-- 数据行 -->
    <div class="divide-y">
      <div
        v-for="doc in documents"
        :key="doc.id"
        class="grid grid-cols-[1fr_70px_80px_140px_90px_120px] gap-2 px-4 py-3 text-sm items-center hover:bg-accent/50 transition-colors"
      >
        <span class="truncate" :title="doc.fileName || doc.name">{{ doc.fileName || doc.name || '-' }}</span>
        <span class="text-center text-muted-foreground text-xs">{{ doc.fileType || doc.type || '-' }}</span>
        <span class="text-center text-muted-foreground text-xs">{{ formatSize(doc.fileSize || doc.size) }}</span>
        <span class="text-center text-muted-foreground text-xs">{{ formatDate(doc.createdAt) }}</span>
        <div class="flex justify-center">
          <Badge :variant="statusVariant(doc.status)">{{ statusLabel(doc.status) }}</Badge>
        </div>
        <div class="flex items-center justify-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            class="h-8 w-8"
            title="查看详情"
            @click="emit('viewDetail', doc)"
          >
            <FileText class="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            class="h-8 w-8"
            :disabled="doc.status === 'processing'"
            title="重新解析"
            @click="emit('reparse', doc.id)"
          >
            <RotateCw class="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            class="h-8 w-8 text-destructive hover:text-destructive"
            title="删除"
            @click="emit('delete', doc.id)"
          >
            <Trash2 class="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!documents.length" class="text-center py-12 text-sm text-muted-foreground">
      <FileText class="h-10 w-10 mx-auto mb-2 text-muted-foreground/40" />
      <p>暂无文档</p>
    </div>
  </div>
</template>

<script setup>
import { FileText, RotateCw, Trash2 } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'

defineProps({
  documents: { type: Array, default: () => [] },
})

const emit = defineEmits(['delete', 'viewDetail', 'reparse'])

function formatSize(bytes) {
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

function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function statusVariant(status) {
  const map = {
    pending: 'secondary',
    processing: 'outline',
    done: 'default',
    failed: 'destructive',
    parsed: 'default',
    error: 'destructive',
  }
  return map[status] || 'secondary'
}

function statusLabel(status) {
  const map = {
    pending: '等待解析',
    processing: '解析中',
    done: '已完成',
    failed: '解析失败',
    parsed: '已完成',
    error: '解析失败',
  }
  return map[status] || status || '未知'
}
</script>

<template>
  <div class="py-4">
    <!-- 拖拽上传区域 -->
    <div
      class="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors"
      :class="isDragOver ? 'border-primary bg-primary/5' : 'border-border hover:border-muted-foreground/50'"
      @dragover.prevent="isDragOver = true"
      @dragleave.prevent="isDragOver = false"
      @drop.prevent="handleDrop"
      @click="triggerFileInput"
    >
      <FileText class="h-10 w-10 mx-auto mb-2 text-muted-foreground/60" />
      <p class="text-sm text-muted-foreground mb-1">
        拖拽文件到此处，或
        <button
          type="button"
          class="text-primary underline-offset-4 hover:underline font-medium"
          @click.stop="triggerFileInput"
        >点击上传</button>
      </p>
      <p class="text-xs text-muted-foreground/60">支持 .md .pdf .docx .pptx .png .jpg 格式</p>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      :accept="acceptTypes"
      multiple
      class="hidden"
      @change="handleFileChange"
    />

    <!-- 上传进度 -->
    <div v-if="uploading" class="mt-4 space-y-2">
      <div class="flex items-center justify-between text-sm">
        <span class="text-muted-foreground truncate max-w-[200px]">{{ uploadingFileName }}</span>
        <span class="text-muted-foreground">{{ Math.round(uploadProgress) }}%</span>
      </div>
      <div class="h-2 bg-secondary rounded-full overflow-hidden">
        <div
          class="h-full bg-primary transition-all duration-300 rounded-full"
          :style="{ width: uploadProgress + '%' }"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, type PropType } from 'vue'
import type { Id } from '@/types/domain'
import { FileText } from 'lucide-vue-next'

defineProps({
  knowledgeBaseId: { type: [Number, String] as PropType<Id>, required: true },
})

const emit = defineEmits<{ uploadSuccess: [file: File] }>()

const acceptTypes = '.md,.pdf,.docx,.pptx,.png,.jpg'
const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')

const MAX_SIZE = 50 * 1024 * 1024 // 50MB

function triggerFileInput() {
  fileInputRef.value?.click()
}

function validateFile(file: File) {
  if (file.size > MAX_SIZE) {
    alert('文件大小不能超过 50MB')
    return false
  }
  return true
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const files = event.dataTransfer?.files
  if (files?.length) {
    uploadFiles(Array.from(files))
  }
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files?.length) {
    uploadFiles(Array.from(files))
  }
  // 重置 input 以便再次选择相同文件
  target.value = ''
}

function uploadFiles(files: File[]) {
  for (const file of files) {
    if (!validateFile(file)) continue
    simulateUpload(file)
  }
}

function simulateUpload(file: File) {
  uploading.value = true
  uploadProgress.value = 0
  uploadingFileName.value = file.name

  const timer = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += Math.random() * 10
    }
  }, 200)

  // 模拟上传完成
  setTimeout(() => {
    clearInterval(timer)
    uploadProgress.value = 100
    emit('uploadSuccess', file)

    setTimeout(() => {
      uploading.value = false
      uploadProgress.value = 0
    }, 1000)
  }, 1500)
}
</script>

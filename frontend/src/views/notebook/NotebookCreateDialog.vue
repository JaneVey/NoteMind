<template>
  <div v-if="visible" class="fixed inset-0 z-[300] flex items-center justify-center bg-black/25" @click.self="close">
    <div class="w-[340px] overflow-hidden rounded-xl border border-zinc-200 bg-white shadow-2xl">
      <header class="flex items-center justify-between border-b border-zinc-100 px-4 py-3.5">
        <div class="flex items-center gap-2">
          <span class="flex h-6 w-6 items-center justify-center rounded-md bg-[#eaf5ef]">
            <NotebookIcon class="h-4 w-4 text-[#2f7d5e]" />
          </span>
          <h2 class="text-[15px] font-semibold text-zinc-900">创建笔记本</h2>
        </div>
        <button class="dialog-icon-button" title="关闭" @click="close">
          <X class="h-5 w-5" />
        </button>
      </header>

      <div class="space-y-4 px-4 py-4">
        <label class="block">
          <span class="mb-1.5 block text-sm text-zinc-600">名称 <span class="text-red-500">*</span></span>
          <input
            v-model="name"
            class="h-9 w-full rounded-md border border-zinc-300 px-3 text-sm outline-none focus:border-zinc-500"
            placeholder="请输入笔记本名称"
            maxlength="255"
            autofocus
            @keydown.enter="submit"
          />
        </label>

        <div>
          <span class="mb-1.5 block text-sm text-zinc-600">图标</span>
          <div class="grid grid-cols-3 gap-2">
            <label class="glyph-tile" :class="uploading ? 'glyph-tile-active' : ''" title="上传自定义图标">
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp"
                class="hidden"
                :disabled="uploading"
                @change="onFileChange"
              />
              <Upload class="h-4 w-4 text-zinc-400" />
              <span class="text-[11px] text-zinc-400">{{ uploading ? '上传中' : '上传' }}</span>
            </label>

            <button
              v-if="uploadedIcon"
              type="button"
              class="glyph-tile glyph-tile-image"
              :class="icon === uploadedIcon ? 'glyph-tile-active' : ''"
              @click="selectUploadedIcon"
            >
              <img :src="uploadedIcon" alt="自定义图标" />
            </button>

            <button
              v-for="preset in PRESET_ICONS"
              :key="preset"
              type="button"
              class="glyph-tile"
              :class="icon === preset ? 'glyph-tile-active' : ''"
              @click="icon = preset"
            >
              <span class="text-[38px] leading-none">{{ preset }}</span>
            </button>
          </div>
          <p class="mt-1.5 text-[11px] text-zinc-400">支持 PNG / JPEG / WebP，不超过 1MB</p>
        </div>
      </div>

      <footer class="flex justify-end gap-2 border-t border-zinc-100 px-4 py-3">
        <button class="dialog-secondary" @click="close">取消</button>
        <button
          class="dialog-primary"
          :disabled="!name.trim() || !icon || uploading || submitting"
          @click="submit"
        >{{ submitting ? '创建中…' : '创建' }}</button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Notebook as NotebookIcon, Upload, X } from 'lucide-vue-next'
import { uploadImage } from '@/api/file'
import { useToast } from '@/composables/useToast'

const PRESET_ICONS = ['📒', '📚', '💡', '🎯', '🧠', '🗂️', '🌱']
const ALLOWED_TYPES = ['image/png', 'image/jpeg', 'image/webp']
const MAX_SIZE = 1024 * 1024

const props = defineProps<{
  visible: boolean
  submitting?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  submit: [payload: { name: string; icon: string }]
}>()

const toast = useToast()
const name = ref('')
const icon = ref(PRESET_ICONS[0])
const uploadedIcon = ref<string | null>(null)
const uploading = ref(false)

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      name.value = ''
      icon.value = PRESET_ICONS[0]
      uploadedIcon.value = null
      uploading.value = false
    }
  },
)

function close(): void {
  emit('update:visible', false)
}

function submit(): void {
  if (!name.value.trim() || !icon.value) return
  emit('submit', { name: name.value.trim(), icon: icon.value })
}

function selectUploadedIcon(): void {
  if (uploadedIcon.value) icon.value = uploadedIcon.value
}

async function onFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!ALLOWED_TYPES.includes(file.type)) {
    toast.error('仅支持 PNG / JPEG / WebP 图片')
    return
  }
  if (file.size > MAX_SIZE) {
    toast.error('图片不能超过 1MB')
    return
  }

  uploading.value = true
  try {
    const result = await uploadImage(file)
    uploadedIcon.value = result.url
    icon.value = result.url
  } catch (cause) {
    toast.error(cause instanceof Error ? cause.message : '图片上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.dialog-icon-button { display: inline-flex; height: 28px; width: 28px; align-items: center; justify-content: center; border-radius: 6px; color: #71717a; }
.dialog-icon-button:hover { background: #f0f0ee; color: #27272a; }
.glyph-tile { display: flex; aspect-ratio: 1; flex-direction: column; align-items: center; justify-content: center; gap: 3px; border: 1px dashed #d4d4d8; border-radius: 9px; background: #fafafa; transition: border-color .15s ease, box-shadow .15s ease; }
.glyph-tile:hover { border-color: #a1a1aa; }
.glyph-tile-active { border: 2px solid #2f7d5e; background: #eaf5ef; box-shadow: 0 0 0 2px rgba(47, 125, 94, .12); }
.glyph-tile-image { padding: 0; overflow: hidden; }
.glyph-tile-image img { height: 100%; width: 100%; object-fit: cover; }
.dialog-secondary { height: 34px; border-radius: 7px; padding: 0 14px; font-size: 13px; color: #3f3f46; }
.dialog-secondary:hover { background: #f4f4f5; }
.dialog-primary { height: 34px; border-radius: 7px; background: #18181b; padding: 0 16px; font-size: 13px; font-weight: 500; color: #fff; }
.dialog-primary:hover { background: #27272a; }
.dialog-primary:disabled { opacity: .4; }
</style>

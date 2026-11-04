<template>
  <div class="pointer-events-none fixed inset-x-0 top-6 z-[300] flex flex-col items-center gap-2 px-4">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="pointer-events-auto flex max-w-[90vw] items-center gap-2.5 rounded-lg border border-zinc-200/80 bg-white/95 px-4 py-2.5 text-sm text-zinc-700 shadow-lg backdrop-blur-sm"
    >
      <CircleCheck v-if="toast.type === 'success'" class="h-4 w-4 shrink-0 text-emerald-600" />
      <CircleX v-else class="h-4 w-4 shrink-0 text-red-600" />
      <span class="leading-5">{{ toast.message }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck, CircleX } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

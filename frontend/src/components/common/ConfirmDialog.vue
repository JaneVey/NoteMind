<template>
  <div v-if="state" class="fixed inset-0 z-[300] flex items-center justify-center bg-black/25" @click.self="settle(false)">
    <div class="w-[420px] rounded-xl border border-zinc-200 bg-white p-5 shadow-2xl">
      <div class="flex items-start gap-3">
        <div
          class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
          :class="state.danger ? 'bg-red-50 text-red-600' : 'bg-zinc-100 text-zinc-500'"
        >
          <AlertTriangle class="h-[18px] w-[18px]" />
        </div>
        <div class="min-w-0">
          <h3 class="text-base font-semibold text-zinc-900">{{ state.title }}</h3>
          <p class="mt-1.5 text-sm leading-6 text-zinc-500">{{ state.message }}</p>
          <div v-if="state.severe" class="mt-3 rounded-md bg-red-50 px-3 py-2 text-xs leading-5 text-red-600">
            此操作不可撤销，请确认后继续。
          </div>
        </div>
      </div>
      <div class="mt-5 flex justify-end gap-2">
        <button class="h-9 rounded-md px-4 text-sm text-zinc-600 hover:bg-zinc-100" @click="settle(false)">
          {{ state.cancelText ?? '取消' }}
        </button>
        <button
          class="h-9 rounded-md px-4 text-sm font-medium text-white"
          :class="state.danger ? 'bg-red-600 hover:bg-red-700' : 'bg-zinc-900 hover:bg-zinc-800'"
          @click="settle(true)"
        >{{ state.confirmText ?? '确定' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AlertTriangle } from 'lucide-vue-next'
import { useConfirm } from '@/composables/useConfirm'

const { state, settle } = useConfirm()
</script>

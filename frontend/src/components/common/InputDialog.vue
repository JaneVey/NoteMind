<template>
  <div v-if="state" class="fixed inset-0 z-[300] flex items-center justify-center bg-black/20" @click.self="settle(null)">
    <div class="w-[380px] rounded-xl border border-zinc-200 bg-white p-5 shadow-2xl">
      <h3 class="mb-3 text-base font-semibold text-zinc-900">{{ state.title }}</h3>
      <textarea
        v-if="state.multiline"
        v-model="value"
        rows="4"
        class="w-full resize-none rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-zinc-500"
        :placeholder="state.placeholder"
        autofocus
        @keydown.ctrl.enter="submit"
        @keydown.esc="settle(null)"
      />
      <input
        v-else
        v-model="value"
        class="h-10 w-full rounded-md border border-zinc-300 px-3 text-sm outline-none focus:border-zinc-500"
        :placeholder="state.placeholder"
        autofocus
        @keydown.enter="submit"
        @keydown.esc="settle(null)"
      />
      <div class="mt-4 flex justify-end gap-2">
        <button class="h-9 rounded-md px-4 text-sm text-zinc-600 hover:bg-zinc-100" @click="settle(null)">取消</button>
        <button
          class="h-9 rounded-md bg-zinc-900 px-4 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-40"
          :disabled="!value.trim() && !state.allowEmpty"
          @click="submit"
        >{{ state.confirmText ?? '确定' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useInputDialog } from '@/composables/useInputDialog'

const { state, settle } = useInputDialog()
const value = ref('')

watch(state, (next) => {
  value.value = next?.initialValue ?? ''
})

function submit(): void {
  if (!state.value) return
  const trimmed = value.value.trim()
  if (!trimmed && !state.value.allowEmpty) return
  settle(trimmed)
}
</script>

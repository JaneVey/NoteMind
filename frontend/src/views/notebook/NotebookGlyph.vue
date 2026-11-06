<template>
  <img v-if="isImage" :src="icon ?? ''" alt="" class="h-full w-full rounded object-cover" />
  <span
    v-else
    class="leading-none"
    :class="size === 'lg' ? 'text-[26px]' : size === 'md' ? 'text-[20px]' : 'text-[15px]'"
  >{{ icon || '📒' }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  icon: string | null
  size?: 'sm' | 'md' | 'lg'
}>()

const isImage = computed(() => !!props.icon && /^https?:\/\//.test(props.icon))
</script>

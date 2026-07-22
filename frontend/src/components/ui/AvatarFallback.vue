<script setup lang="ts">
import { AvatarFallback } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  class: { type: String, default: '' },
  delayMs: { type: Number, default: 0 },
})
</script>

<template>
  <AvatarFallback
    :delay-ms="delayMs"
    :class="cn(
      'flex h-full w-full items-center justify-center rounded-full bg-muted text-sm font-medium',
      props.class,
    )"
  >
    <slot />
  </AvatarFallback>
</template>

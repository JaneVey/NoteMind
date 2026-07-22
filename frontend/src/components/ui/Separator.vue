<script setup>
import { Separator as RadixSeparator } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  orientation: { type: String, default: 'horizontal' },
  decorative: { type: Boolean, default: true },
  class: { type: String, default: '' },
})
</script>

<template>
  <RadixSeparator
    :orientation="orientation"
    :decorative="decorative"
    :class="cn(
      'shrink-0 bg-border',
      orientation === 'horizontal' ? 'h-px w-full' : 'h-full w-px',
      props.class,
    )"
  />
</template>

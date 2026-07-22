<script setup>
import { SelectTrigger } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  class: { type: String, default: '' },
})
</script>

<template>
  <SelectTrigger
    :class="cn(
      'flex h-9 w-full items-center justify-between whitespace-nowrap rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm ring-offset-background',
      'placeholder:text-muted-foreground',
      'focus:outline-none focus:ring-1 focus:ring-ring',
      'disabled:cursor-not-allowed disabled:opacity-50',
      '[&>span]:line-clamp-1',
      props.class,
    )"
  >
    <slot />
  </SelectTrigger>
</template>

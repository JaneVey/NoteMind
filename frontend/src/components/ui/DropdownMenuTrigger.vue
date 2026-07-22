<script setup>
import { DropdownMenuTrigger } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  as: { type: String, default: 'button' },
  class: { type: String, default: '' },
})
</script>

<template>
  <DropdownMenuTrigger :as="as" :class="cn(props.class)">
    <slot />
  </DropdownMenuTrigger>
</template>

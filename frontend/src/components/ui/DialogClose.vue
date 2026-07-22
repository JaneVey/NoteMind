<script setup>
import { DialogClose } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  as: { type: String, default: 'button' },
  class: { type: String, default: '' },
})
</script>

<template>
  <DialogClose :as="as" :class="cn(props.class)">
    <slot />
  </DialogClose>
</template>

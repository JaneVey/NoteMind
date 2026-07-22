<script setup>
import { DialogTrigger } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  as: { type: String, default: 'button' },
  class: { type: String, default: '' },
})
</script>

<template>
  <DialogTrigger :as="as" :class="cn(props.class)">
    <slot />
  </DialogTrigger>
</template>

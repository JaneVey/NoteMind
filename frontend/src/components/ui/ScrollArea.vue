<script setup lang="ts">
import { cn } from '@/lib/utils'

const props = defineProps({
  class: { type: String, default: '' },
})
</script>

<template>
  <div :class="cn('overflow-y-auto scrollbar-thin', props.class)">
    <slot />
  </div>
</template>

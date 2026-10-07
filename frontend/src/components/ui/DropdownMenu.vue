<script setup lang="ts">
import { DropdownMenuRoot } from 'radix-vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  class: { type: String, default: '' },
})

const emit = defineEmits(['update:open'])
</script>

<template>
  <DropdownMenuRoot :open="open" @update:open="emit('update:open', $event)">
    <slot />
  </DropdownMenuRoot>
</template>

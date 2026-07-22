<script setup>
import { DialogRoot } from 'radix-vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  class: { type: String, default: '' },
})

const emit = defineEmits(['update:open'])
</script>

<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <slot />
  </DialogRoot>
</template>

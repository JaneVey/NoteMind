<script setup>
import { SelectRoot } from 'radix-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  class: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <SelectRoot :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)">
    <slot />
  </SelectRoot>
</template>

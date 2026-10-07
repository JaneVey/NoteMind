<script setup lang="ts">
import { SelectValue } from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps({
  placeholder: { type: String, default: '' },
  class: { type: String, default: '' },
})
</script>

<template>
  <SelectValue :placeholder="placeholder" :class="cn(props.class)" />
</template>

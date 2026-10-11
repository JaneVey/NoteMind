<script setup lang="ts">
import { computed, ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import Input from './Input.vue'

/**
 * 带显隐切换的密码输入框。
 *
 * <p>为什么自己做而不是用浏览器自带的：那个按钮只有 Edge / Chromium 有，
 * Firefox 与 Safari 没有；而且它的显示依赖元素的聚焦与禁用状态，
 * 提交时输入框被禁用就会把它弄丢。样式也改不了、测不了。
 *
 * <p>浏览器的那个按钮由 style.css 里的 ::-ms-reveal 规则隐藏，避免出现两个。
 */

const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: 'current-password' },
  disabled: { type: Boolean, default: false },
  class: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const revealed = ref(false)

const inputType = computed(() => (revealed.value ? 'text' : 'password'))

/** 右侧留出按钮的位置。pr-9 在 Tailwind 里排在 px-3 之后，能覆盖掉右边的内边距 */
const inputClass = computed(() => `${props.class} pr-9`.trim())

const toggleLabel = computed(() => (revealed.value ? '隐藏密码' : '显示密码'))
</script>

<template>
  <div class="relative">
    <Input
      :id="props.id"
      :model-value="props.modelValue"
      :type="inputType"
      :placeholder="props.placeholder"
      :autocomplete="props.autocomplete"
      :disabled="props.disabled"
      :class="inputClass"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <button
      type="button"
      class="absolute right-2 top-1/2 -translate-y-1/2 rounded p-0.5 text-muted-foreground transition-colors hover:text-foreground"
      :title="toggleLabel"
      :aria-label="toggleLabel"
      tabindex="-1"
      @click="revealed = !revealed"
    >
      <EyeOff v-if="revealed" class="h-4 w-4" />
      <Eye v-else class="h-4 w-4" />
    </button>
  </div>
</template>

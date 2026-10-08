<template>
  <div ref="rootRef" class="relative">
    <slot name="trigger" :is-open="isOpen" :toggle="toggle" />
    <slot v-if="isOpen" name="panel" :close="close" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useDropdown } from '@/composables/useDropdown'

/**
 * 工具栏下拉菜单外壳。
 *
 * <p>把「触发按钮 + 弹出面板」这对重复结构收敛成一个组件，开关与「点击外部关闭」
 * 交给 {@link useDropdown}。根元素必须同时包含两者 —— 这是 `useDropdown` 的前提。
 *
 * <p>插槽：
 * - `trigger`：作用域参数 `{ isOpen, toggle }`，用于给按钮加激活态 class、绑定点击
 * - `panel`：作用域参数 `{ close }`，菜单项选完后调用它关闭面板
 */
const rootRef = ref<HTMLElement | null>(null)
const { isOpen, toggle, close } = useDropdown(rootRef)
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[89]" @click="emit('close')" />
    <div class="conversation-options-menu" :style="menuStyle">
      <button class="conversation-menu-option" @click="emit('rename')"><PenLine class="h-5 w-5" />重命名</button>
      <button class="conversation-menu-option" @click="emit('toggle-pin')">
        <component :is="pinned ? PinOff : Pin" class="h-5 w-5" />
        {{ pinned ? '取消置顶聊天' : '置顶聊天' }}
      </button>
      <div class="my-1 border-t border-zinc-100" />
      <button class="conversation-menu-option conversation-menu-delete" @click="emit('remove')"><Trash2 class="h-5 w-5" />删除</button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { PenLine, Pin, PinOff, Trash2 } from 'lucide-vue-next'

defineProps<{
  /** 目标会话是否已置顶，决定第二项文案与图标 */
  pinned: boolean
  /** 由触发元素位置算好的 fixed 定位样式 */
  menuStyle: Record<string, string>
}>()

const emit = defineEmits<{
  close: []
  rename: []
  'toggle-pin': []
  remove: []
}>()
</script>

<style scoped>
.conversation-options-menu {
  position: fixed;
  z-index: 91;
  width: 180px;
  border: 1px solid #dedede;
  border-radius: 12px;
  background: #ffffff;
  padding: 7px;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.14);
}

.conversation-menu-option {
  display: flex;
  height: 38px;
  width: 100%;
  align-items: center;
  gap: 10px;
  border-radius: 8px;
  padding: 0 9px;
  text-align: left;
  font-size: 14px;
  color: #303030;
}

.conversation-menu-option:hover {
  background: #f2f2f2;
}

.conversation-menu-delete {
  color: #e53935;
}

.conversation-menu-delete:hover {
  background: #fff0f0;
  color: #d32f2f;
}
</style>

<template>
  <Teleport to="body">
    <div class="rename-dialog-backdrop" @click.self="emit('close')">
      <form class="rename-dialog" @submit.prevent="submit">
        <h2>重命名聊天</h2>
        <input v-model="renameValue" autofocus maxlength="48" placeholder="输入聊天名称" />
        <footer>
          <button type="button" class="project-dialog-cancel" @click="emit('close')">取消</button>
          <button class="project-dialog-submit" :disabled="!renameValue.trim()">保存</button>
        </footer>
      </form>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  /** 会话当前标题，作为输入框初值 */
  title: string
}>()

const emit = defineEmits<{
  close: []
  confirm: [title: string]
}>()

// 弹窗由父组件用 v-if 挂载，因此在打开时取到的一定是最新标题
const renameValue = ref(props.title)

function submit(): void {
  emit('confirm', renameValue.value.trim())
}
</script>

<style scoped>
.rename-dialog-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.24);
  padding: 20px;
}

.rename-dialog {
  width: min(100%, 400px);
  border-radius: 14px;
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}

.rename-dialog h2 {
  margin: 0;
  color: #202124;
  font-size: 20px;
  font-weight: 700;
}

.rename-dialog input {
  height: 44px;
  width: 100%;
  margin-top: 18px;
  border: 1px solid #d7d7d7;
  border-radius: 9px;
  padding: 0 12px;
  color: #272727;
  font-size: 15px;
  outline: none;
}

.rename-dialog input:focus {
  border-color: #1681f8;
}

.rename-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.project-dialog-cancel,
.project-dialog-submit {
  height: 46px;
  border-radius: 13px;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 500;
}

.project-dialog-cancel {
  color: #858585;
}

.project-dialog-cancel:hover {
  background: #f1f1f1;
  color: #444444;
}

.project-dialog-submit {
  background: #202124;
  color: #ffffff;
}

.project-dialog-submit:hover:not(:disabled) {
  background: #3d3f43;
}

.project-dialog-submit:disabled {
  cursor: not-allowed;
  background: #c9c9c9;
}
</style>

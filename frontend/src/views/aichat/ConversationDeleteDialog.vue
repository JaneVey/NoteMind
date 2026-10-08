<template>
  <Teleport to="body">
    <!-- backdrop 类名沿用原文件（删除确认与重命名弹窗共用同一套 backdrop 样式） -->
    <div class="rename-dialog-backdrop" @click.self="emit('close')">
      <section class="delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-chat-title">
        <h2 id="delete-chat-title">删除聊天？</h2>
        <p>这会删除“<strong>{{ title }}</strong>”。</p>
        <footer>
          <button class="delete-dialog-cancel" @click="emit('close')">取消</button>
          <button class="delete-dialog-confirm" @click="emit('confirm')">删除</button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  /** 待删除会话的标题 */
  title: string
}>()

const emit = defineEmits<{
  close: []
  confirm: []
}>()
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

.delete-dialog {
  width: min(100%, 650px);
  border: 1px solid #bcbcbc;
  border-radius: 18px;
  background: #ffffff;
  padding: 28px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.18);
}

.delete-dialog h2 {
  margin: 0;
  color: #202124;
  font-size: 26px;
  font-weight: 700;
}

.delete-dialog p {
  margin: 24px 0 0;
  color: #303030;
  font-size: 18px;
}

.delete-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 28px;
}

.delete-dialog-cancel,
.delete-dialog-confirm {
  height: 48px;
  border-radius: 999px;
  padding: 0 22px;
  font-size: 16px;
  font-weight: 500;
}

.delete-dialog-cancel {
  border: 1px solid #dddddd;
  background: #ffffff;
  color: #333333;
}

.delete-dialog-cancel:hover {
  background: #f5f5f5;
}

.delete-dialog-confirm {
  background: #f9073e;
  color: #ffffff;
}

.delete-dialog-confirm:hover {
  background: #dc0034;
}
</style>

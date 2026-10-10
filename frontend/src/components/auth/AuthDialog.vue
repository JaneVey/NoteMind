<script setup lang="ts">
import { ref, watch } from 'vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui'
import { useAuthDialog } from '@/composables/useAuthDialog'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'

/**
 * 认证弹框 —— 登录的主路径。
 *
 * <p>做成弹框而不是跳页面，是为了不丢用户已经输入的内容：跳登录页会让刚写的 200 字消失，
 * 弹框则能在登录成功后自动把那条消息发出去。
 *
 * <p>状态由 useAuthDialog 的模块级状态驱动（全局唯一）；关闭时 settle 兑现调用方的 Promise，
 * 让 requireAuth 之后的代码得以继续执行。
 *
 * <p>用 Radix 的 Dialog 而不是手写遮罩：焦点陷阱、Esc 关闭、屏幕阅读器标注都是现成的。
 */

const { visible, tab, reason, settle, switchTab } = useAuthDialog()

/** 注册成功后带过来的用户名，用于切回登录时预填 */
const registeredUsername = ref('')

// 每次重新打开弹框都清掉上一次的预填，避免"上次注册的名字还留着"
watch(visible, (isVisible) => {
  if (isVisible) {
    registeredUsername.value = ''
  }
})

function handleRegistered(username: string): void {
  registeredUsername.value = username
  switchTab('login')
}

function handleSuccess(): void {
  // 兑现等待者：调用方据此"续做"原本被打断的动作
  settle(true)
}
</script>

<template>
  <Dialog
    :open="visible"
    @update:open="(open: boolean) => { if (!open) settle(false) }"
  >
    <DialogContent class="sm:max-w-[420px]">
      <DialogHeader>
        <DialogTitle>{{ tab === 'login' ? '登录 NoteMind' : '注册 NoteMind' }}</DialogTitle>
        <DialogDescription>
          {{ tab === 'login' ? '登录后即可保存内容、使用知识库与 AI 助手。' : '注册一个账号，开始沉淀你的知识。' }}
        </DialogDescription>
      </DialogHeader>

      <LoginForm
        v-if="tab === 'login'"
        :reason="reason"
        :initial-identifier="registeredUsername"
        @success="handleSuccess"
        @switch-to-register="switchTab('register')"
      />

      <RegisterForm
        v-else
        @registered="handleRegistered"
        @switch-to-login="switchTab('login')"
      />
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui'
import { useAuthDialog } from '@/composables/useAuthDialog'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'

/**
 * 认证弹框 —— **登录的主路径**。
 *
 * <h3>为什么必须是弹框而不是页面跳转</h3>
 *
 * 这是「游客模式」体验的分水岭。假设用户打了 200 字的需求后点发送：
 * <pre>
 *   ❌ 跳登录页：200 字丢失，登录回来还要重打   → 用户直接走人
 *   ✅ 弹框：输入内容原样还在 → 登录成功 → 自动把那条消息发出去
 * </pre>
 *
 * <p>弹框由 {@code useAuthDialog} 的模块级状态驱动，全局唯一。
 * 关闭时通过 {@link useAuthDialog} 的 settle 兑现调用方的 Promise，
 * 让 {@code requireAuth} 之后的代码得以继续执行。
 *
 * <p>用 Radix 的 Dialog 而不是手写遮罩：焦点陷阱、Esc 关闭、
 * 屏幕阅读器标注都是白送的，手写很难做对。
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

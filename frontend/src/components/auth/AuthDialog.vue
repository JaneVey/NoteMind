<script setup lang="ts">
import { ref, watch } from 'vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui'
import { useAuthDialog } from '@/composables/useAuthDialog'
import type { AuthTab } from '@/composables/useAuthDialog'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'

/**
 * 认证弹框 —— 登录的主路径。
 *
 * <p>为什么用弹框而不是跳页面：用户打了 200 字点发送，跳转会把内容丢掉。
 * 弹框保住输入，登录完成后由 requireAuth 的调用方自动续做原本的动作。
 *
 * <p>用 Radix 的 Dialog 而不是手写遮罩：焦点陷阱、Esc 关闭、屏幕阅读器标注都是现成的。
 */

const { visible, tab, reason, settle, switchTab } = useAuthDialog()

/** 注册成功后带过来的用户名，切回登录时预填 */
const registeredUsername = ref('')

const tabs: Array<{ value: AuthTab; label: string }> = [
  { value: 'login', label: '登录' },
  { value: 'register', label: '注册' },
]

// 每次重新打开都清掉上一次的预填
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
  settle(true)
}

/**
 * 打开时把焦点交给第一个输入框，而不是标签按钮。
 *
 * <p>默认行为是聚焦第一个可聚焦元素，也就是「登录」这个标签。结果有两个问题：
 * 打开弹框后直接打字没有反应（焦点在按钮上），以及标签上会出现一圈
 * 浏览器默认的聚焦轮廓，看上去像多了一个状态。
 */
function handleOpenAutoFocus(event: Event): void {
  event.preventDefault()
  // 表单这时候可能还没挂载完，等一帧再取
  requestAnimationFrame(() => {
    document.querySelector<HTMLInputElement>('#auth-identifier, #reg-username')?.focus()
  })
}
</script>

<template>
  <Dialog
    :open="visible"
    @update:open="(open: boolean) => { if (!open) settle(false) }"
  >
    <DialogContent
      class="sm:max-w-[420px]"
      @open-auto-focus="handleOpenAutoFocus"
    >
      <!-- 品牌区。标记是内联 SVG，不依赖图片资源 -->
      <DialogHeader class="space-y-0">
        <div class="flex items-center gap-3">
          <svg class="h-9 w-9 shrink-0" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="9" fill="hsl(var(--primary))" />
            <path
              d="M13.6 18.2 L18.4 13.8"
              stroke="hsl(var(--primary-foreground))"
              stroke-width="2"
              stroke-linecap="round"
            />
            <circle cx="11.5" cy="20" r="2.7" fill="hsl(var(--primary-foreground))" />
            <circle cx="20.5" cy="12" r="2.7" fill="hsl(var(--primary-foreground))" />
          </svg>
          <div class="min-w-0">
            <DialogTitle class="text-base font-semibold leading-tight text-zinc-900">
              NoteMind
            </DialogTitle>
            <DialogDescription class="mt-0.5 text-xs leading-snug text-zinc-500">
              笔记与知识库打通，AI 回答标注出处
            </DialogDescription>
          </div>
        </div>
      </DialogHeader>

      <!-- 登录 / 注册切换。做成顶部分段控件，比底部的文字链接更容易被看见 -->
      <div class="mt-5 grid grid-cols-2 gap-1 rounded-lg bg-zinc-100 p-1">
        <button
          v-for="item in tabs"
          :key="item.value"
          type="button"
          class="rounded-md px-3 py-1.5 text-sm transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-ring/40"
          :class="tab === item.value
            ? 'bg-white font-medium text-zinc-900 shadow-sm'
            : 'text-zinc-500 hover:text-zinc-800'"
          @click="switchTab(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="mt-5">
        <LoginForm
          v-if="tab === 'login'"
          :reason="reason"
          :initial-identifier="registeredUsername"
          @success="handleSuccess"
        />

        <RegisterForm
          v-else
          @registered="handleRegistered"
        />
      </div>
    </DialogContent>
  </Dialog>
</template>

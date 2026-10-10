<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { Button, Input } from '@/components/ui'
import { ApiError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'

/**
 * 登录表单。
 *
 * <p>被两处复用：{@code AuthDialog}（模态框，主路径）与独立页。
 * 两者共用同一套实现，避免"弹框能登录、独立页不能"这类走偏。
 *
 * <p>登录用单一输入框收「用户名或邮箱」—— 市面常见做法（GitHub、Notion 都支持），
 * 用户不必回忆"我当初是用哪个注册的"。
 *
 * <h3>为什么按钮不做禁用式校验</h3>
 * 见 {@code RegisterForm} 的说明：禁用按钮 + 无提示 = 用户不知道自己卡在哪。
 * 这里同样改为**按钮常可点击 + 逐字段错误提示**。
 */

const props = defineProps<{
  /**
   * 为什么现在需要登录，例如"发送消息需要先登录"。
   *
   * <p>只有**被动作打断**时才传 —— 那种场景下用户需要一个解释。
   * 从用户菜单主动点"登录"进来的不传，否则会和弹框自身的说明文字重复。
   */
  reason?: string
  /** 初始值（注册成功后切回登录时预填用户名） */
  initialIdentifier?: string
}>()

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'switch-to-register'): void
}>()

const auth = useAuthStore()

const identifier = ref(props.initialIdentifier ?? '')
const password = ref('')
const rememberMe = ref(false)

const submitting = ref(false)
const attempted = ref(false)
const serverError = ref('')

const errors = reactive({ identifier: '', password: '' })

function validateAll(): boolean {
  errors.identifier = identifier.value.trim() ? '' : '请输入用户名或邮箱'
  errors.password = password.value ? '' : '请输入密码'
  return !errors.identifier && !errors.password
}

watch([identifier, password], () => {
  if (attempted.value) {
    validateAll()
  }
})

async function handleSubmit(): Promise<void> {
  attempted.value = true
  serverError.value = ''

  if (!validateAll()) {
    return
  }

  submitting.value = true
  try {
    await auth.login({
      identifier: identifier.value.trim(),
      password: password.value,
      rememberMe: rememberMe.value,
    })
    emit('success')
  } catch (error) {
    // 后端对"账号不存在"与"密码错误"返回同一个错误码，不泄露账号是否存在。
    // 直接展示后端文案即可，不做额外区分
    serverError.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="space-y-4" novalidate @submit.prevent="handleSubmit">
    <!--
      只在被动作打断时显示。样式做成"提示条"而不是普通说明文字，
      让用户一眼看出"是因为我刚才那个操作才需要登录"
    -->
    <p
      v-if="props.reason"
      class="rounded-md border border-primary/20 bg-primary/5 px-3 py-2 text-xs text-foreground"
    >
      {{ props.reason }}
    </p>

    <div class="space-y-1.5">
      <label for="auth-identifier" class="text-sm font-medium">用户名或邮箱</label>
      <Input
        id="auth-identifier"
        v-model="identifier"
        type="text"
        autocomplete="username"
        placeholder="admin 或 admin@notemind.local"
        :disabled="submitting"
        :class="errors.identifier ? 'border-destructive' : ''"
      />
      <p v-if="errors.identifier" class="text-xs text-destructive">{{ errors.identifier }}</p>
    </div>

    <div class="space-y-1.5">
      <label for="auth-password" class="text-sm font-medium">密码</label>
      <Input
        id="auth-password"
        v-model="password"
        type="password"
        autocomplete="current-password"
        placeholder="请输入密码"
        :disabled="submitting"
        :class="errors.password ? 'border-destructive' : ''"
      />
      <p v-if="errors.password" class="text-xs text-destructive">{{ errors.password }}</p>
    </div>

    <label class="flex cursor-pointer select-none items-center gap-2 text-sm text-muted-foreground">
      <input
        v-model="rememberMe"
        type="checkbox"
        class="h-4 w-4 rounded border-input accent-primary"
        :disabled="submitting"
      />
      记住我（30 天内免登录）
    </label>

    <p v-if="serverError" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ serverError }}
    </p>

    <Button type="submit" class="w-full" :disabled="submitting">
      <Loader2 v-if="submitting" class="mr-2 h-4 w-4 animate-spin" />
      {{ submitting ? '登录中…' : '登录' }}
    </Button>

    <p class="text-center text-sm text-muted-foreground">
      还没有账号？
      <button
        type="button"
        class="font-medium text-primary underline-offset-4 hover:underline"
        @click="emit('switch-to-register')"
      >
        立即注册
      </button>
    </p>
  </form>
</template>

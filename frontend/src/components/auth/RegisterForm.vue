<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { Button, Input } from '@/components/ui'
import { ApiError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'

/**
 * 注册表单。
 *
 * <p>按钮只在提交中禁用，其余时候随时可点：初版绑的是 `:disabled="!canSubmit"`，
 * 三个字段全通过才可点，而按钮禁用时没有任何提示说明哪一项不合格 —— 填完了按钮还是灰的，
 * 完全不知道该怎么办。现在改成逐字段显示错误，并在首次提交后转为实时校验。
 *
 * <p>用户名字符集限制为 ASCII，是刻意的产品选择，与后端 `@Pattern` 一致；
 * 中文请填在"昵称"里，昵称不限字符。校验规则与后端保持一致，避免"前端过了后端拒"。
 */

const emit = defineEmits<{
  (e: 'registered', username: string): void
  (e: 'switch-to-login'): void
}>()

const auth = useAuthStore()

const username = ref('')
const email = ref('')
const password = ref('')
const nickname = ref('')

const submitting = ref(false)
const serverError = ref('')
/** 是否已经尝试过提交。首次提交前不打扰用户，提交后再实时校验 */
const attempted = ref(false)

const errors = reactive({ username: '', email: '', password: '' })

const USERNAME_PATTERN = /^[a-zA-Z0-9_-]+$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN = 8
const PASSWORD_MAX = 100

function validateAll(): boolean {
  // 用户名
  const name = username.value.trim()
  if (!name) {
    errors.username = '请输入用户名'
  } else if (name.length < 3 || name.length > 50) {
    errors.username = `用户名长度需为 3-50 个字符（当前 ${name.length} 个）`
  } else if (!USERNAME_PATTERN.test(name)) {
    errors.username = '用户名只能包含字母、数字、下划线和连字符；中文请填在"昵称"里'
  } else {
    errors.username = ''
  }

  // 邮箱
  const mail = email.value.trim()
  if (!mail) {
    errors.email = '请输入邮箱'
  } else if (!EMAIL_PATTERN.test(mail)) {
    errors.email = '邮箱格式不正确，例如 name@example.com'
  } else if (mail.length > 255) {
    errors.email = '邮箱长度不能超过 255 个字符'
  } else {
    errors.email = ''
  }

  // 密码
  if (!password.value) {
    errors.password = '请输入密码'
  } else if (password.value.length < PASSWORD_MIN) {
    errors.password = `密码至少 ${PASSWORD_MIN} 位（当前 ${password.value.length} 位）`
  } else if (password.value.length > PASSWORD_MAX) {
    errors.password = `密码不能超过 ${PASSWORD_MAX} 位`
  } else {
    errors.password = ''
  }

  return !errors.username && !errors.email && !errors.password
}

// 首次提交之后改为实时校验：改一个字符，错误立刻更新或消失
watch([username, email, password], () => {
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
    await auth.register({
      username: username.value.trim(),
      email: email.value.trim(),
      password: password.value,
      nickname: nickname.value.trim() || username.value.trim(),
    })
    emit('registered', username.value.trim())
  } catch (error) {
    serverError.value = error instanceof ApiError ? error.message : '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="space-y-4" novalidate @submit.prevent="handleSubmit">
    <div class="space-y-1.5">
      <label for="reg-username" class="text-sm font-medium">用户名</label>
      <Input
        id="reg-username"
        v-model="username"
        type="text"
        autocomplete="username"
        placeholder="3-50 位字母、数字、下划线或连字符"
        :disabled="submitting"
        :class="errors.username ? 'border-destructive' : ''"
      />
      <p v-if="errors.username" class="text-xs text-destructive">{{ errors.username }}</p>
    </div>

    <div class="space-y-1.5">
      <label for="reg-email" class="text-sm font-medium">邮箱</label>
      <Input
        id="reg-email"
        v-model="email"
        type="email"
        autocomplete="email"
        placeholder="用于找回密码"
        :disabled="submitting"
        :class="errors.email ? 'border-destructive' : ''"
      />
      <p v-if="errors.email" class="text-xs text-destructive">{{ errors.email }}</p>
    </div>

    <div class="space-y-1.5">
      <label for="reg-password" class="text-sm font-medium">密码</label>
      <Input
        id="reg-password"
        v-model="password"
        type="password"
        autocomplete="new-password"
        :placeholder="`至少 ${PASSWORD_MIN} 位`"
        :disabled="submitting"
        :class="errors.password ? 'border-destructive' : ''"
      />
      <p v-if="errors.password" class="text-xs text-destructive">{{ errors.password }}</p>
      <!-- 实时提示：不必猜"还差多少" -->
      <p v-else-if="password.length > 0 && password.length < PASSWORD_MIN" class="text-xs text-muted-foreground">
        还需 {{ PASSWORD_MIN - password.length }} 位
      </p>
    </div>

    <div class="space-y-1.5">
      <label for="reg-nickname" class="text-sm font-medium">
        昵称 <span class="font-normal text-muted-foreground">（可选，可用中文）</span>
      </label>
      <Input
        id="reg-nickname"
        v-model="nickname"
        type="text"
        placeholder="不填则与用户名相同"
        :disabled="submitting"
      />
    </div>

    <p v-if="serverError" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ serverError }}
    </p>

    <!-- 按钮只在提交中禁用：点了就会看到具体哪里不合格 -->
    <Button type="submit" class="w-full" :disabled="submitting">
      <Loader2 v-if="submitting" class="mr-2 h-4 w-4 animate-spin" />
      {{ submitting ? '注册中…' : '注册' }}
    </Button>

    <p class="text-center text-sm text-muted-foreground">
      已有账号？
      <button
        type="button"
        class="font-medium text-primary underline-offset-4 hover:underline"
        @click="emit('switch-to-login')"
      >
        去登录
      </button>
    </p>
  </form>
</template>

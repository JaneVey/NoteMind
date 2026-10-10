<script setup lang="ts">
import { computed, ref } from 'vue'
import { Loader2 } from 'lucide-vue-next'
import { Button, Input } from '@/components/ui'
import { ApiError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'

/**
 * 注册表单。
 *
 * <p><b>邮箱必填</b>：它是账号找回的唯一途径。没有邮箱的账号一旦忘记密码就只能作废。
 * 但**不强制邮箱验证** —— 未验证的账号先可用，界面引导验证
 * （GitHub、Notion 都是这个策略，避免注册门槛过高）。
 *
 * <p>注册成功后**不自动登录**，而是切回登录标签并预填用户名 ——
 * 让用户清楚地知道"注册成功了，现在请登录"，而不是被莫名地跳来跳去。
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
const errorMessage = ref('')

/** 前端先做基本校验，减少无意义的请求；真正的判定仍在后端 */
const usernameValid = computed(() => /^[a-zA-Z0-9_-]{3,50}$/.test(username.value))
const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value))
const passwordValid = computed(() => password.value.length >= 8 && password.value.length <= 100)

const canSubmit = computed(
  () => usernameValid.value && emailValid.value && passwordValid.value && !submitting.value,
)

async function handleSubmit(): Promise<void> {
  errorMessage.value = ''

  if (!usernameValid.value) {
    errorMessage.value = '用户名需为 3-50 个字符，只能包含字母、数字、下划线和连字符'
    return
  }
  if (!emailValid.value) {
    errorMessage.value = '请输入正确的邮箱地址'
    return
  }
  if (!passwordValid.value) {
    errorMessage.value = '密码长度需为 8-100 个字符'
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
    errorMessage.value = error instanceof ApiError ? error.message : '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="space-y-4" @submit.prevent="handleSubmit">
    <div class="space-y-1.5">
      <label for="reg-username" class="text-sm font-medium">用户名</label>
      <Input
        id="reg-username"
        v-model="username"
        type="text"
        autocomplete="username"
        placeholder="3-50 位字母、数字、下划线或连字符"
        :disabled="submitting"
      />
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
      />
    </div>

    <div class="space-y-1.5">
      <label for="reg-password" class="text-sm font-medium">密码</label>
      <Input
        id="reg-password"
        v-model="password"
        type="password"
        autocomplete="new-password"
        placeholder="至少 8 位"
        :disabled="submitting"
      />
    </div>

    <div class="space-y-1.5">
      <label for="reg-nickname" class="text-sm font-medium">
        昵称 <span class="font-normal text-muted-foreground">（可选）</span>
      </label>
      <Input
        id="reg-nickname"
        v-model="nickname"
        type="text"
        placeholder="不填则与用户名相同"
        :disabled="submitting"
      />
    </div>

    <p v-if="errorMessage" class="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
      {{ errorMessage }}
    </p>

    <Button type="submit" class="w-full" :disabled="!canSubmit">
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

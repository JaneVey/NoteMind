<template>
  <div class="flex h-screen w-screen">
    <!-- Left: Brand Showcase -->
    <div
      class="hidden w-1/2 md:flex flex-col items-center justify-center
             bg-gradient-to-br from-violet-600 via-purple-700 to-indigo-900
             text-white p-12"
    >
      <div class="text-center mb-12">
        <div class="text-6xl mb-4">&#9670;</div>
        <h1 class="text-4xl font-bold mb-2">NoteMind</h1>
        <p class="text-lg text-purple-200">基于 RAG 的智能知识管理平台</p>
      </div>

      <div class="space-y-6 w-full max-w-sm">
        <div class="flex items-center gap-4">
          <div
            class="flex items-center justify-center w-10 h-10 rounded-lg bg-white/10"
          >
            <BookOpen class="w-5 h-5" />
          </div>
          <div>
            <p class="font-medium">智能笔记</p>
            <p class="text-sm text-purple-200">随时随地记录灵感</p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <div
            class="flex items-center justify-center w-10 h-10 rounded-lg bg-white/10"
          >
            <Brain class="w-5 h-5" />
          </div>
          <div>
            <p class="font-medium">AI 助手</p>
            <p class="text-sm text-purple-200">智能问答与内容生成</p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <div
            class="flex items-center justify-center w-10 h-10 rounded-lg bg-white/10"
          >
            <Sparkles class="w-5 h-5" />
          </div>
          <div>
            <p class="font-medium">知识库</p>
            <p class="text-sm text-purple-200">构建个人知识体系</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Register Card -->
    <div
      class="flex-1 flex items-center justify-center
             bg-gradient-to-br from-slate-50 to-slate-100 p-8"
    >
      <Card class="w-96">
        <CardHeader class="text-center">
          <CardTitle class="text-2xl">创建账号</CardTitle>
          <CardDescription>注册 NoteMind 账号</CardDescription>
        </CardHeader>

        <CardContent class="space-y-4">
          <div class="space-y-2">
            <Input
              v-model="username"
              placeholder="用户名"
              :disabled="loading"
            />
          </div>
          <div class="space-y-2">
            <Input
              v-model="nickname"
              placeholder="昵称（可选）"
              :disabled="loading"
            />
          </div>
          <div class="space-y-2">
            <Input
              v-model="email"
              type="email"
              placeholder="邮箱（可选）"
              :disabled="loading"
            />
          </div>
          <div class="space-y-2">
            <Input
              v-model="password"
              type="password"
              placeholder="密码（至少 6 位）"
              :disabled="loading"
            />
          </div>
          <div class="space-y-2">
            <Input
              v-model="confirmPassword"
              type="password"
              placeholder="确认密码"
              :disabled="loading"
            />
          </div>

          <div
            v-if="errorMessage"
            class="text-sm text-red-500"
          >
            {{ errorMessage }}
          </div>

          <Button
            class="w-full"
            :disabled="loading"
            @click="handleRegister"
          >
            <span
              v-if="loading"
              class="inline-block w-4 h-4 border-2 border-white border-t-transparent
                     rounded-full animate-spin mr-2"
            />
            {{ loading ? '注册中...' : '注册' }}
          </Button>
        </CardContent>

        <CardFooter class="justify-center">
          <p class="text-sm text-muted-foreground">
            已有账号？
            <router-link
              to="/login"
              class="text-primary font-medium hover:underline"
            >
              去登录
            </router-link>
          </p>
        </CardFooter>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, Brain, Sparkles } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/authStore'
import {
  Button,
  Input,
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from '@/components/ui'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const nickname = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleRegister() {
  errorMessage.value = ''

  if (!username.value) {
    errorMessage.value = '请输入用户名'
    return
  }

  if (!password.value) {
    errorMessage.value = '请输入密码'
    return
  }

  if (password.value.length < 6) {
    errorMessage.value = '密码长度不能少于 6 位'
    return
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true

  try {
    await authStore.register(
      username.value,
      password.value,
      email.value,
      nickname.value,
    )
    router.push('/login')
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error ? err.message : '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

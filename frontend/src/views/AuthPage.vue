<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'

/**
 * 认证独立页。
 *
 * <p>弹框是主路径，这个页面是**次要路径**：直接访问 `/login` 链接、
 * 邮件里的跳转、以及 OAuth 回调失败时都会落到这里。
 *
 * <p>它复用与弹框**完全相同**的表单组件 —— 两套实现走偏是常见问题
 * （"弹框能登录、独立页不能"）。
 */

const route = useRoute()
const router = useRouter()

const isRegister = computed(() => route.name === 'Register')

/** 登录成功后回到原目标页 */
function goAfterAuth(): void {
  const redirect = route.query.redirect
  // 只接受站内路径：以单个 / 开头，排除 //evil.com 这类协议相对 URL
  const isSafe = typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
  void router.replace(isSafe ? (redirect as string) : '/')
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-[#fdfdfc] px-4">
    <div class="w-full max-w-[420px]">
      <!-- 品牌区 -->
      <div class="mb-8 text-center">
        <h1 class="text-2xl font-semibold tracking-tight text-zinc-900">NoteMind</h1>
        <p class="mt-1.5 text-sm text-zinc-500">基于 RAG 的智能知识管理平台</p>
      </div>

      <div class="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
        <h2 class="mb-1 text-lg font-medium text-zinc-900">
          {{ isRegister ? '注册账号' : '登录' }}
        </h2>
        <p class="mb-5 text-sm text-zinc-500">
          {{ isRegister ? '注册后即可保存内容、使用知识库与 AI 助手。' : '欢迎回来。' }}
        </p>

        <LoginForm
          v-if="!isRegister"
          @success="goAfterAuth"
          @switch-to-register="router.push('/register')"
        />

        <RegisterForm
          v-else
          @registered="router.push({ name: 'Login', query: route.query })"
          @switch-to-login="router.push({ name: 'Login', query: route.query })"
        />
      </div>
    </div>
  </div>
</template>

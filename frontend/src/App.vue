<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AuthDialog from '@/components/auth/AuthDialog.vue'
import { useAuthStore } from '@/stores/authStore'

/**
 * 应用根组件。
 *
 * <h3>为什么这里要等一次"启动恢复"</h3>
 *
 * Access Token 只存在内存里（这是双 Token 方案防 XSS 的关键），
 * 所以刷新页面后内存是空的。恢复登录态的办法是**无条件调一次 /auth/refresh**：
 * refresh token 在 HttpOnly Cookie 里，浏览器会自动带上。
 *
 * <p>如果不等这个结果就渲染，用户会看到"先显示未登录、再跳成已登录"的闪烁。
 * 所以这里先渲染一个极简占位，恢复完成后再渲染真正的内容。
 *
 * <p><b>为什么不在 main.ts 里 await 后再 mount</b>：那会让整个首屏被一次网络请求阻塞，
 * 后端不可用时页面会长时间空白。放在这里既能消除闪烁，又不阻塞挂载。
 */

const auth = useAuthStore()

/** 启动恢复是否完成 */
const ready = ref(false)

onMounted(async () => {
  try {
    await auth.bootstrap()
  } finally {
    // 即使恢复失败（后端不可用等）也要放行，让用户能看到界面
    ready.value = true
  }
})
</script>

<template>
  <template v-if="ready">
    <router-view />

    <!--
      认证弹框挂在应用根部，全局唯一。
      它由 useAuthDialog 的模块级状态驱动 —— 任何地方调用 requireAuth()
      都能唤起它，不必在每个页面重复挂载。
    -->
    <AuthDialog />
  </template>

  <!-- 启动占位：只在这一瞬间可见 -->
  <div v-else class="flex min-h-screen items-center justify-center bg-[#fdfdfc]">
    <div class="text-sm text-zinc-400">正在恢复登录状态…</div>
  </div>
</template>

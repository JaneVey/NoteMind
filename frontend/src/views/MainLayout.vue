<template>
  <div class="flex h-screen overflow-hidden bg-[#f7f7f6] text-zinc-800">
    <aside class="flex w-[60px] shrink-0 flex-col items-center bg-[#eeeeed] text-zinc-500">
      <div class="relative pt-3" ref="userMenuRef">
        <button
          class="flex h-8 w-8 items-center justify-center overflow-hidden rounded-md bg-zinc-800 text-[11px] font-semibold text-white shadow-sm transition-colors hover:bg-zinc-700"
          title="个人中心"
          @click="showUserMenu = !showUserMenu"
        >
          {{ avatarText }}
        </button>

        <div
          v-if="showUserMenu"
          class="absolute left-full top-3 z-50 ml-2 w-44 overflow-hidden rounded-lg border border-zinc-100 bg-white text-zinc-700 shadow-lg"
        >
          <div class="border-b border-zinc-100 px-3 py-2 text-xs text-zinc-400">
            {{ authStore.userInfo?.nickname || authStore.userInfo?.username || '原型用户' }}
          </div>
          <button class="menu-row" @click="openProfileDialog">修改资料</button>
          <button class="menu-row" @click="router.push('/settings')">设置</button>
          <div class="border-t border-zinc-100" />
          <button class="menu-row text-red-500 hover:bg-red-50" @click="handleLogout">退出登录</button>
        </div>
      </div>

      <nav class="mt-7 flex flex-1 flex-col items-center gap-4">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="isActive(item.path) ? 'text-zinc-900' : 'hover:text-zinc-700'"
          :title="item.label"
        >
          <component :is="item.icon" class="h-[22px] w-[22px]" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="flex w-full flex-col items-center gap-7 pb-5">
        <button
          class="text-zinc-600 transition-colors hover:text-zinc-900"
          title="更多菜单"
          @click="showOptionsPanel = true"
        >
          <Menu class="h-[24px] w-[24px]" />
        </button>
      </div>
    </aside>

    <main class="min-w-0 flex-1 overflow-hidden bg-white">
      <router-view />
    </main>
  </div>

  <div
    v-if="showOptionsPanel"
    class="fixed inset-0 z-[100] flex items-start justify-center bg-black/20"
    @click.self="showOptionsPanel = false"
  >
    <div class="mt-16 w-[900px] max-h-[80vh] overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-2xl text-zinc-800 flex flex-col">
      <header class="flex items-center justify-between shrink-0 border-b border-zinc-200 px-8 py-5">
        <h2 class="text-xl font-semibold">设置</h2>
        <button class="text-zinc-500 transition-colors hover:text-zinc-900" @click="showOptionsPanel = false">
          <X class="h-5 w-5" />
        </button>
      </header>
      <div class="flex min-h-0 flex-1">
        <aside class="w-[220px] shrink-0 border-r border-zinc-100 p-4">
          <button
            v-for="item in optionItems"
            :key="item.label"
            class="option-row"
            :class="item.active ? 'bg-zinc-100 text-zinc-900' : 'text-zinc-600 hover:bg-zinc-50'"
          >
            <component :is="item.icon" class="h-[18px] w-[18px]" />
            {{ item.label }}
          </button>
        </aside>
        <main class="flex-1 overflow-y-auto px-8 py-6">
          <section class="max-w-[640px] space-y-6">
            <div>
              <h3 class="text-lg font-medium text-zinc-900">版本 1.0.0</h3>
              <p class="mt-1 text-sm text-zinc-500">NoteMind 原型版本：1.0.0</p>
              <a class="mt-1 block text-sm text-violet-600 hover:underline">阅读更新日志</a>
              <Button class="mt-2" size="sm">检查更新</Button>
            </div>

            <hr class="border-zinc-100" />

            <div class="flex items-center justify-between">
              <div>
                <h4 class="text-base font-medium text-zinc-900">自动更新</h4>
                <p class="mt-0.5 text-sm text-zinc-500">关闭后 NoteMind 将不会自动更新。</p>
              </div>
              <label class="relative inline-flex h-7 w-12 shrink-0 items-center rounded-full bg-violet-500 px-0.5">
                <span class="h-5 w-5 translate-x-5 rounded-full bg-white shadow" />
              </label>
            </div>

            <div class="flex items-center justify-between">
              <div>
                <h4 class="text-base font-medium text-zinc-900">语言</h4>
                <p class="mt-0.5 text-sm text-zinc-500">更改界面语言。</p>
              </div>
              <select class="h-9 w-44 rounded-lg border border-zinc-200 bg-white px-3 text-sm outline-none">
                <option>简体中文</option>
              </select>
            </div>

            <div class="flex items-center justify-between">
              <div>
                <h4 class="text-base font-medium text-zinc-900">获取帮助</h4>
                <p class="mt-0.5 text-sm text-zinc-500">获取关于使用 NoteMind 的帮助。</p>
              </div>
              <Button variant="outline" size="sm">打开</Button>
            </div>

            <hr class="border-zinc-100" />

            <div>
              <h3 class="text-lg font-medium text-zinc-900">账户</h3>
            </div>

            <div class="flex items-center justify-between">
              <div>
                <h4 class="text-base font-medium text-zinc-900">你的账户</h4>
                <p class="mt-0.5 text-sm text-zinc-500">当前登录的账户为 {{ authStore.userInfo?.nickname || 'JanVey' }}。</p>
              </div>
              <div class="flex gap-2">
                <Button variant="outline" size="sm">管理</Button>
                <Button variant="outline" size="sm" class="text-red-500" @click="handleLogout">退出登录</Button>
              </div>
            </div>

            <div class="flex items-center justify-between">
              <div>
                <h4 class="text-base font-medium text-zinc-900">个人许可证</h4>
                <p class="mt-0.5 text-sm text-zinc-500">你尚未购买个人许可证。</p>
              </div>
              <Button size="sm">购买</Button>
            </div>
          </section>
        </main>
      </div>
    </div>
  </div>

  <Dialog v-model:open="showProfileDialog">
    <DialogContent class="sm:max-w-[420px]">
      <DialogHeader>
        <DialogTitle>修改资料</DialogTitle>
        <DialogDescription>原型阶段只保留最小资料编辑入口。</DialogDescription>
      </DialogHeader>

      <div class="space-y-4 py-2">
        <div class="flex items-center gap-4">
          <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-zinc-800 text-lg font-bold text-white">
            {{ avatarText }}
          </div>
          <div class="text-sm text-zinc-500">头像根据昵称首字母生成。</div>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-zinc-700">昵称</label>
          <Input v-model="profileForm.nickname" placeholder="输入昵称" maxlength="20" />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-zinc-700">用户名</label>
          <Input :model-value="authStore.userInfo?.username || 'prototype-user'" disabled />
        </div>
      </div>

      <div class="flex justify-end gap-2 pt-2">
        <Button variant="outline" @click="showProfileDialog = false">取消</Button>
        <Button :disabled="!profileForm.nickname.trim() || saving" @click="handleSaveProfile">
          {{ saving ? '保存中...' : '保存' }}
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CircleUserRound,
  FileCog,
  Keyboard,
  Library,
  Menu,
  MessageSquare,
  Network,
  Palette,
  PenLine,
  Plug,
  Puzzle,
  ShieldCheck,
  X,
} from 'lucide-vue-next'
import { updateUserProfile } from '@/api/user'
import { useAuthStore } from '@/stores/authStore'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import DialogContent from '@/components/ui/DialogContent.vue'
import DialogDescription from '@/components/ui/DialogDescription.vue'
import DialogHeader from '@/components/ui/DialogHeader.vue'
import DialogTitle from '@/components/ui/DialogTitle.vue'
import Input from '@/components/ui/Input.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const showUserMenu = ref(false)
const userMenuRef = ref(null)
const showOptionsPanel = ref(false)
const showProfileDialog = ref(false)
const saving = ref(false)
const profileForm = reactive({ nickname: '' })

const menuItems = [
  { path: '/chat', icon: MessageSquare, label: 'AI助手' },
  { path: '/notebook', icon: PenLine, label: '笔记' },
  { path: '/knowledge', icon: Library, label: '知识库' },
  { path: '/graph', icon: Network, label: '图谱' },
]

const optionItems = [
  { label: '关于', icon: CircleUserRound, active: true },
  { label: '编辑器', icon: PenLine },
  { label: '文件与链接', icon: FileCog },
  { label: '外观', icon: Palette },
  { label: '快捷键', icon: Keyboard },
  { label: '钥匙串', icon: ShieldCheck },
  { label: '核心插件', icon: Plug },
  { label: '第三方插件', icon: Puzzle },
]

const avatarText = computed(() => {
  const nick = authStore.userInfo?.nickname || authStore.userInfo?.username
  return nick ? nick.charAt(0).toUpperCase() : 'J'
})

function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function openProfileDialog() {
  showUserMenu.value = false
  profileForm.nickname = authStore.userInfo?.nickname || 'JanVey'
  showProfileDialog.value = true
}

async function handleSaveProfile() {
  const name = profileForm.nickname.trim()
  if (!name || saving.value) return
  saving.value = true
  try {
    if (authStore.getToken()) {
      await updateUserProfile({ nickname: name })
    }
    authStore.userInfo = { ...(authStore.userInfo || {}), nickname: name }
    localStorage.setItem('notemind_user', JSON.stringify(authStore.userInfo))
    showProfileDialog.value = false
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function handleClickOutside(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  authStore.initFromStorage()
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.nav-item {
  display: flex;
  width: 58px;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  line-height: 1.2;
  transition: color 0.18s ease;
}

.menu-row {
  width: 100%;
  padding: 8px 12px;
  text-align: left;
  font-size: 14px;
  transition: background-color 0.18s ease;
}

.menu-row:hover {
  background: #f7f7f7;
}

.option-row {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;
  border-radius: 6px;
  padding: 8px 12px;
  text-align: left;
  font-size: 22px;
  line-height: 1.25;
  transition: background-color 0.18s ease, color 0.18s ease;
}
</style>

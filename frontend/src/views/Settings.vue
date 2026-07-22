<template>
  <div class="p-6 max-w-[1000px] mx-auto">
    <!-- Toast notification -->
    <div
      v-if="toast.visible"
      class="fixed top-4 right-4 z-[100] px-4 py-3 rounded-lg shadow-lg text-sm font-medium border"
      :class="toast.type === 'success'
        ? 'bg-green-50 text-green-800 border-green-200 dark:bg-green-950 dark:text-green-200 dark:border-green-800'
        : 'bg-red-50 text-red-800 border-red-200 dark:bg-red-950 dark:text-red-200 dark:border-red-800'"
    >
      {{ toast.message }}
    </div>

    <h2 class="text-xl font-semibold text-foreground mb-6">设置</h2>

    <div class="flex gap-6">
      <!-- ========== 左侧 Tab 按钮组 ========== -->
      <div class="flex flex-col w-44 shrink-0 space-y-1">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          class="text-left px-3 py-2 rounded-md text-sm transition-colors"
          :class="activeTab === tab.key
            ? 'bg-primary/10 text-primary font-medium'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground'"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- ========== 右侧内容区 ========== -->
      <div class="flex-1 min-w-0">

        <!-- ======== Tab1: AI 模型 ======== -->
        <div v-if="activeTab === 'model'" class="space-y-4">
          <h3 class="text-base font-semibold text-foreground m-0">供应商配置</h3>

          <!-- 供应商卡片列表 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Card
              v-for="provider in providers"
              :key="provider.id"
              :class="provider.isActive
                ? 'border-primary ring-1 ring-primary/20 bg-primary/[0.03]'
                : ''"
            >
              <CardContent class="p-4">
                <div class="flex items-center justify-between mb-3">
                  <span class="font-semibold text-sm text-foreground">{{ provider.name }}</span>
                  <Badge v-if="provider.isActive" variant="default" class="text-xs">当前活跃</Badge>
                </div>
                <div class="text-xs text-muted-foreground space-y-1 mb-3">
                  <div>
                    <span class="text-muted-foreground/60">对话模型：</span>
                    {{ provider.chatModel || '-' }}
                  </div>
                  <div>
                    <span class="text-muted-foreground/60">Embedding：</span>
                    {{ provider.embeddingModel || '-' }}
                  </div>
                </div>
                <div class="flex gap-1.5">
                  <Button
                    v-if="!provider.isActive"
                    variant="outline"
                    size="sm"
                    @click="setActive(provider)"
                  >
                    激活
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    @click="editProvider(provider)"
                  >
                    编辑
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    class="text-destructive hover:bg-destructive/10"
                    @click="confirmDeleteProvider(provider)"
                  >
                    删除
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          <Button @click="showAddProvider = true">
            添加供应商
          </Button>

          <!-- 添加供应商 Dialog：选择类型 -->
          <Dialog v-model:open="showAddProvider">
            <DialogContent class="max-w-sm">
              <DialogHeader>
                <DialogTitle>选择供应商类型</DialogTitle>
                <DialogDescription>选择要添加的 AI 供应商</DialogDescription>
              </DialogHeader>
              <div class="flex justify-center gap-3 py-4">
                <button
                  v-for="type in providerTypes"
                  :key="type.value"
                  @click="newProviderType = type.value"
                  class="px-5 py-2.5 rounded-md text-sm font-medium transition-colors border"
                  :class="newProviderType === type.value
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-input text-muted-foreground hover:border-primary/50 hover:text-foreground'"
                >
                  {{ type.label }}
                </button>
              </div>
              <div class="flex justify-end gap-3">
                <Button variant="outline" @click="showAddProvider = false">取消</Button>
                <Button @click="confirmAddProvider">下一步</Button>
              </div>
            </DialogContent>
          </Dialog>

          <!-- 编辑供应商 Dialog -->
          <Dialog v-model:open="providerDialogVisible">
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{{ editingProvider ? '编辑供应商' : '配置供应商' }}</DialogTitle>
                <DialogDescription>配置 API 连接信息</DialogDescription>
              </DialogHeader>
              <div class="space-y-4 py-2">
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">供应商名称</label>
                  <Input v-model="providerForm.name" disabled />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">API Key</label>
                  <Input v-model="providerForm.apiKey" type="password" placeholder="输入 API Key" />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">Base URL</label>
                  <Input v-model="providerForm.baseUrl" placeholder="https://api.example.com" />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">对话模型</label>
                  <Input v-model="providerForm.chatModel" placeholder="如 deepseek-chat" />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">Embedding 模型</label>
                  <Input v-model="providerForm.embeddingModel" placeholder="如 text-embedding-ada-002" />
                </div>
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <Button variant="outline" @click="providerDialogVisible = false">取消</Button>
                <Button @click="saveProvider">保存</Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>

        <!-- ======== Tab2: AI 参数 ======== -->
        <div v-if="activeTab === 'params'" class="space-y-4 max-w-md">
          <h3 class="text-base font-semibold text-foreground m-0">对话参数</h3>

          <div class="space-y-6">
            <div class="space-y-3">
              <label class="text-sm font-medium text-foreground">
                Temperature（随机性）
              </label>
              <div class="flex items-center gap-3">
                <input
                  v-model.number="aiParams.temperature"
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  class="flex-1 h-2 bg-muted rounded-full appearance-none cursor-pointer accent-primary
                    [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4
                    [&::-webkit-slider-thumb]:bg-primary [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:shadow"
                />
                <span class="text-sm text-muted-foreground min-w-8 text-right tabular-nums">
                  {{ aiParams.temperature.toFixed(1) }}
                </span>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">最大 Token 数</label>
              <Input
                v-model.number="aiParams.maxTokens"
                type="number"
                :min="64"
                :max="8192"
                :step="64"
                class="w-40"
              />
            </div>

            <Button @click="saveAiParams">保存参数</Button>
          </div>
        </div>

        <!-- ======== Tab3: Prompt 指令 ======== -->
        <div v-if="activeTab === 'prompt'" class="space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold text-foreground m-0">快捷指令</h3>
            <Button size="sm" @click="openNewPrompt">新建指令</Button>
          </div>

          <!-- 指令列表 -->
          <div v-if="promptShortcuts.length" class="divide-y border rounded-lg">
            <div
              v-for="item in promptShortcuts"
              :key="item.id"
              class="flex items-center gap-3 px-4 py-3 hover:bg-muted/50 transition-colors"
            >
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-foreground truncate">{{ item.name }}</div>
                <div class="text-xs text-muted-foreground truncate mt-0.5">{{ item.prompt }}</div>
              </div>
              <div class="shrink-0">
                <Badge
                  :variant="item.isPreset ? 'secondary' : 'outline'"
                  class="text-xs"
                >
                  {{ item.isPreset ? '系统预设' : '自定义' }}
                </Badge>
              </div>
              <div class="shrink-0 flex gap-0.5">
                <Button variant="ghost" size="sm" @click="editPrompt(item)">编辑</Button>
                <Button
                  v-if="!item.isPreset"
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:bg-destructive/10"
                  @click="deletePrompt(item.id)"
                >
                  删除
                </Button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="flex flex-col items-center justify-center py-12 text-center border rounded-lg">
            <p class="text-sm text-muted-foreground">暂无快捷指令</p>
          </div>

          <!-- 新建/编辑 Prompt Dialog -->
          <Dialog v-model:open="showPromptDialog">
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{{ editingPrompt ? '编辑指令' : '新建指令' }}</DialogTitle>
                <DialogDescription>配置 Prompt 快捷指令</DialogDescription>
              </DialogHeader>
              <div class="space-y-4 py-2">
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">名称</label>
                  <Input v-model="promptForm.name" placeholder="指令名称" />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-medium text-foreground">指令内容</label>
                  <textarea
                    v-model="promptForm.prompt"
                    rows="5"
                    placeholder="输入 Prompt 指令内容"
                    class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring resize-y min-h-[100px]"
                  />
                </div>
              </div>
              <div class="flex justify-end gap-3 pt-2">
                <Button variant="outline" @click="closePromptDialog">取消</Button>
                <Button @click="savePrompt">保存</Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>

        <!-- ======== Tab4: 用户画像 ======== -->
        <div v-if="activeTab === 'profile'" class="space-y-4 max-w-md">
          <div>
            <h3 class="text-base font-semibold text-foreground m-0">AI 个性化设定</h3>
            <p class="text-sm text-muted-foreground mt-1">AI 将根据以下信息调整回答风格和内容</p>
          </div>

          <div class="space-y-5">
            <!-- 启用开关 -->
            <div class="flex items-center justify-between">
              <label class="text-sm font-medium text-foreground">启用画像</label>
              <button
                type="button"
                role="switch"
                :aria-checked="userProfile.enabled"
                :class="userProfile.enabled
                  ? 'bg-primary'
                  : 'bg-input'"
                class="relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                @click="userProfile.enabled = !userProfile.enabled"
              >
                <span
                  :class="userProfile.enabled ? 'translate-x-4' : 'translate-x-0'"
                  class="pointer-events-none block h-4 w-4 rounded-full bg-background shadow transition-transform"
                />
              </button>
            </div>

            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">身份</label>
              <Input v-model="userProfile.identity" placeholder="如：软件工程专业大三学生" />
            </div>

            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">技术方向</label>
              <textarea
                v-model="userProfile.techStack"
                rows="3"
                placeholder="如：Java 后端、Spring Boot、Vue3"
                class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring resize-y min-h-[80px]"
              />
            </div>

            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">回答偏好</label>
              <textarea
                v-model="userProfile.preferences"
                rows="3"
                placeholder="如：偏爱工程化解释，结合实际项目场景"
                class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring resize-y min-h-[80px]"
              />
            </div>

            <Button @click="saveProfile">保存画像</Button>
          </div>
        </div>

        <!-- ======== Tab5: 基础设置 ======== -->
        <div v-if="activeTab === 'general'" class="space-y-4 max-w-md">
          <h3 class="text-base font-semibold text-foreground m-0">基础设置</h3>

          <div class="space-y-5">
            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">主题</label>
              <div class="flex gap-2">
                <button
                  @click="systemConfig.theme = 'light'"
                  class="inline-flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium transition-colors border"
                  :class="systemConfig.theme === 'light'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-input text-muted-foreground hover:border-primary/50 hover:text-foreground'"
                >
                  <Sun class="w-4 h-4" />
                  亮色模式
                </button>
                <button
                  @click="systemConfig.theme = 'dark'"
                  class="inline-flex items-center gap-1.5 px-4 py-2 rounded-md text-sm font-medium transition-colors border"
                  :class="systemConfig.theme === 'dark'
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-input text-muted-foreground hover:border-primary/50 hover:text-foreground'"
                >
                  <Moon class="w-4 h-4" />
                  暗色模式
                </button>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-sm font-medium text-foreground">字体大小</label>
              <Input
                v-model.number="systemConfig.fontSize"
                type="number"
                :min="12"
                :max="24"
                :step="1"
                class="w-24"
              />
            </div>

            <Separator />

            <div>
              <h4 class="text-sm font-medium text-foreground mb-3">个人信息</h4>
              <div class="space-y-3">
                <div class="space-y-1.5">
                  <label class="text-xs text-muted-foreground">用户名</label>
                  <Input v-model="userInfo.username" disabled />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs text-muted-foreground">昵称</label>
                  <Input v-model="userInfo.nickname" disabled />
                </div>
                <div class="space-y-1.5">
                  <label class="text-xs text-muted-foreground">邮箱</label>
                  <Input v-model="userInfo.email" disabled />
                </div>
              </div>
            </div>

            <Button @click="saveSystemConfig">保存设置</Button>
          </div>
        </div>

      </div>
    </div>

    <!-- 全局确认 Dialog -->
    <Dialog v-model:open="confirmDialog.visible">
      <DialogContent class="max-w-sm">
        <DialogHeader>
          <DialogTitle>提示</DialogTitle>
        </DialogHeader>
        <p class="text-sm text-foreground py-3">{{ confirmDialog.message }}</p>
        <div class="flex justify-end gap-3">
          <Button variant="outline" @click="confirmDialog.visible = false">取消</Button>
          <Button variant="destructive" @click="handleConfirm">确定</Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Sun, Moon } from 'lucide-vue-next'
import {
  Button,
  Input,
  Badge,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Separator,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

type ToastType = 'success' | 'error'
type AiProvider = { id: number; name: string; isActive: boolean; chatModel: string; embeddingModel: string; baseUrl: string; apiKey: string }
type PromptShortcut = { id: number; name: string; prompt: string; isPreset: boolean }

/* ========== Tab ========== */
const activeTab = ref('model')

const tabs = [
  { key: 'model', label: 'AI 模型' },
  { key: 'params', label: 'AI 参数' },
  { key: 'prompt', label: 'Prompt 指令' },
  { key: 'profile', label: '用户画像' },
  { key: 'general', label: '基础设置' },
]

/* ========== Toast ========== */
const toast = reactive<{ visible: boolean; message: string; type: ToastType }>({
  visible: false,
  message: '',
  type: 'success',
})

function showToast(message: string, type: ToastType = 'success') {
  toast.message = message
  toast.type = type
  toast.visible = true
  setTimeout(() => { toast.visible = false }, 3000)
}

/* ========== Confirm ========== */
const confirmDialog = reactive<{ visible: boolean; message: string; onConfirm: (() => void) | null }>({
  visible: false,
  message: '',
  onConfirm: null,
})

function showConfirm(message: string, onConfirm: () => void) {
  confirmDialog.message = message
  confirmDialog.onConfirm = onConfirm
  confirmDialog.visible = true
}

function handleConfirm() {
  if (confirmDialog.onConfirm) {
    confirmDialog.onConfirm()
  }
  confirmDialog.visible = false
  confirmDialog.onConfirm = null
}

/* ========== AI 供应商 ========== */
const providers = ref<AiProvider[]>([
  { id: 1, name: 'DeepSeek', isActive: true, chatModel: 'deepseek-chat', embeddingModel: 'text-embedding-ada-002', baseUrl: 'https://api.deepseek.com', apiKey: '' },
  { id: 2, name: 'OpenAI', isActive: false, chatModel: 'gpt-4o', embeddingModel: 'text-embedding-3-small', baseUrl: 'https://api.openai.com', apiKey: '' },
])

const providerTypes = [
  { value: 'deepseek', label: 'DeepSeek' },
  { value: 'openai', label: 'OpenAI' },
  { value: 'gemini', label: 'Gemini' },
]

const showAddProvider = ref(false)
const newProviderType = ref('deepseek')
const providerDialogVisible = ref(false)
const editingProvider = ref<AiProvider | null>(null)
const providerForm = reactive({
  name: '',
  apiKey: '',
  baseUrl: '',
  chatModel: '',
  embeddingModel: '',
})

function setActive(provider: AiProvider) {
  providers.value.forEach((p) => (p.isActive = false))
  provider.isActive = true
  showToast(`已切换至 ${provider.name}`)
}

function editProvider(provider: AiProvider) {
  editingProvider.value = provider
  Object.assign(providerForm, {
    name: provider.name,
    apiKey: provider.apiKey,
    baseUrl: provider.baseUrl,
    chatModel: provider.chatModel,
    embeddingModel: provider.embeddingModel,
  })
  providerDialogVisible.value = true
}

function confirmDeleteProvider(provider: AiProvider) {
  showConfirm(`确定删除供应商「${provider.name}」吗？`, () => {
    providers.value = providers.value.filter((p) => p.id !== provider.id)
    showToast('删除成功')
  })
}

function confirmAddProvider() {
  showAddProvider.value = false
  editingProvider.value = null
  providerForm.name = ''
  providerForm.apiKey = ''
  providerForm.baseUrl = ''
  providerForm.chatModel = ''
  providerForm.embeddingModel = ''
  providerDialogVisible.value = true
}

function saveProvider() {
  if (editingProvider.value) {
    Object.assign(editingProvider.value, { ...providerForm })
    showToast('更新成功')
  } else {
    providers.value.push({
      id: Date.now(),
      isActive: false,
      name: providerForm.name || newProviderType.value,
      apiKey: providerForm.apiKey,
      baseUrl: providerForm.baseUrl,
      chatModel: providerForm.chatModel,
      embeddingModel: providerForm.embeddingModel,
    })
    showToast('添加成功')
  }
  providerDialogVisible.value = false
}

/* ========== AI 参数 ========== */
const aiParams = reactive({
  temperature: 0.7,
  maxTokens: 2048,
})

function saveAiParams() {
  showToast('参数保存成功')
}

/* ========== Prompt 指令 ========== */
const promptShortcuts = ref([
  { id: 1, name: '代码解释', prompt: '请详细解释以下代码的工作原理：', isPreset: true },
  { id: 2, name: '总结', prompt: '请总结以下内容的核心要点：', isPreset: true },
  { id: 3, name: '翻译', prompt: '请将以下内容翻译成中文：', isPreset: true },
])

const showPromptDialog = ref(false)
const editingPrompt = ref<PromptShortcut | null>(null)
const promptForm = reactive({ name: '', prompt: '' })

function openNewPrompt() {
  editingPrompt.value = null
  promptForm.name = ''
  promptForm.prompt = ''
  showPromptDialog.value = true
}

function editPrompt(item: PromptShortcut) {
  editingPrompt.value = item
  promptForm.name = item.name
  promptForm.prompt = item.prompt
  showPromptDialog.value = true
}

function closePromptDialog() {
  showPromptDialog.value = false
  editingPrompt.value = null
  promptForm.name = ''
  promptForm.prompt = ''
}

function deletePrompt(id: number) {
  promptShortcuts.value = promptShortcuts.value.filter((p) => p.id !== id)
  showToast('删除成功')
}

function savePrompt() {
  if (editingPrompt.value) {
    editingPrompt.value.name = promptForm.name
    editingPrompt.value.prompt = promptForm.prompt
    showToast('更新成功')
  } else {
    promptShortcuts.value.push({
      id: Date.now(),
      name: promptForm.name,
      prompt: promptForm.prompt,
      isPreset: false,
    })
    showToast('创建成功')
  }
  closePromptDialog()
}

/* ========== 用户画像 ========== */
const userProfile = reactive({
  enabled: false,
  identity: '',
  techStack: '',
  preferences: '',
})

function saveProfile() {
  showToast('用户画像保存成功')
}

/* ========== 基础设置 ========== */
const systemConfig = reactive({
  theme: 'light',
  fontSize: 14,
})

const userInfo = reactive({
  username: authStore.userInfo?.username || 'notemind_user',
  nickname: authStore.userInfo?.nickname || 'NoteMind 用户',
  email: 'user@example.com',
})

function saveSystemConfig() {
  showToast('设置保存成功')
}
</script>

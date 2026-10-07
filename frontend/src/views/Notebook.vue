<template>
  <div
    class="notebook-shell flex h-full min-w-[1180px] overflow-hidden bg-white text-zinc-800"
    :class="isSidePanelResizing || isRightPanelResizing ? 'select-none' : ''"
  >
    <aside
      class="flex w-[54px] shrink-0 flex-col bg-[#fbfbfb] text-zinc-500"
      :class="sidePanelVisible ? '' : 'border-r border-zinc-200'"
    >
      <div class="flex h-12 items-center justify-center border-b border-zinc-200 bg-[#fafafa]">
        <button
          class="sidebar-toggle"
          :title="sidePanelVisible ? '折叠左侧边栏' : '展开左侧边栏'"
          :aria-label="sidePanelVisible ? '折叠左侧边栏' : '展开左侧边栏'"
          @click="toggleSidePanel"
        >
          <PanelLeftClose v-if="sidePanelVisible" class="h-[18px] w-[18px]" />
          <PanelLeftOpen v-else class="h-[18px] w-[18px]" />
        </button>
      </div>

      <nav
        class="flex flex-1 flex-col items-center gap-5 pt-4"
        :class="sidePanelVisible ? 'border-r border-zinc-200' : ''"
      >
        <button class="rail-icon" title="打开快速切换">
          <FileSearch class="h-[20px] w-[20px]" />
        </button>
        <button class="rail-icon" title="查看关系图谱">
          <Network class="h-[20px] w-[20px]" />
        </button>
        <button class="rail-icon" title="新建白板">
          <Frame class="h-[20px] w-[20px]" />
        </button>
        <button class="rail-icon" title="打开/创建今天的日记">
          <CalendarDays class="h-[20px] w-[20px]" />
        </button>
        <button class="rail-icon" title="打开命令面板">
          <Command class="h-[20px] w-[20px]" />
        </button>
      </nav>
    </aside>

    <aside
      v-show="sidePanelVisible"
      class="relative flex shrink-0 flex-col border-r border-zinc-200 bg-[#fafafa]"
      :style="{ width: `${sidePanelWidth}px` }"
    >
      <header class="flex h-12 items-center gap-1 border-b border-zinc-200 px-3">
        <button
          class="side-tab"
          :class="sideTab === 'files' ? 'side-tab-active' : ''"
          title="文件列表"
          @click="sideTab = 'files'"
        >
          <ListTree class="h-[18px] w-[18px]" />
        </button>
        <button
          class="side-tab"
          :class="sideTab === 'search' ? 'side-tab-active' : ''"
          title="搜索"
          @click="sideTab = 'search'"
        >
          <Search class="h-[18px] w-[18px]" />
        </button>
        <button
          class="side-tab"
          :class="sideTab === 'bookmarks' ? 'side-tab-active' : ''"
          title="书签"
          @click="sideTab = 'bookmarks'"
        >
          <Bookmark class="h-[18px] w-[18px]" />
        </button>
      </header>

      <div class="tree-action-bar">
        <button class="tree-action" title="新建笔记">
          <FilePlus class="tree-action-icon" />
        </button>
        <button class="tree-action" title="新建文件夹">
          <FolderPlus class="tree-action-icon" />
        </button>
        <button class="tree-action" title="排序">
          <ArrowUpDown class="tree-action-icon" />
        </button>
        <button
          class="tree-action"
          :title="treeExpanded ? '全部折叠' : '全部展开'"
          @click="treeExpanded = !treeExpanded"
        >
          <FoldVertical v-if="treeExpanded" class="tree-action-icon" />
          <UnfoldVertical v-else class="tree-action-icon" />
        </button>
      </div>

      <section class="flex-1 overflow-y-auto px-3 py-3">
        <div class="mb-2 flex items-center justify-between px-2 text-sm font-medium text-zinc-500">
          <span class="flex items-center gap-1">
            <ChevronDown class="h-4 w-4 transition-transform" :class="treeExpanded ? '' : '-rotate-90'" />
            目录
          </span>
          <button class="small-icon h-6 w-6" title="新建文件夹">
            <Plus class="h-4 w-4" />
          </button>
        </div>

        <div v-show="treeExpanded" class="space-y-1">
          <button
            v-for="folder in currentRepo.folders"
            :key="folder.id"
            class="folder-row"
          >
            <Folder class="h-[18px] w-[18px] text-[#72b79d]" />
            <span class="truncate">{{ folder.name }}</span>
          </button>
        </div>

        <div class="mt-5 mb-2 px-2 text-sm font-medium text-zinc-500">笔记</div>
        <div v-show="treeExpanded">
          <button
            v-for="note in currentRepo.notes"
            :key="note.id"
            class="note-row"
            :class="activeNoteId === note.id ? 'note-row-active' : ''"
            @click="selectNote(note)"
          >
            <FileText class="mt-0.5 h-[18px] w-[18px] shrink-0 text-zinc-400" />
            <span class="min-w-0 flex-1 text-left">
              <span class="block truncate text-sm font-medium text-zinc-700">{{ note.title }}</span>
              <span class="mt-1 block truncate text-xs text-zinc-400">{{ note.summary }}</span>
            </span>
          </button>
        </div>
      </section>

      <footer class="flex h-12 items-center border-t border-zinc-200 px-4 text-sm text-zinc-500">
        <div class="flex items-center gap-2">
          <ChevronsUpDown class="h-4 w-4 text-zinc-300" />
          <span class="truncate">{{ currentRepo.name }}</span>
        </div>
      </footer>

      <div
        class="side-panel-resizer"
        title="拖拽调整宽度"
        @mousedown.prevent="startSidePanelResize"
      />
    </aside>

    <main class="flex min-w-0 flex-1 flex-col bg-white">
      <header class="workspace-tabs">
        <div class="workspace-tab is-active">
          <span class="min-w-0 flex-1 truncate">{{ activeNote.title }}</span>
          <button class="tab-close" title="关闭标签">
            <X class="h-4 w-4" />
          </button>
        </div>
        <button class="tab-new" title="新建标签">
          <Plus class="h-4 w-4" />
        </button>

        <div class="ml-auto flex items-center gap-1 self-center px-4">
          <div ref="viewMenuRef" class="relative">
            <button
              class="view-menu-trigger"
              title="更多操作"
              @click="showViewMenu = !showViewMenu"
            >
              <MoreHorizontal class="h-4 w-4" />
              <ChevronDown class="h-3 w-3" />
            </button>
            <div v-if="showViewMenu" class="view-menu">
              <button
                class="view-menu-item"
                :class="viewMode === 'source' ? 'is-active' : ''"
                @click="selectViewMode('source')"
              >
                <Code2 class="h-4 w-4" />
                源码模式
              </button>
              <button
                class="view-menu-item"
                :class="viewMode === 'reading' ? 'is-active' : ''"
                @click="selectViewMode('reading')"
              >
                <Eye class="h-4 w-4" />
                阅读模式
              </button>
              <div class="view-menu-divider" />
              <button class="view-menu-item" @click="selectViewMode('knowledge')">
                <Library class="h-4 w-4" />
                加入知识库
              </button>
            </div>
          </div>
          <button
            class="sidebar-toggle"
            :title="rightPanelVisible ? '折叠右侧边栏' : '展开右侧边栏'"
            :aria-label="rightPanelVisible ? '折叠右侧边栏' : '展开右侧边栏'"
            @click="toggleRightPanel"
          >
          <PanelRightClose v-if="rightPanelVisible" class="h-[18px] w-[18px]" />
          <PanelRightOpen v-else class="h-[18px] w-[18px]" />
          </button>
        </div>
      </header>

      <article class="workspace-leaf flex-1 overflow-y-auto bg-white">
        <div class="mx-auto max-w-[780px] px-10 pb-10 pt-8">
          <input v-model="activeNote.title" class="title-input" placeholder="输入笔记标题" />
          <textarea
            v-model="noteContent"
            class="markdown-area"
            spellcheck="false"
            placeholder="开始写 Markdown 笔记..."
            @input="autoResizeTextarea($event)"
          />
        </div>
      </article>

      <footer class="flex h-8 shrink-0 items-center justify-end gap-4 border-t border-zinc-100 px-5 text-xs text-zinc-500">
        <span>{{ backlinkCount }} 条反向链接</span>
        <span class="flex items-center gap-1">
          <Edit3 class="h-4 w-4" />
          {{ wordCount }} 个词
        </span>
        <span>{{ noteContent.length }} 个字符</span>
      </footer>
    </main>

    <aside
      v-show="rightPanelVisible"
      class="relative flex shrink-0 flex-col border-l border-zinc-200 bg-[#fbfbfb]"
      :class="isRightPanelResizing ? 'select-none' : ''"
      :style="{ width: `${rightPanelWidth}px` }"
    >
      <div
        class="side-panel-resizer"
        style="right: auto; left: -3px"
        title="拖拽调整宽度"
        @mousedown.prevent="startRightPanelResize"
      />
      <header class="flex h-12 items-center justify-center gap-1 border-b border-zinc-200 px-2">
        <button
          v-for="tab in rightTabs"
          :key="tab.key"
          class="right-tab-btn"
          :class="rightTab === tab.key ? 'right-tab-active' : ''"
          :title="tab.label"
          @click="rightTab = tab.key"
        >
          <!-- 反向链接：倾斜链节 + 左箭头 -->
          <span v-if="tab.key === 'backlinks'" class="relative inline-flex items-center justify-center">
            <Link class="h-[18px] w-[18px] -rotate-45" />
            <svg class="absolute -bottom-0.5 -right-0.5 h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 12H5" />
              <path d="M12 19l-7-7 7-7" />
            </svg>
          </span>
          <!-- 出链：链节 + 右箭头 -->
          <span v-else-if="tab.key === 'outgoing'" class="relative inline-flex items-center justify-center">
            <Link class="h-[18px] w-[18px]" />
            <svg class="absolute -bottom-0.5 -right-0.5 h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M5 12h14" />
              <path d="M12 5l7 7-7 7" />
            </svg>
          </span>
          <!-- 其余 tab 直接渲染图标 -->
          <component v-else :is="tab.icon" class="h-[18px] w-[18px]" />
        </button>
      </header>

      <!-- 反向链接 -->
      <section v-if="rightTab === 'backlinks'" class="flex-1 overflow-y-auto px-3 py-4">
        <div class="mb-3 text-sm font-medium text-zinc-500">{{ activeNote.title }} 的反向链接</div>
        <div class="rounded-lg border border-zinc-100 bg-white p-4 text-sm leading-6 text-zinc-500">
          暂无反向链接
        </div>
      </section>

      <!-- 出链 -->
      <section v-else-if="rightTab === 'outgoing'" class="flex-1 overflow-y-auto px-3 py-4">
        <div class="mb-3 text-sm font-medium text-zinc-500">{{ activeNote.title }} 的出链</div>
        <div class="rounded-lg border border-zinc-100 bg-white p-4 text-sm leading-6 text-zinc-500">
          暂无出链
        </div>
      </section>

      <!-- 标签 -->
      <section v-else-if="rightTab === 'tags'" class="flex-1 overflow-y-auto px-3 py-4">
        <div class="mb-3 text-sm font-medium text-zinc-500">标签</div>
        <div class="flex flex-wrap gap-2">
          <span class="rounded-md bg-zinc-100 px-2.5 py-1 text-xs font-medium text-zinc-600">笔记</span>
          <span class="rounded-md bg-zinc-100 px-2.5 py-1 text-xs font-medium text-zinc-600">欢迎</span>
        </div>
      </section>

      <!-- 笔记属性 -->
      <section v-else-if="rightTab === 'properties'" class="flex-1 overflow-y-auto px-3 py-4">
        <div class="mb-3 text-sm font-medium text-zinc-500">笔记属性</div>
        <div class="space-y-2">
          <label class="text-xs text-zinc-400">状态</label>
          <div class="rounded-md border border-zinc-100 bg-white px-3 py-2 text-sm text-zinc-700">草稿</div>
        </div>
        <div class="mt-3 space-y-2">
          <label class="text-xs text-zinc-400">创建时间</label>
          <div class="rounded-md border border-zinc-100 bg-white px-3 py-2 text-sm text-zinc-700">2025-01-15</div>
        </div>
      </section>

      <!-- 大纲 -->
      <section v-else-if="rightTab === 'outline'" class="flex-1 overflow-y-auto px-3 py-4">
        <div class="mb-3 text-sm font-medium text-zinc-500">{{ activeNote.title }} 的大纲</div>
        <div class="rounded-lg border border-zinc-100 bg-white p-4 text-sm leading-6 text-zinc-500">
          暂无大纲内容
        </div>
      </section>

      <!-- AI 助手 -->
      <template v-else-if="rightTab === 'ai'">
        <div class="border-b border-zinc-100 px-4 py-4">
          <div class="text-sm text-zinc-500">当前上下文</div>
          <div class="mt-2 rounded-md border border-zinc-100 bg-white px-3 py-2 text-sm text-zinc-700">
            {{ activeNote.title }}
          </div>
        </div>

        <div class="grid grid-cols-2 gap-2 border-b border-zinc-100 p-4">
          <button
            v-for="action in aiActions"
            :key="action.label"
            class="ai-action"
            @click="aiInput = action.prompt"
          >
            <component :is="action.icon" class="h-4 w-4" />
            {{ action.label }}
          </button>
        </div>

        <section class="flex-1 overflow-y-auto px-3 py-4">
          <div class="rounded-lg border border-zinc-100 bg-white p-4 text-sm leading-7 text-zinc-600">
            选中笔记内容后，可以让 AI 总结、解释、润色，或把整理结果直接写回当前 Markdown。
          </div>
        </section>

        <footer class="border-t border-zinc-100 bg-white p-4">
          <textarea
            v-model="aiInput"
            rows="3"
            class="ai-input"
            placeholder="基于当前笔记提问..."
            @keydown.enter.exact.prevent="sendAiMessage"
          />
          <div class="mt-2 flex items-center justify-between">
            <button class="model-chip" title="选择模型">
              DeepSeek
              <ChevronDown class="h-3 w-3" />
            </button>
            <button
              class="send-button"
              :class="aiInput.trim() ? 'send-button-active' : ''"
              :disabled="!aiInput.trim()"
              title="发送"
              @click="sendAiMessage"
            >
              <Send class="h-4 w-4" />
            </button>
          </div>
        </footer>
      </template>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  AlignLeft,
  ArrowUpDown,
  ArrowUpRight,
  Bookmark,
  CalendarDays,
  ChevronDown,
  ChevronsUpDown,
  Code2,
  Command,
  Edit3,
  Eye,
  FileEdit,
  FilePlus,
  FileSearch,
  FileText,
  Folder,
  FolderPlus,
  FoldVertical,
  Frame,
  HelpCircle,
  Library,
  Link,
  ListTree,
  MoreHorizontal,
  Network,
  PanelLeftClose,
  PanelLeftOpen,
  PanelRightClose,
  PanelRightOpen,
  Plus,
  Search,
  Send,
  Sparkles,
  Tags,
  UnfoldVertical,
  WandSparkles,
  X,
} from 'lucide-vue-next'

const TREE_ACTION_SIZE = 34
const TREE_ACTION_ICON = 18
const TREE_ACTION_GAP = 4
const TREE_ACTION_PAD_X = 8
/** 完整展示 4 个工具按钮所需的最小侧栏宽度：4*34 + 3*4 + 2*8 = 164 */
const SIDE_PANEL_MIN_WIDTH = 184.333
const SIDE_PANEL_MAX_WIDTH = 520
const SIDE_PANEL_DEFAULT_WIDTH = 330
const RIGHT_PANEL_MIN_WIDTH = 184.333
const RIGHT_PANEL_DEFAULT_WIDTH = 380

const activeRepoId = ref('graduate')
const activeNoteId = ref('welcome')
const aiInput = ref('')
const treeExpanded = ref(true)
const sideTab = ref('files')
const sidePanelVisible = ref(true)
const sidePanelWidth = ref(SIDE_PANEL_DEFAULT_WIDTH)
const isSidePanelResizing = ref(false)
const rightPanelVisible = ref(true)
const rightPanelWidth = ref(RIGHT_PANEL_DEFAULT_WIDTH)
const isRightPanelResizing = ref(false)
const rightTab = ref('ai')

const rightTabs = [
  { key: 'backlinks', icon: Link, label: '反向链接' },
  { key: 'outgoing', icon: ArrowUpRight, label: '出链' },
  { key: 'tags', icon: Tags, label: '标签' },
  { key: 'properties', icon: FileEdit, label: '属性' },
  { key: 'outline', icon: AlignLeft, label: '大纲' },
  { key: 'ai', icon: Sparkles, label: 'AI 助手' },
]
const showViewMenu = ref(false)
const viewMenuRef = ref<HTMLElement | null>(null)
const viewMode = ref('source')

let removeResizeListeners: (() => void) | null = null

function toggleSidePanel() {
  sidePanelVisible.value = !sidePanelVisible.value
}

function toggleRightPanel() {
  rightPanelVisible.value = !rightPanelVisible.value
}

function selectViewMode(mode: 'source' | 'reading' | 'knowledge') {
  if (mode === 'source' || mode === 'reading') {
    viewMode.value = mode
  }
  showViewMenu.value = false
}

function handleViewMenuOutside(event: MouseEvent) {
  if (viewMenuRef.value && !viewMenuRef.value.contains(event.target as Node)) {
    showViewMenu.value = false
  }
}

function startSidePanelResize(event: MouseEvent) {
  isSidePanelResizing.value = true
  const startX = event.clientX
  const startWidth = sidePanelWidth.value

  const onMove = (moveEvent: MouseEvent) => {
    const nextWidth = startWidth + (moveEvent.clientX - startX)
    if (nextWidth < SIDE_PANEL_MIN_WIDTH) {
      // 卡在最小宽度，继续拖拽累积偏移，超过 3/4 最小宽度才收起
      const deficit = SIDE_PANEL_MIN_WIDTH - nextWidth
      if (deficit > SIDE_PANEL_MIN_WIDTH * 0.75) {
        sidePanelVisible.value = false
        stopSidePanelResize()
        return
      }
      sidePanelWidth.value = SIDE_PANEL_MIN_WIDTH
      return
    }
    sidePanelWidth.value = Math.min(nextWidth, SIDE_PANEL_MAX_WIDTH)
  }

  const stopSidePanelResize = () => {
    isSidePanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stopSidePanelResize)
    removeResizeListeners = null
  }

  removeResizeListeners = stopSidePanelResize
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stopSidePanelResize)
}

let removeRightResizeListeners: (() => void) | null = null

function startRightPanelResize(event: MouseEvent) {
  isRightPanelResizing.value = true
  const startX = event.clientX
  const startWidth = rightPanelWidth.value

  const onMove = (moveEvent: MouseEvent) => {
    // Dragging right → panel gets narrower
    const nextWidth = startWidth - (moveEvent.clientX - startX)
    if (nextWidth < RIGHT_PANEL_MIN_WIDTH) {
      // 卡在最小宽度，继续拖拽累积偏移，超过 3/4 最小宽度才收起
      const deficit = RIGHT_PANEL_MIN_WIDTH - nextWidth
      if (deficit > RIGHT_PANEL_MIN_WIDTH * 0.75) {
        rightPanelVisible.value = false
        stopResize()
        return
      }
      rightPanelWidth.value = RIGHT_PANEL_MIN_WIDTH
      return
    }
    rightPanelWidth.value = nextWidth
  }

  const stopResize = () => {
    isRightPanelResizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stopResize)
    removeRightResizeListeners = null
  }

  removeRightResizeListeners = stopResize
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stopResize)
}

onMounted(() => {
  document.addEventListener('mousedown', handleViewMenuOutside)
  // 初始化 textarea 高度
  nextTick(() => {
    const textarea = document.querySelector<HTMLTextAreaElement>('.markdown-area')
    if (textarea) resizeTextarea(textarea)
  })
})

onBeforeUnmount(() => {
  removeResizeListeners?.()
  removeRightResizeListeners?.()
  document.removeEventListener('mousedown', handleViewMenuOutside)
})

const repositories = ref([
  {
    id: 'graduate',
    name: '毕设仓库',
    count: 6,
    folders: [
      { id: 'plan', name: '开发计划' },
      { id: 'design', name: '系统设计' },
      { id: 'rag', name: 'RAG资料' },
    ],
    notes: [
      {
        id: 'welcome',
        title: '欢迎',
        summary: '新仓库说明与起步内容',
        content: '# 欢迎\n\n这是你的新仓库。\n\n写点笔记、创建 [[链接]]，或者把已有 PDF / Word / Markdown 资料导入知识库。\n\n当你准备好了，可以把这篇欢迎笔记删除，让这个仓库真正服务你的毕设和学习。',
      },
      {
        id: 'architecture',
        title: 'NoteMind 系统架构',
        summary: 'Vue3、Spring Boot、RAG、pgvector',
        content: '# NoteMind 系统架构\n\n- 前端：Vue 3 + Vite + Tailwind CSS\n- 后端：Spring Boot 3 + Spring AI\n- 数据：PostgreSQL + pgvector\n- 文件：MinIO\n\n核心链路：笔记沉淀知识，知识库负责检索，AI 辅助整理和生成。',
      },
    ],
  },
  {
    id: 'java',
    name: 'Java仓库',
    count: 4,
    folders: [
      { id: 'spring', name: 'Spring Boot' },
      { id: 'security', name: '认证鉴权' },
    ],
    notes: [
      {
        id: 'jwt',
        title: 'JWT 鉴权流程',
        summary: '登录、签发、拦截器、刷新 Token',
        content: '# JWT 鉴权流程\n\n用户登录后，后端校验账号密码，通过后签发 Token。前端将 Token 存储到本地，并在 Axios 请求拦截器中自动携带。\n\n后端过滤器解析 Token，把用户身份放入 SecurityContext。',
      },
    ],
  },
  {
    id: 'career',
    name: '求职仓库',
    count: 3,
    folders: [
      { id: 'resume', name: '简历' },
      { id: 'interview', name: '面试表达' },
    ],
    notes: [
      {
        id: 'project-intro',
        title: '项目面试表达',
        summary: '把毕设讲成工程项目',
        content: '# 项目面试表达\n\nNoteMind 是一个基于 RAG 的智能知识管理平台。我的重点是把 Markdown 笔记、知识库检索和 AI 辅助整理串成完整链路。',
      },
    ],
  },
])

const aiActions = [
  { label: '总结', icon: Sparkles, prompt: '请总结当前笔记的核心内容：' },
  { label: '解释', icon: HelpCircle, prompt: '请解释当前笔记里的关键概念：' },
  { label: '润色', icon: WandSparkles, prompt: '请优化当前选中内容的表达：' },
  { label: '生成提纲', icon: FileText, prompt: '请基于当前笔记生成一份结构化提纲：' },
]

const currentRepo = computed(() => repositories.value.find((repo) => repo.id === activeRepoId.value) || repositories.value[0])
const activeNote = computed(() => {
  const note = currentRepo.value.notes.find((item) => item.id === activeNoteId.value)
  return note || currentRepo.value.notes[0]
})
const noteContent = ref(activeNote.value.content)
const wordCount = computed(() => noteContent.value.trim() ? noteContent.value.trim().split(/\s+/).length : 0)
const backlinkCount = computed(() => (noteContent.value.match(/\[\[/g) || []).length)

function resizeTextarea(textarea: HTMLTextAreaElement) {
  textarea.style.height = 'auto'
  textarea.style.height = `${textarea.scrollHeight}px`
}

function autoResizeTextarea(event: Event) {
  const textarea = event.currentTarget
  if (textarea instanceof HTMLTextAreaElement) resizeTextarea(textarea)
}

type PrototypeNote = { id: string; title: string; summary: string; content: string }

function selectNote(note: PrototypeNote) {
  activeNoteId.value = note.id
  noteContent.value = note.content
  nextTick(() => {
    const textarea = document.querySelector<HTMLTextAreaElement>('.markdown-area')
    if (textarea) resizeTextarea(textarea)
  })
}

function sendAiMessage() {
  if (!aiInput.value.trim()) return
  aiInput.value = ''
}
</script>

<style scoped>
.notebook-shell {
  /* 工具栏按钮尺寸：右侧面板后续复用 */
  --tree-action-size: 34px;
  --tree-action-icon: 18px;
  --tree-action-gap: 4px;
  --tree-action-pad-x: 8px;
  /* 4 * 34 + 3 * 4 + 2 * 8 = 164 */
  --side-panel-min-width: 164px;
}

.rail-icon,
.small-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.rail-icon {
  height: 30px;
  width: 30px;
}

.small-icon {
  height: 30px;
  width: 30px;
}

.rail-icon:hover,
.small-icon:hover {
  background: #ededed;
  color: #27272a;
}

/* Obsidian sidebar-toggle / clickable-icon */
.sidebar-toggle {
  display: inline-flex;
  height: 26px;
  width: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #787878;
  transition: color 0.1s ease, background-color 0.1s ease;
}

.sidebar-toggle:hover {
  background: rgba(0, 0, 0, 0.075);
  color: #222222;
}

.tree-action-bar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: var(--tree-action-gap);
  border-bottom: 1px solid #f4f4f5;
  padding: 8px var(--tree-action-pad-x);
}

.tree-action {
  display: inline-flex;
  height: var(--tree-action-size);
  width: var(--tree-action-size);
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.tree-action-icon {
  height: var(--tree-action-icon);
  width: var(--tree-action-icon);
}

.tree-action:hover {
  background: #ededed;
  color: #27272a;
}

.side-panel-resizer {
  position: absolute;
  top: 0;
  right: -3px;
  z-index: 20;
  width: 6px;
  height: 100%;
  cursor: col-resize;
}

.side-panel-resizer:hover,
.notebook-shell.select-none .side-panel-resizer {
  background: rgba(63, 141, 105, 0.18);
}

.side-tab {
  display: inline-flex;
  height: 30px;
  width: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.side-tab:hover {
  background: #ededed;
  color: #27272a;
}

.side-tab-active {
  background: #e9f5ef;
  color: #27272a;
}

/* 右侧面板 tab 按钮 */
.right-tab-btn {
  display: inline-flex;
  height: 30px;
  width: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: color 0.18s ease, background-color 0.18s ease;
}

.right-tab-btn:hover {
  background: #ededed;
  color: #27272a;
}

.right-tab-active {
  background: #e9f5ef;
  color: #27272a;
}

.workspace-tabs {
  display: flex;
  height: 48px;
  flex-shrink: 0;
  align-items: flex-end;
  gap: 2px;
  border-bottom: 1px solid #e4e4e7;
  background: #f4f4f3;
  padding: 0 12px;
}

.workspace-tab {
  display: inline-flex;
  height: 44px;
  min-width: 140px;
  max-width: 240px;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  padding: 0 10px 0 12px;
  font-size: 13px;
  color: #71717a;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.workspace-tab:hover {
  background: rgba(255, 255, 255, 0.55);
  color: #3f3f46;
}

.workspace-tab.is-active {
  position: relative;
  z-index: 1;
  margin-bottom: -1px;
  height: 45px;
  border-color: #e4e4e7;
  background: #ffffff;
  color: #27272a;
  box-shadow: 0 1px 0 #ffffff;
}

.tab-close {
  display: inline-flex;
  height: 28px;
  width: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #a1a1aa;
  opacity: 0;
  transition: opacity 0.12s ease, background-color 0.12s ease, color 0.12s ease;
}

.workspace-tab:hover .tab-close,
.workspace-tab.is-active .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: #f4f4f5;
  color: #27272a;
}

.tab-new {
  display: inline-flex;
  height: 28px;
  width: 28px;
  align-items: center;
  justify-content: center;
  align-self: center;
  border-radius: 6px;
  color: #a1a1aa;
  transition: color 0.15s ease, background-color 0.15s ease;
}

.tab-new:hover {
  background: rgba(255, 255, 255, 0.7);
  color: #3f3f46;
}

.workspace-leaf {
  background: #ffffff;
}

.folder-row,
.note-row {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 9px;
  border-radius: 7px;
  text-align: left;
  color: #52525b;
  transition: background-color 0.18s ease;
}

.folder-row {
  height: 36px;
  padding: 0 10px;
  font-size: 14px;
}

.note-row {
  min-height: 54px;
  padding: 8px 10px;
}

.folder-row:hover,
.note-row:hover,
.note-row-active {
  background: #e9f5ef;
}

.toolbar-button,
.toolbar-button-primary {
  display: inline-flex;
  height: 30px;
  align-items: center;
  gap: 6px;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13px;
  transition: background-color 0.18s ease;
}

.toolbar-button {
  color: #52525b;
}

.toolbar-button:hover {
  background: #eeeeee;
}

.toolbar-button-primary {
  background: #e7f4ee;
  color: #2f8c6b;
}

.view-menu-trigger {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 2px;
  border-radius: 4px;
  padding: 0 6px;
  color: #787878;
  transition: color 0.1s ease, background-color 0.1s ease;
}

.view-menu-trigger:hover {
  background: rgba(0, 0, 0, 0.075);
  color: #222222;
}

.view-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 40;
  min-width: 168px;
  overflow: hidden;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(24, 24, 27, 0.1);
  padding: 4px;
}

.view-menu-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  border-radius: 6px;
  padding: 8px 10px;
  text-align: left;
  font-size: 13px;
  color: #3f3f46;
  transition: background-color 0.12s ease;
}

.view-menu-item:hover,
.view-menu-item.is-active {
  background: #f4f4f5;
}

.view-menu-divider {
  height: 1px;
  margin: 4px 6px;
  background: #f4f4f5;
}

.title-input {
  width: 100%;
  border: 0;
  background: transparent;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
  color: #18181b;
  outline: none;
}

.markdown-area {
  margin-top: 24px;
  width: 100%;
  resize: none;
  border: 0;
  background: transparent;
  font-size: 18px;
  line-height: 2;
  color: #27272a;
  outline: none;
  overflow: hidden;
  min-height: 360px;
}

.ai-action {
  display: inline-flex;
  height: 34px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 7px;
  border: 1px solid #e8e8e8;
  background: white;
  font-size: 13px;
  color: #52525b;
  transition: background-color 0.18s ease, border-color 0.18s ease;
}

.ai-action:hover {
  border-color: #cfe8dc;
  background: #f3faf6;
}

.ai-input {
  width: 100%;
  resize: none;
  border-radius: 10px;
  border: 1px solid #e4e4e7;
  background: #fafafa;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 22px;
  color: #3f3f46;
  outline: none;
}

.model-chip {
  display: inline-flex;
  height: 30px;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  border: 1px solid #e8e8e8;
  background: #fafafa;
  padding: 0 12px;
  font-size: 13px;
  color: #52525b;
}

.send-button {
  display: inline-flex;
  height: 32px;
  width: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #d4d4d8;
  color: white;
  transition: background-color 0.18s ease;
}

.send-button-active {
  background: #2f8c6b;
}
</style>

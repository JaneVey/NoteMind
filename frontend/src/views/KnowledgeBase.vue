<template>
  <div class="flex h-full min-w-0 overflow-hidden bg-white text-zinc-800">
    <!-- 知识库列表 - resizable -->
    <aside
      ref="listRef"
      :style="{ width: listVisible ? listWidth + 'px' : '0px' }"
      :class="[
        'flex min-w-0 shrink-0 flex-col bg-[#fdfdfc]',
        listVisible ? 'border-r border-zinc-100' : 'overflow-hidden border-0'
      ]"
    >
      <header class="flex h-14 items-center justify-between px-4">
        <button class="kb-icon" title="收起侧栏" @click="toggleList">
          <PanelLeftClose class="h-[18px] w-[18px]" />
        </button>
        <div class="flex items-center gap-1">
          <button class="kb-icon" title="搜索知识库">
            <Search class="h-[19px] w-[19px]" />
          </button>
          <button class="kb-mini-icon" title="创建知识库" @click="showCreateDialog = true">
            <Plus class="h-4 w-4" />
          </button>
        </div>
      </header>

      <section class="px-3 pb-3">
        <div class="space-y-1">
          <button
            v-for="base in personalBases"
            :key="base.id"
            class="base-row"
            :class="activeBaseId === base.id ? 'base-row-active' : ''"
            @click="selectBase(base.id)"
          >
            <Folder class="h-[18px] w-[18px] fill-[#a9dfcf] text-[#74c6af]" />
            <span class="truncate">{{ base.name }}</span>
          </button>
        </div>
      </section>
    </aside>

    <!-- Resizer 1: 列表 ↔ 详情 -->
    <div
      v-if="listVisible"
      class="side-panel-resizer"
      @mousedown.prevent="startResize('list')"
    />

    <!-- 知识库详情 + Q&A 容器 -->
    <div ref="rightWrapper" class="flex min-w-0 flex-1">
      <!-- 知识库详情 - resizable -->
      <section
        ref="detailRef"
        :style="{ width: detailWidth + 'px' }"
        class="flex shrink-0 flex-col border-r border-zinc-100 bg-white"
      >
        <header class="relative border-b border-zinc-100 px-4 pb-5" :class="!listVisible ? 'pt-14' : 'pt-8'">
          <button
            v-if="!listVisible"
            class="kb-icon absolute left-4 top-3"
            title="展开侧栏"
            @click="toggleList"
          >
            <PanelLeftOpen class="h-[18px] w-[18px]" />
          </button>
          <button class="kb-icon absolute right-4 top-3" title="更多操作">
            <MoreHorizontal class="h-[18px] w-[18px]" />
          </button>
          <div class="flex items-center gap-5">
            <div class="knowledge-cover">
              <FileText class="h-12 w-12 fill-white text-white" />
            </div>
            <div class="min-w-0">
              <h1 class="truncate text-[24px] font-semibold text-zinc-800">{{ activeBase.name }}</h1>
              <p class="mt-3 text-[15px] text-zinc-400">{{ activeBase.type }}</p>
            </div>
          </div>
        </header>

        <div class="flex items-center justify-between px-4 py-4">
          <div class="text-[15px] font-semibold text-zinc-800">
            内容<span class="text-zinc-800">({{ activeBase.count }})</span>
          </div>
          <div class="flex items-center gap-2 text-zinc-400">
            <button class="kb-mini-icon" title="搜索内容">
              <Search class="h-4 w-4" />
            </button>
            <button class="kb-mini-icon" title="排序">
              <ArrowDownUp class="h-4 w-4" />
            </button>
            <button class="kb-mini-icon" title="导入内容" @click="showImportHint = true">
              <FilePlus2 class="h-4 w-4" />
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto px-3">
          <button
            v-for="item in activeBase.contents"
            :key="item.id"
            class="content-row"
            @click="selectedContent = item"
          >
            <FileText class="h-9 w-9 shrink-0 text-[#67b89e]" />
            <span class="min-w-0 flex-1 text-left">
              <span class="block truncate text-[15px] font-medium text-zinc-700">{{ item.name }}</span>
              <span class="mt-1 block text-[13px] text-zinc-400">{{ item.meta }}</span>
            </span>
            <span
              class="rounded-full px-2 py-0.5 text-xs"
              :class="item.status === '完成'
                ? 'bg-[#e8f6ef] text-[#3f8d69]'
                : 'bg-zinc-100 text-zinc-400'"
            >
              {{ item.status }}
            </span>
          </button>

          <div class="py-8 text-center text-sm text-zinc-300">
            {{ showImportHint ? '支持 Markdown、PDF、Word、PPT、图片等资料导入。' : '没有更多内容了' }}
          </div>
        </div>
      </section>

      <!-- Resizer 2: 详情 ↔ Q&A -->
      <div class="side-panel-resizer" @mousedown.prevent="startResize('detail')" />

      <!-- Q&A 问答区 -->
      <main class="relative flex min-w-0 flex-1 flex-col bg-[#fdfdfc]">
      <header class="flex h-14 items-center justify-end gap-3 px-5 text-zinc-500">
        <button class="kb-icon" title="新建内容">
          <FilePlus2 class="h-[18px] w-[18px]" />
        </button>
        <button class="kb-icon" title="历史记录">
          <Clock class="h-[18px] w-[18px]" />
        </button>
      </header>

      <section class="flex flex-1 flex-col items-center justify-center px-8 pb-28 text-center">
        <div class="mb-4 flex h-9 w-9 items-center justify-center rounded-md bg-[#e5f4ee] text-[#9ed9c4]">
          <MessageSquareMore class="h-5 w-5" />
        </div>
        <p class="text-[16px] text-zinc-400">
          {{ selectedContent ? `基于「${selectedContent.name}」提问` : '基于知识库问答' }}
        </p>
        <div v-if="selectedContent" class="mt-5 w-full max-w-md rounded-lg border border-zinc-100 bg-white p-4 text-left text-sm text-zinc-500 shadow-sm">
          <div class="mb-2 font-medium text-zinc-700">引用来源预览</div>
          <p>{{ selectedContent.citation }}</p>
        </div>
      </section>

      <div class="w-full shrink-0 px-8 pt-10 pb-2">
        <div class="mx-auto w-full max-w-[920px]">
          <div class="ask-box">
            <textarea
              v-model="ragQuestion"
              rows="2"
              class="ask-input"
              placeholder="基于知识库提问"
              @keydown.enter.exact.prevent="askQuestion"
            />
            <div class="flex items-center justify-between pt-2">
              <button class="ask-chip" title="选择模型">
                DS 快速
                <ChevronDown class="h-3 w-3" />
              </button>
              <div class="flex items-center gap-1">
                <button class="kb-icon" title="添加附件">
                  <Paperclip class="h-[18px] w-[18px]" />
                </button>
                <button
                  class="send-button"
                  :class="ragQuestion.trim() ? 'send-button-active' : ''"
                  :disabled="!ragQuestion.trim()"
                  title="发送"
                  @click="askQuestion"
                >
                  <Send class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
          <div class="mt-3 text-center text-xs text-zinc-300">内容由AI生成仅供参考</div>
        </div>
      </div>
    </main>

    </div>

    <Dialog v-model:open="showCreateDialog">
      <DialogContent class="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>创建知识库</DialogTitle>
        </DialogHeader>
        <div class="space-y-3 py-2">
          <Input v-model="newBaseName" placeholder="知识库名称" />
          <textarea
            v-model="newBaseType"
            rows="3"
            class="w-full resize-none rounded-md border border-input px-3 py-2 text-sm outline-none focus:ring-1 focus:ring-ring"
            placeholder="描述，例如：个人知识库"
          />
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="outline" @click="showCreateDialog = false">取消</Button>
          <Button :disabled="!newBaseName.trim()" @click="createBase">创建</Button>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import {
  ArrowDownUp,
  ChevronDown,
  Clock,
  FilePlus2,
  FileText,
  Folder,
  MessageSquareMore,
  MoreHorizontal,
  PanelLeftClose,
  PanelLeftOpen,
  Paperclip,
  PenLine,
  Plus,
  Scissors,
  Search,
  Send,
  Upload,
} from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Dialog from '@/components/ui/Dialog.vue'
import DialogContent from '@/components/ui/DialogContent.vue'
import DialogHeader from '@/components/ui/DialogHeader.vue'
import DialogTitle from '@/components/ui/DialogTitle.vue'
import Input from '@/components/ui/Input.vue'

const activeBaseId = ref('project')
const selectedContent = ref(null)
const ragQuestion = ref('')
const showCreateDialog = ref(false)
const showImportHint = ref(false)
const newBaseName = ref('')
const newBaseType = ref('')

// --- 推拉 & 折叠 ---
const LIST_MIN = 180
const LIST_MAX = 320
const DETAIL_MIN = 320
const DETAIL_MAX = 480

const listRef = ref(null)
const detailRef = ref(null)
const rightWrapper = ref(null)

const listVisible = ref(true)
const listWidth = ref(320)
const detailWidth = ref(480)

function toggleList() {
  listVisible.value = !listVisible.value
}

function startResize(type) {
  const startX = event.clientX
  const startList = listWidth.value
  const startDetail = detailWidth.value
  document.documentElement.style.cursor = 'col-resize'
  document.documentElement.style.userSelect = 'none'

  function onMove(e) {
    const dx = e.clientX - startX

    if (type === 'list') {
      // === Resizer1: 列表 ↔ (详情+Q&A) ===
      let newList = startList + dx
      newList = Math.max(LIST_MIN, Math.min(newList, LIST_MAX))

      // 折叠触发：拖到最小值后再左拖 LIST_MIN/2
      if (dx < 0 && newList === LIST_MIN) {
        const overshoot = Math.abs(startList + dx - LIST_MIN)
        if (overshoot >= LIST_MIN / 2) {
          listVisible.value = false
          window.removeEventListener('mousemove', onMove)
          window.removeEventListener('mouseup', stopResize)
          resetCursor()
          return
        }
      }

      listWidth.value = newList
      // detail 不变（在 rightWrapper 内固定宽度，Q&A 自适应）

    } else if (type === 'detail') {
      // === Resizer2: 详情 ↔ Q&A（两阶段联动） ===
      if (dx >= 0) {
        // ---- 向右拖动 ----
        // Phase 1: 详情增长到 DETAIL_MAX
        let newDetail = startDetail + dx
        if (newDetail <= DETAIL_MAX) {
          newDetail = Math.max(DETAIL_MIN, newDetail)
          listWidth.value = startList
        } else if (listVisible.value) {
          // Phase 2: 详情到顶 + 列表可见 → 联动增长列表
          newDetail = DETAIL_MAX
          const remainDx = dx - (DETAIL_MAX - startDetail)
          let newList = startList + remainDx
          newList = Math.max(LIST_MIN, Math.min(newList, LIST_MAX))
          listWidth.value = newList
        } else {
          newDetail = DETAIL_MAX
        }
        detailWidth.value = newDetail

      } else {
        // ---- 向左拖动 ----
        // Phase 1: 详情缩小到 DETAIL_MIN
        let newDetail = startDetail + dx
        if (newDetail >= DETAIL_MIN) {
          newDetail = Math.min(DETAIL_MAX, Math.max(DETAIL_MIN, newDetail))
          listWidth.value = startList
        } else if (listVisible.value) {
          // Phase 2: 详情到底 + 列表可见 → 联动缩小列表
          newDetail = DETAIL_MIN
          const remainDx = dx + (startDetail - DETAIL_MIN)
          let newList = startList + remainDx
          newList = Math.max(LIST_MIN, Math.min(newList, LIST_MAX))
          listWidth.value = newList
        } else {
          newDetail = DETAIL_MIN
        }
        detailWidth.value = newDetail
      }
    }
  }

  function resetCursor() {
    document.documentElement.style.cursor = ''
    document.documentElement.style.userSelect = ''
  }

  function stopResize() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', stopResize)
    resetCursor()
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', stopResize)
}

const personalBases = ref([
  { id: 'java', name: 'Java知识库', type: '个人知识库', count: 9, contents: [] },
  { id: 'graduate', name: '毕设资料库', type: '个人知识库', count: 15, contents: [] },
  { id: 'paper', name: '论文库', type: '个人知识库', count: 6, contents: [] },
  { id: 'interview', name: '求职知识库', type: '个人知识库', count: 8, contents: [] },
  {
    id: 'project',
    name: '项目复盘库',
    type: '个人知识库',
    count: 3,
    contents: [
      {
        id: 'novel',
        name: '小说网站项目复盘.md',
        meta: 'Markdown | 7/9',
        status: '完成',
        citation: '来源：小说网站项目复盘.md / 后端链路 / JWT 鉴权设计',
      },
      {
        id: 'rag',
        name: 'RAG系统架构设计.pdf',
        meta: 'PDF | 7/8',
        status: '解析中',
        citation: '来源：RAG系统架构设计.pdf / 文档处理流程',
      },
      {
        id: 'speech',
        name: '项目面试表达.docx',
        meta: 'Word | 7/8',
        status: '完成',
        citation: '来源：项目面试表达.docx / 自我介绍 / 项目亮点',
      },
    ],
  },
])

const activeBase = computed(() => {
  const current = personalBases.value.find((item) => item.id === activeBaseId.value)
  if (current?.contents.length) return current
  return {
    ...(current || personalBases.value[0]),
    contents: [
      {
        id: `${current?.id || 'base'}-doc-1`,
        name: `${current?.name || '知识库'}导入说明.md`,
        meta: 'Markdown | 7/21',
        status: '待处理',
        citation: '来源将在 RAG 回答中以文档名和章节形式展示。',
      },
    ],
  }
})

function selectBase(id) {
  activeBaseId.value = id
  selectedContent.value = null
  showImportHint.value = false
}

function createBase() {
  const name = newBaseName.value.trim()
  if (!name) return
  const id = `base-${Date.now()}`
  personalBases.value.push({
    id,
    name,
    type: newBaseType.value.trim() || '个人知识库',
    count: 0,
    contents: [],
  })
  activeBaseId.value = id
  showCreateDialog.value = false
  newBaseName.value = ''
  newBaseType.value = ''
}

function askQuestion() {
  const question = ragQuestion.value.trim()
  if (!question) return
  selectedContent.value = selectedContent.value || activeBase.value.contents[0] || null
  ragQuestion.value = ''
}
</script>

<style scoped>
.kb-icon,
.kb-mini-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.kb-icon {
  height: 32px;
  width: 32px;
}

.kb-mini-icon {
  height: 28px;
  width: 28px;
}

.kb-icon:hover,
.kb-mini-icon:hover {
  background: #f4f4f5;
  color: #27272a;
}

.group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px 8px;
  font-size: 16px;
  color: #52525b;
}

.base-row,
.group-row {
  display: flex;
  height: 40px;
  width: 100%;
  align-items: center;
  gap: 10px;
  border-radius: 7px;
  padding: 0 12px;
  text-align: left;
  font-size: 16px;
  color: #3f3f46;
  transition: background-color 0.18s ease;
}

.base-row:hover,
.group-row:hover,
.base-row-active {
  background: #e9f5ef;
}

.knowledge-cover {
  display: flex;
  height: 88px;
  width: 88px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 4px solid #ecfaf5;
  background: linear-gradient(180deg, #8ddac4 0%, #7ccfb8 100%);
}

.content-row {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 13px;
  border-radius: 8px;
  padding: 10px 10px;
  transition: background-color 0.18s ease;
}

.content-row:hover {
  background: #f7fbf9;
}

.ask-box {
  border: 1px solid #e3e3e3;
  border-radius: 23px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(30, 41, 36, 0.08);
  padding: 14px 16px 12px;
}

.ask-input {
  min-height: 54px;
  width: 100%;
  resize: none;
  border: 0;
  background: transparent;
  color: #3f3f46;
  font-size: 18px;
  line-height: 28px;
  outline: none;
}

.ask-input::placeholder {
  color: #c7c7c7;
}

.ask-chip {
  display: inline-flex;
  height: 34px;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  border: 1px solid #ededed;
  background: #fafafa;
  padding: 0 13px;
  font-size: 15px;
  color: #3f3f46;
}

.send-button {
  display: inline-flex;
  height: 34px;
  width: 34px;
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

/* --- 推拉 resizer（与 Notebook.vue 一致） --- */
.side-panel-resizer {
  width: 4px;
  cursor: col-resize;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  background: transparent;
  transition: background 0.2s ease;
}
.side-panel-resizer::after {
  content: '';
  position: absolute;
  left: -3px;
  right: -3px;
  top: 0;
  bottom: 0;
}
.side-panel-resizer:hover,
.side-panel-resizer:active {
  background: #2f8c6b;
}
</style>

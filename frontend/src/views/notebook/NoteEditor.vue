<template>
  <main class="flex min-w-0 flex-1 flex-col bg-white">
    <EditorToolbar
      :view-mode="viewMode"
      :right-panel-visible="rightPanelVisible"
      @run-command="runRichCommand"
      @format-block="formatRichBlock"
      @select-block="formatRichBlock"
      @align="runRichCommand"
      @apply-color="applyRichColor"
      @insert-link="insertRichLink"
      @insert-wiki-link="insertWikiLink"
      @insert-todo="insertTodo"
      @insert-horizontal-rule="insertHorizontalRule"
      @toggle-find="showFindPanel = !showFindPanel"
      @toggle-mode="toggleViewMode"
      @update:right-panel-visible="emit('update:rightPanelVisible', $event)"
    />

    <FindPanel v-if="showFindPanel" @close="showFindPanel = false" />

    <article class="min-h-0 flex-1 overflow-y-auto bg-white">
      <div class="editor-page">
        <input
          :value="title"
          class="title-input"
          placeholder="无标题笔记"
          @compositionstart="composing = true"
          @compositionend="onCompositionEnd"
          @input="onTitleInput"
        />
        <div
          v-if="viewMode === 'writing'"
          ref="richEditorRef"
          class="rich-editor"
          contenteditable="true"
          spellcheck="false"
          data-placeholder="开始记录你的想法..."
          @input="syncRichEditor"
        />
        <textarea
          v-else-if="viewMode === 'source'"
          ref="sourceEditorRef"
          :value="content"
          class="source-editor"
          spellcheck="false"
          @compositionstart="composing = true"
          @compositionend="onCompositionEnd"
          @input="onSourceInput"
        />
        <section v-else class="reading-content" v-html="renderedNoteContent" />
      </div>
    </article>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import type { Id } from '@/types/common'
import EditorToolbar from './EditorToolbar.vue'
import FindPanel from './FindPanel.vue'
import type { ViewMode } from './types'

/**
 * 编辑器主区：顶部工具栏 + 查找面板 + 标题输入 + 正文编辑（三种视图模式）。
 *
 * <p>标题与正文通过 `v-model:title` / `v-model:content` 与页面容器双向同步，
 * 内容变化后再用 `change` 事件通知容器（容器负责未保存标记与防抖保存）。
 *
 * <p><b>视图模式切换为什么用两个 watcher</b>：
 * `viewMode` 由容器持有（底部状态栏也要用它），切换只能通过 prop 变化感知：
 * - **渲染前**（默认 pre）把富文本内容写回 Markdown —— 此时旧的 contenteditable 元素还在；
 * - **渲染后**（post）把 Markdown 渲染进富文本编辑器 —— 此时新元素已经挂载。
 * 顺序与原来「先 sync 再 nextTick(load)」完全一致。
 */
const props = defineProps<{
  noteId: Id
  title: string
  content: string
  viewMode: ViewMode
  rightPanelVisible: boolean
  /** 插入「笔记链接」时的目标标题；没有其他笔记时为 null */
  wikiLinkTargetTitle: string | null
}>()

const emit = defineEmits<{
  'update:title': [title: string]
  'update:content': [content: string]
  'update:viewMode': [mode: ViewMode]
  'update:rightPanelVisible': [visible: boolean]
  change: []
}>()

const showFindPanel = ref(false)
const richEditorRef = ref<HTMLElement | null>(null)
const sourceEditorRef = ref<HTMLTextAreaElement | null>(null)
/** 输入法组字中：与 v-model 自带的保护一致，组字过程不写回模型 */
const composing = ref(false)

const renderedNoteContent = computed(() => renderMarkdown(props.content))

function onTitleInput(event: Event): void {
  if (composing.value) return
  const target = event.target
  if (!(target instanceof HTMLInputElement)) return
  emit('update:title', target.value)
  emit('change')
}

function onSourceInput(event: Event): void {
  if (composing.value) return
  const target = event.target
  if (!(target instanceof HTMLTextAreaElement)) return
  emit('update:content', target.value)
  emit('change')
}

function onCompositionEnd(event: CompositionEvent): void {
  composing.value = false
  if (event.target instanceof HTMLTextAreaElement) onSourceInput(event)
  else onTitleInput(event)
}

function switchToWriting(): void {
  emit('update:viewMode', 'writing')
}

function toggleViewMode(): void {
  emit('update:viewMode', props.viewMode === 'writing' ? 'reading' : 'writing')
}

function focusEditor(): void {
  if (props.viewMode === 'writing') richEditorRef.value?.focus()
  else if (props.viewMode === 'source') sourceEditorRef.value?.focus()
}

function runRichCommand(command: string, value?: string): void {
  if (props.viewMode !== 'writing') {
    switchToWriting()
    nextTick(() => runRichCommand(command, value))
    return
  }
  richEditorRef.value?.focus()
  document.execCommand(command, false, value)
  syncRichEditor()
}

function formatRichBlock(tag: string): void {
  runRichCommand('formatBlock', tag)
}

function applyRichColor(command: string, color: string): void {
  runRichCommand(command, color)
}

function insertTodo(): void {
  if (props.viewMode !== 'writing') {
    switchToWriting()
    nextTick(insertTodo)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHTML', false, '<p><input type="checkbox" contenteditable="false"> 待办事项</p>')
  syncRichEditor()
}

function insertHorizontalRule(): void {
  if (props.viewMode !== 'writing') {
    switchToWriting()
    nextTick(insertHorizontalRule)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHorizontalRule')
  syncRichEditor()
}

function insertRichLink(): void {
  const url = window.prompt('输入链接地址')
  if (url) runRichCommand('createLink', url)
}

function insertWikiLink(): void {
  const target = props.wikiLinkTargetTitle
  if (!target) return
  if (props.viewMode !== 'writing') {
    switchToWriting()
    nextTick(insertWikiLink)
    return
  }
  richEditorRef.value?.focus()
  document.execCommand('insertHTML', false, `<span class="wikilink-chip" contenteditable="false">${escapeHtml(target)}</span>&nbsp;`)
  syncRichEditor()
}

function syncRichEditor(): void {
  if (!richEditorRef.value) return
  emit('update:content', richHtmlToMarkdown(richEditorRef.value.innerHTML))
  emit('change')
}

function loadRichEditor(): void {
  if (richEditorRef.value) richEditorRef.value.innerHTML = markdownToRichHtml(props.content)
}

function escapeHtml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function markdownInlineToHtml(value: string): string {
  return escapeHtml(value)
    .replace(/\[\[(.*?)\]\]/g, '<span class="wikilink-chip" contenteditable="false">$1</span>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/~~(.*?)~~/g, '<s>$1</s>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
}

function markdownToRichHtml(value: string): string {
  const lines = value.split('\n')
  const blocks: string[] = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    if (!line.trim()) {
      index += 1
      continue
    }
    const heading = line.match(/^(#{1,3})\s+(.*)$/)
    if (heading) {
      const level = heading[1].length
      blocks.push(`<h${level}>${markdownInlineToHtml(heading[2])}</h${level}>`)
      index += 1
      continue
    }
    if (line.startsWith('- ')) {
      const items: string[] = []
      while (index < lines.length && lines[index].startsWith('- ')) {
        items.push(`<li>${markdownInlineToHtml(lines[index].slice(2))}</li>`)
        index += 1
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
      continue
    }
    if (/^\d+\.\s/.test(line)) {
      const items: string[] = []
      while (index < lines.length && /^\d+\.\s/.test(lines[index])) {
        items.push(`<li>${markdownInlineToHtml(lines[index].replace(/^\d+\.\s/, ''))}</li>`)
        index += 1
      }
      blocks.push(`<ol>${items.join('')}</ol>`)
      continue
    }
    if (line.startsWith('> ')) {
      blocks.push(`<blockquote>${markdownInlineToHtml(line.slice(2))}</blockquote>`)
      index += 1
      continue
    }
    blocks.push(`<p>${markdownInlineToHtml(line)}</p>`)
    index += 1
  }
  return blocks.join('')
}

function richHtmlToMarkdown(html: string): string {
  const documentNode = new DOMParser().parseFromString(html, 'text/html')
  const serialize = (node: Node): string => {
    if (node.nodeType === Node.TEXT_NODE) return node.textContent || ''
    if (node.nodeType !== Node.ELEMENT_NODE) return ''
    const element = node as HTMLElement
    const content = Array.from(element.childNodes).map(serialize).join('')
    if (element.classList.contains('wikilink-chip')) return `[[${element.textContent || ''}]]`

    switch (element.tagName) {
      case 'BR': return '\n'
      case 'H1': return `# ${content.trim()}\n\n`
      case 'H2': return `## ${content.trim()}\n\n`
      case 'H3': return `### ${content.trim()}\n\n`
      case 'P':
      case 'DIV': return `${content.trim()}\n\n`
      case 'STRONG':
      case 'B': return `**${content}**`
      case 'EM':
      case 'I': return `*${content}*`
      case 'S':
      case 'STRIKE': return `~~${content}~~`
      case 'CODE': return `\`${content}\``
      case 'A': return `[${content}](${element.getAttribute('href') || ''})`
      case 'LI': return content
      case 'UL': return Array.from(element.children).map((item) => `- ${serialize(item).trim()}`).join('\n') + '\n\n'
      case 'OL': return Array.from(element.children).map((item, itemIndex) => `${itemIndex + 1}. ${serialize(item).trim()}`).join('\n') + '\n\n'
      case 'BLOCKQUOTE': return `> ${content.trim()}\n\n`
      case 'PRE': return `\`\`\`\n${element.textContent?.trim() || ''}\n\`\`\`\n\n`
      default: return content
    }
  }
  return Array.from(documentNode.body.childNodes).map(serialize).join('').replace(/\n{3,}/g, '\n\n').trim()
}

function renderMarkdown(value: string): string {
  return escapeHtml(value)
    .replace(/^### (.*)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*)$/gm, '<h1>$1</h1>')
    .replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^- (.*)$/gm, '<li>$1</li>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n{2,}/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
}

// 进入写作模式：DOM 更新完再把 Markdown 渲染进富文本编辑器
watch(() => props.viewMode, (mode) => {
  if (mode === 'writing') loadRichEditor()
}, { flush: 'post' })

// 离开写作模式：先把富文本内容写回 Markdown（此刻旧的 contenteditable 还在）
watch(() => props.viewMode, (_mode, previousMode) => {
  if (previousMode === 'writing') syncRichEditor()
})

// 切换笔记：重新载入正文并把焦点放回编辑器
watch(() => props.noteId, () => {
  nextTick(() => {
    loadRichEditor()
    focusEditor()
  })
})

onMounted(() => {
  nextTick(loadRichEditor)
})
</script>

<style scoped>
.editor-page { width: min(100%, 900px); margin: 0 auto; padding: 58px 48px 100px; }
.title-input { width: 100%; border: 0; background: transparent; outline: none; font-size: 34px; line-height: 1.25; font-weight: 700; color: #18181b; }
.title-input::placeholder { color: #d4d4d8; }
.rich-editor { min-height: 620px; margin-top: 30px; outline: none; color: #3f3f46; font-size: 16px; line-height: 1.9; }
.rich-editor:empty::before { content: attr(data-placeholder); color: #c4c4c8; pointer-events: none; }
.rich-editor :deep(h1), .rich-editor :deep(h2), .rich-editor :deep(h3) { color: #1f2933; font-weight: 700; }.rich-editor :deep(h1) { margin: 30px 0 14px; font-size: 28px; }.rich-editor :deep(h2) { margin: 27px 0 12px; font-size: 22px; }.rich-editor :deep(h3) { margin: 22px 0 10px; font-size: 18px; }
.rich-editor :deep(p) { margin: 0 0 15px; }.rich-editor :deep(ul), .rich-editor :deep(ol) { margin: 0 0 17px 24px; }.rich-editor :deep(li) { padding-left: 4px; }.rich-editor :deep(blockquote) { margin: 18px 0; border-left: 3px solid #8fc9b0; padding-left: 14px; color: #6b7280; }.rich-editor :deep(pre) { margin: 16px 0; border-radius: 6px; background: #f4f5f4; padding: 14px; font-family: Consolas, "Cascadia Code", monospace; font-size: 13px; line-height: 1.6; }
.rich-editor :deep(code) { border-radius: 4px; background: #f1f1ef; padding: 2px 5px; font-family: Consolas, monospace; font-size: 13px; }.rich-editor :deep(.wikilink-chip) { display: inline-flex; align-items: center; border-radius: 4px; background: #e8f5ee; padding: 1px 5px; color: #267653; font-size: .92em; }
.source-editor { display: block; width: 100%; min-height: 620px; margin-top: 30px; resize: none; border: 1px solid #e7e7e7; border-radius: 6px; background: #fafafa; padding: 16px; outline: none; color: #3f3f46; font-family: Consolas, "Cascadia Code", monospace; font-size: 14px; line-height: 1.75; }
.reading-content { margin-top: 30px; color: #3f3f46; font-size: 16px; line-height: 1.9; }
.reading-content :deep(h1) { margin: 28px 0 12px; font-size: 27px; color: #18181b; }
.reading-content :deep(h2) { margin: 24px 0 10px; font-size: 21px; color: #27272a; }
.reading-content :deep(h3) { margin: 20px 0 8px; font-size: 17px; color: #3f3f46; }
.reading-content :deep(p) { margin: 0 0 14px; }
.reading-content :deep(li) { margin-left: 20px; }
.reading-content :deep(blockquote) { margin: 14px 0; border-left: 3px solid #94cdb5; padding-left: 14px; color: #71717a; }
.reading-content :deep(code) { border-radius: 4px; background: #f1f1ef; padding: 2px 5px; font-family: Consolas, monospace; font-size: 13px; }
@media (max-width: 1180px) { .editor-page { padding-left: 32px; padding-right: 32px; } }
</style>

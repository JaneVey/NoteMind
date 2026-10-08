<template>
  <div class="editor-toolbar">
    <button class="format-button" title="撤销" @click="emit('run-command', 'undo')"><Undo2 class="h-4 w-4" /></button>
    <button class="format-button" title="重做" @click="emit('run-command', 'redo')"><Redo2 class="h-4 w-4" /></button>
    <button class="format-button" title="格式刷"><Paintbrush class="h-4 w-4" /></button>
    <button class="format-button" title="清除格式" @click="emit('run-command', 'removeFormat')"><Eraser class="h-4 w-4" /></button>
    <span class="toolbar-divider" />
    <ToolbarMenu>
      <template #trigger="{ isOpen, toggle }">
        <button class="format-select" :class="isOpen ? 'format-select-active' : ''" title="插入" @click="toggle"><CirclePlus class="h-4 w-4" />插入 <ChevronDown class="h-3.5 w-3.5" /></button>
      </template>
      <template #panel="{ close }">
        <div class="insert-menu">
          <button class="insert-menu-item"><Table2 class="h-4 w-4" />表格</button>
          <button class="insert-menu-item" @click="emit('insert-link'); close()"><Link class="h-4 w-4" />链接</button>
          <button class="insert-menu-item"><ImagePlus class="h-4 w-4" />图片</button>
          <div class="toolbar-menu-divider" />
          <button class="insert-menu-item" @click="emit('format-block', 'pre')"><Code2 class="h-4 w-4" />代码块</button>
          <button class="insert-menu-item"><Sigma class="h-4 w-4" />公式</button>
          <button class="insert-menu-item" @click="emit('insert-horizontal-rule'); close()"><Minus class="h-4 w-4" />分割线</button>
          <button class="insert-menu-item" @click="emit('format-block', 'blockquote')"><Quote class="h-4 w-4" />引用</button>
          <div class="toolbar-menu-divider" />
          <button class="insert-menu-item"><Mic class="h-4 w-4" />录音纪要</button>
          <button class="insert-menu-item"><Paperclip class="h-4 w-4" />附件</button>
          <button class="insert-menu-item" @click="emit('insert-wiki-link'); close()"><NotebookPen class="h-4 w-4" />笔记</button>
        </div>
      </template>
    </ToolbarMenu>
    <span class="toolbar-divider" />
    <button class="format-button font-bold" title="加粗" @click="emit('run-command', 'bold')">B</button>
    <button class="format-button font-serif text-base italic" title="斜体" @click="emit('run-command', 'italic')">I</button>
    <button class="format-button underline" title="下划线" @click="emit('run-command', 'underline')">U</button>
    <button class="format-button" title="删除线" @click="emit('run-command', 'strikeThrough')"><Strikethrough class="h-4 w-4" /></button>
    <ToolbarMenu>
      <template #trigger="{ isOpen, toggle }">
        <button class="format-button" :class="isOpen ? 'format-button-active' : ''" title="高亮" @click="toggle"><Highlighter class="h-4 w-4" /></button>
      </template>
      <template #panel="{ close }">
        <div class="color-menu"><p>背景颜色</p><button class="color-reset" @click="emit('apply-color', 'hiliteColor', 'transparent'); close()">无颜色</button><div class="color-grid"><button v-for="color in highlightColors" :key="color" :style="{ background: color }" @click="emit('apply-color', 'hiliteColor', color); close()" /></div></div>
      </template>
    </ToolbarMenu>
    <ToolbarMenu>
      <template #trigger="{ isOpen, toggle }">
        <button class="format-button text-color-button" :class="isOpen ? 'format-button-active' : ''" title="文字颜色" @click="toggle">A</button>
      </template>
      <template #panel="{ close }">
        <div class="color-menu"><p>文字颜色</p><button class="color-reset" @click="emit('apply-color', 'foreColor', '#3f3f46'); close()">默认颜色</button><div class="color-grid"><button v-for="color in textColors" :key="color" :style="{ background: color }" @click="emit('apply-color', 'foreColor', color); close()" /></div></div>
      </template>
    </ToolbarMenu>
    <ToolbarMenu>
      <template #trigger="{ isOpen, toggle }">
        <button class="format-select" :class="isOpen ? 'format-select-active' : ''" title="切换格式" @click="toggle">正文 1 <ChevronDown class="h-3.5 w-3.5" /></button>
      </template>
      <template #panel="{ close }">
        <div class="block-menu"><button v-for="block in blockOptions" :key="block.tag" :class="`block-menu-${block.tag}`" @click="emit('select-block', block.tag); close()">{{ block.label }}</button></div>
      </template>
    </ToolbarMenu>
    <span class="toolbar-divider" />
    <ToolbarMenu>
      <template #trigger="{ isOpen, toggle }">
        <button class="format-button" :class="isOpen ? 'format-button-active' : ''" title="对齐方式" @click="toggle"><AlignLeft class="h-4 w-4" /></button>
      </template>
      <template #panel="{ close }">
        <div class="align-menu"><button v-for="align in alignOptions" :key="align.value" :title="align.label" @click="emit('align', align.value); close()"><component :is="align.icon" class="h-4 w-4" /></button></div>
      </template>
    </ToolbarMenu>
    <button class="format-button" title="待办列表" @click="emit('insert-todo')"><ListTodo class="h-4 w-4" /></button>
    <button class="format-button" title="无序列表" @click="emit('run-command', 'insertUnorderedList')"><List class="h-4 w-4" /></button>
    <button class="format-button" title="有序列表" @click="emit('run-command', 'insertOrderedList')"><ListOrdered class="h-4 w-4" /></button>
    <button class="format-button" title="减少缩进" @click="emit('run-command', 'outdent')"><IndentDecrease class="h-4 w-4" /></button>
    <button class="format-button" title="增加缩进" @click="emit('run-command', 'indent')"><IndentIncrease class="h-4 w-4" /></button>
    <button class="format-button" title="查找和替换" @click="emit('toggle-find')"><FileSearch class="h-4 w-4" /></button>

    <div class="ml-auto flex items-center gap-1">
      <button class="mode-button" :class="viewMode === 'writing' ? 'mode-button-active' : ''" :title="viewMode === 'writing' ? '切换到阅读模式' : '切换到编辑模式'" @click="emit('toggle-mode')"><PenLine v-if="viewMode === 'writing'" class="h-4 w-4" /><Eye v-else class="h-4 w-4" /></button>
      <ToolbarMenu class="ml-1">
        <template #trigger="{ toggle }">
          <button class="icon-button" title="更多操作" @click="toggle"><MoreHorizontal class="h-[19px] w-[19px]" /></button>
        </template>
        <template #panel>
          <div class="more-menu">
            <button class="more-menu-item"><Library class="h-4 w-4" />加入知识库</button>
            <button class="more-menu-item"><Download class="h-4 w-4" />导出 Markdown</button>
            <button class="more-menu-item"><Trash2 class="h-4 w-4" />移至回收站</button>
          </div>
        </template>
      </ToolbarMenu>
      <button v-if="!rightPanelVisible" class="icon-button ml-1" title="展开上下文面板" @click="emit('update:rightPanelVisible', true)"><PanelRightOpen class="h-[18px] w-[18px]" /></button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  AlignCenter, AlignJustify, AlignLeft, AlignRight, ChevronDown, CirclePlus, Code2, Download, Eraser, Eye,
  FileSearch, Highlighter, ImagePlus, IndentDecrease, IndentIncrease, Library, Link, List, ListOrdered, ListTodo,
  Mic, Minus, MoreHorizontal, NotebookPen, Paintbrush, PanelRightOpen, Paperclip, PenLine, Quote, Redo2,
  Sigma, Strikethrough, Table2, Trash2, Undo2,
} from 'lucide-vue-next'
import type { ViewMode } from './types'
import ToolbarMenu from './ToolbarMenu.vue'

/**
 * 编辑器顶部工具栏。
 *
 * <p>本身不做编辑动作，只把用户意图用事件上抛给 `NoteEditor`；
 * 六个下拉菜单（插入 / 高亮 / 文字颜色 / 切换格式 / 对齐 / 更多）统一由
 * `ToolbarMenu` 承载，开关与「点击外部关闭」在里面走 `useDropdown`。
 */
defineProps<{
  viewMode: ViewMode
  rightPanelVisible: boolean
}>()

const emit = defineEmits<{
  'run-command': [command: string, value?: string]
  'format-block': [tag: string]
  'select-block': [tag: string]
  align: [command: string]
  'apply-color': [command: string, color: string]
  'insert-link': []
  'insert-wiki-link': []
  'insert-todo': []
  'insert-horizontal-rule': []
  'toggle-find': []
  'toggle-mode': []
  'update:rightPanelVisible': [visible: boolean]
}>()

const highlightColors = ['#fff0ae', '#baf0d0', '#bde5ff', '#ffd3d3', '#e6d5ff', '#ffe0ad', '#d9d9d9']
const textColors = ['#18181b', '#2563eb', '#059669', '#dc2626', '#9333ea', '#d97706', '#71717a']
const blockOptions = [
  { tag: 'h1', label: '标题' },
  { tag: 'h2', label: '标题 1' },
  { tag: 'h3', label: '标题 2' },
  { tag: 'p', label: '正文 1' },
  { tag: 'div', label: '正文 2' },
]
const alignOptions = [
  { value: 'justifyLeft', label: '左对齐', icon: AlignLeft },
  { value: 'justifyCenter', label: '居中', icon: AlignCenter },
  { value: 'justifyRight', label: '右对齐', icon: AlignRight },
  { value: 'justifyFull', label: '两端对齐', icon: AlignJustify },
]
</script>

<style scoped>
.icon-button, .mode-button, .format-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #71717a;
  transition: background-color .15s ease, color .15s ease;
}
.icon-button, .mode-button { height: 30px; width: 30px; }
.icon-button:hover, .mode-button:hover, .format-button:hover { background: #f0f0ee; color: #27272a; }
.mode-button-active { background: var(--mint); color: #247456; }
.editor-toolbar { display: flex; height: 50px; shrink: 0; align-items: center; gap: 2px; border-bottom: 1px solid #e8e8e8; background: #fff; padding: 0 18px; }
.format-button { height: 28px; width: 28px; font-size: 14px; }
.format-select { display: inline-flex; height: 28px; align-items: center; gap: 4px; border-radius: 5px; padding: 0 8px; font-size: 12px; color: #52525b; }
.format-select:hover, .format-select-active, .format-button-active { background: #f0f0ee; color: #27272a; }
.toolbar-divider { height: 18px; width: 1px; margin: 0 5px; background: #e4e4e7; }
.text-color-button { position: relative; font-family: Georgia, serif; font-size: 17px; }.text-color-button::after { position: absolute; right: 6px; bottom: 4px; left: 6px; height: 2px; background: #35a878; content: ''; }
.insert-menu, .block-menu, .align-menu, .color-menu { position: absolute; z-index: 50; top: calc(100% + 7px); left: 0; background: #fff; box-shadow: 0 12px 28px rgba(24, 24, 27, .14); }
.insert-menu { width: 196px; overflow: hidden; border-radius: 16px; padding: 8px 0; }.insert-menu-item { display: flex; width: 100%; align-items: center; gap: 12px; padding: 8px 15px; text-align: left; color: #3f3f46; font-size: 14px; }.insert-menu-item:hover { background: #f3f8f5; color: #247456; }.toolbar-menu-divider { height: 1px; margin: 5px 12px; background: #e8e8e8; }
.color-menu { width: 280px; border-radius: 16px; padding: 14px 16px; }.color-menu p { margin-bottom: 10px; color: #71717a; font-size: 13px; }.color-reset { width: 100%; border: 1px solid #e4e4e7; border-radius: 6px; padding: 7px; color: #52525b; font-size: 13px; }.color-reset:hover { background: #f6f6f5; }.color-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-top: 12px; }.color-grid button { width: 28px; height: 24px; border-radius: 3px; transition: transform .12s ease; }.color-grid button:hover { outline: 2px solid #3c9270; outline-offset: 1px; transform: scale(1.05); }
.block-menu { width: 150px; overflow: hidden; border-radius: 14px; padding: 8px; }.block-menu button { display: block; width: 100%; border-radius: 6px; padding: 6px 9px; text-align: left; color: #27272a; }.block-menu button:hover { background: #e8f5ee; color: #247456; }.block-menu-h1 { font-size: 25px; font-weight: 700; }.block-menu-h2 { font-size: 21px; font-weight: 700; }.block-menu-h3 { font-size: 17px; font-weight: 700; }.block-menu-p, .block-menu-div { font-size: 14px; }
.align-menu { width: 56px; overflow: hidden; border-radius: 14px; padding: 7px; }.align-menu button { display: flex; width: 100%; height: 30px; align-items: center; justify-content: center; border-radius: 5px; color: #52525b; }.align-menu button:hover { background: #e8f5ee; color: #247456; }
.more-menu { position: absolute; z-index: 40; top: calc(100% + 6px); right: 0; width: 166px; border: 1px solid #e4e4e7; border-radius: 7px; background: #fff; padding: 4px; box-shadow: 0 10px 24px rgba(24,24,27,.1); }
.more-menu-item { display: flex; width: 100%; align-items: center; gap: 8px; border-radius: 5px; padding: 8px; text-align: left; font-size: 13px; color: #52525b; }
.more-menu-item:hover { background: #f4f4f5; }
</style>

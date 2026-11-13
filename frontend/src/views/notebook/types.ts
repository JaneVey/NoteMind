import type { Id } from '@/types/common'

/**
 * 笔记本页（`views/Notebook.vue`）的私有视图模型。
 *
 * <p>它与 `@/types/note.ts` 是两个模型：后者描述后端接口形状（正文叫 `contentMd`、
 * 时间是 ISO-8601 字符串），这里是原型阶段的本地 mock 数据（正文叫 `content`、
 * 时间是用于比较的数值、列表文案是预生成的 `updatedLabel`），强行合并会改变页面行为。
 *
 * <p>后端笔记接口接通后，整体替换为 `@/types/note` 的 `Note` / `Folder`。
 */

/** 侧栏 Tab */
export type SideTab = 'folders' | 'recent' | 'search'

/** 编辑器视图模式 */
export type ViewMode = 'writing' | 'source' | 'reading'

/** 排序方式 */
export type SortOption =
  | 'titleAsc'
  | 'titleDesc'
  | 'updatedDesc'
  | 'updatedAsc'
  | 'createdDesc'
  | 'createdAsc'

/** 「最近编辑」的分组标题 */
export type RecentGroupLabel = '今天' | '过去 7 天' | '过去 30 天'

/** 侧栏列表里的一条笔记 */
export interface NotebookNote {
  id: Id
  title: string
  content: string
  folderId: Id | null
  updatedLabel: string
  recentGroup: RecentGroupLabel
  updatedAt: number
  createdAt: number
}

/** 笔记本页的文件夹（mock 数据只有 id 与 name） */
export interface NotebookFolder {
  id: Id
  name: string
  parentId: Id | null
}

/** 递归目录树的节点：文件夹 + 子文件夹 + 直属笔记（已排序） */
export interface FolderNode {
  folder: NotebookFolder
  children: FolderNode[]
  notes: NotebookNote[]
}

/** 选中项右下角菜单的动作 */
export type ItemAction = 'move' | 'merge' | 'rename' | 'delete'

/** 移动弹窗里的文件夹选项，path 是带上层级的展示文本 */
export interface MoveFolderOption {
  id: Id | null
  path: string
}

/** 编辑器上方的标签页 */
export interface NoteTab {
  id: Id
  title: string
}

/** 目录树里的一组：文件夹 + 其下笔记（已排序） */
export interface FolderGroup {
  folder: NotebookFolder
  notes: NotebookNote[]
}

/** 排序菜单项 */
export interface SortOptionItem {
  value: SortOption
  label: string
  dividerAfter: boolean
}

/** AI 会话里的一条消息 */
export interface ConversationMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
}

/** AI 会话框 */
export interface ConversationSession {
  id: string
  label: string
}

/** 对话历史条目 */
export interface ConversationHistoryItem {
  id: string
  title: string
  time: string
}

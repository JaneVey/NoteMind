import type { Id } from '@/types/common'
import type { NotebookFolder, NotebookNote, SortOption, SortOptionItem } from './types'

/**
 * 笔记本页列表相关的纯函数与常量。
 *
 * <p>侧栏（文件夹 / 最近编辑）与搜索面板都要用同一套排序菜单、同一套比较规则，
 * 也要把文件夹 id 翻成名字，所以放在这里共用 —— 避免两处各写一份、改一处漏一处。
 */

/** 排序菜单项 */
export const sortOptions: SortOptionItem[] = [
  { value: 'titleAsc', label: '文件名（A-Z）', dividerAfter: false },
  { value: 'titleDesc', label: '文件名（Z-A）', dividerAfter: true },
  { value: 'updatedDesc', label: '编辑时间（从新到旧）', dividerAfter: false },
  { value: 'updatedAsc', label: '编辑时间（从旧到新）', dividerAfter: true },
  { value: 'createdDesc', label: '创建时间（从新到旧）', dividerAfter: false },
  { value: 'createdAsc', label: '创建时间（从旧到新）', dividerAfter: false },
]

/** 按指定排序方式比较两条笔记 */
export function compareNotesBy(option: SortOption, left: NotebookNote, right: NotebookNote): number {
  switch (option) {
    case 'titleAsc': return left.title.localeCompare(right.title, 'zh-CN')
    case 'titleDesc': return right.title.localeCompare(left.title, 'zh-CN')
    case 'updatedDesc': return right.updatedAt - left.updatedAt
    case 'updatedAsc': return left.updatedAt - right.updatedAt
    case 'createdDesc': return right.createdAt - left.createdAt
    case 'createdAsc': return left.createdAt - right.createdAt
  }
}

/** 取某个文件夹下的笔记（按 option 排好序）。folderId 传 null 表示「未分类」 */
export function filterFolderNotes(notes: NotebookNote[], folderId: Id | null, option: SortOption): NotebookNote[] {
  return notes.filter((note) => note.folderId === folderId).sort((left, right) => compareNotesBy(option, left, right))
}

/** 文件夹 id → 名称；找不到时用兜底文案 */
export function findFolderName(folders: NotebookFolder[], folderId: Id | null): string {
  return folders.find((folder) => folder.id === folderId)?.name || '我的笔记'
}

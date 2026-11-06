import type { Id, IsoDateTime } from './common'

/**
 * 笔记模块类型，对应后端 `com.notemind.note` 与数据库表 `note_*`。
 *
 * <p>术语统一为「笔记本」（代码里的 `notebook`）。UI 文案不要再写「仓库」——
 * 中文开发语境里那几乎等同于 Git 仓库，也容易与 Obsidian 的 Vault 混淆。
 */

/** 笔记本（数据库 `note_notebook`） */
export interface Notebook {
  id: Id
  userId: Id
  name: string
  description: string | null
  icon: string | null
  sortOrder: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/** 文件夹（数据库 `note_folder`），支持多级 */
export interface Folder {
  id: Id
  userId: Id
  notebookId: Id
  /** 父文件夹；null 表示位于笔记本根层 */
  parentId: Id | null
  name: string
  sortOrder: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/** 笔记详情（数据库 `note`），正文用 `contentMd` */
export interface Note {
  id: Id
  userId: Id
  notebookId: Id
  folderId: Id | null
  title: string
  contentMd: string
  wordCount: number
  pinned: boolean
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * 笔记列表项。
 *
 * <p>列表接口不返回正文，只给前 200 个字符的 `excerpt` —— 一本几十万字的笔记
 * 没必要为了显示两行摘要把全文传过来。
 */
export interface NoteListItem {
  id: Id
  userId: Id
  notebookId: Id
  folderId: Id | null
  title: string
  excerpt: string | null
  wordCount: number
  pinned: boolean
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/** 链接/反链指向的笔记，右侧面板只需要 id 与标题 */
export interface NoteRef {
  id: Id
  title: string
}

/** 双向链接（数据库 `note_link`），由 `[[笔记名]]` 解析产生 */
export interface NoteLink {
  id: Id
  sourceNoteId: Id
  targetNoteId: Id
  linkType: string
  createdAt: IsoDateTime
}

export interface CreateNoteRequest {
  notebookId: Id
  folderId?: Id | null
  title?: string
  contentMd?: string
}

/** 部分更新；移动文件夹走 `moveNote`，不在这里传 folderId */
export interface UpdateNoteRequest {
  title?: string
  contentMd?: string
  pinned?: boolean
}

export interface MoveNoteRequest {
  notebookId: Id
  folderId: Id | null
}

export interface NotebookPayload {
  name: string
  description?: string | null
  icon?: string | null
  sortOrder?: number
}

/** 局部更新：只传要改的字段，description 传空串表示清空 */
export interface UpdateNotebookRequest {
  name?: string
  description?: string
  icon?: string
  sortOrder?: number
}

export interface FolderPayload {
  notebookId: Id
  parentId?: Id | null
  name: string
  sortOrder?: number
}

/** 知识图谱节点（由笔记与链接推导，非数据库表） */
export interface GraphNode {
  id: Id
  label: string
  /** 连接数，用于决定节点大小 */
  degree: number
}

export interface GraphEdge {
  source: Id
  target: Id
}

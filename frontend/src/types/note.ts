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

/**
 * 笔记（数据库 `note`）。
 *
 * <p>正文用 `contentMd`，与数据库列 `content_md`、Java 实体 `contentMd` 端到端同名 ——
 * 同一概念在 DB / Java / 前端各叫各的，读代码要做脑内映射，改代码容易漏改。
 */
export interface Note {
  id: Id
  userId: Id
  notebookId: Id
  folderId: Id | null
  title: string
  contentMd: string
  wordCount: number
  isPinned: boolean
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/** 双向链接（数据库 `note_link`），由 `[[笔记名]]` 解析产生 */
export interface NoteLink {
  id: Id
  sourceNoteId: Id
  targetNoteId: Id
  linkType: string
  createdAt: IsoDateTime
}

/** 新建/更新笔记的请求体 */
export interface SaveNoteRequest {
  notebookId?: Id
  folderId?: Id | null
  title?: string
  contentMd?: string
  isPinned?: boolean
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

import type { Id, IsoDateTime } from './common'

/**
 * 笔记模块类型。
 *
 * <p>与后端 `com.notemind.note` 模块、数据库表 `note_*` 对应。
 *
 * <p><b>术语统一</b>：项目此前的 UI 文案用「仓库」，但代码是 `notebook`。
 * 现统一为「笔记本」—— 「仓库」在中文开发语境里几乎等同于 Git 仓库，跨层沟通有歧义，
 * 且容易与 Obsidian 的 Vault 概念混淆。
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
 * <p><b>字段名说明</b>：正文用 `contentMd` 而不是 `content` ——
 * 与数据库列 `content_md`、Java 实体 `contentMd` 保持**端到端同名**。
 * 项目此前的教训就是"同一概念在 DB / Java / 前端 / UI 文案里各叫各的"，
 * 读代码要在脑子里做映射，改代码容易漏改。
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

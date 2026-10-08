import type { Id, IsoDateTime } from './common'

/**
 * 知识库模块类型。
 *
 * <p>与后端 `com.notemind.knowledge` 模块、数据库表 `knowledge_*` 对应。
 */

/** 知识库（数据库 `knowledge_base`） */
export interface KnowledgeBase {
  id: Id
  userId: Id
  name: string
  description: string | null
  icon: string | null
  sortOrder: number
  /** 文档数量（由后端聚合返回，非表字段） */
  documentCount: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * 文档处理状态。
 *
 * <p><b>变更记录</b>：此前定义了 **7 个值**，把同义词全列上了
 * （`completed` / `done` / `parsed`），理由是"以防万一"。
 * 这让类型彻底失去约束力 —— 后端只认 4 个状态，多出来的值永远不会出现，
 * 却让每个用到它的 `switch` 都要写一堆死分支。
 *
 * <p>状态机：`pending` → `processing` → `done` | `failed`
 */
export type DocumentStatus = 'pending' | 'processing' | 'done' | 'failed'

/**
 * 知识库文档（数据库 `knowledge_document`）。
 *
 * <p><b>变更记录</b>：此前同时有 `name`/`fileName` 与 `type`/`fileType`
 * 与 `size`/`fileSize` 三组重复字段，属于契约未冻结时的对冲。
 * 现统一取与数据库列同名的一组。
 */
export interface KnowledgeDocument {
  id: Id
  knowledgeBaseId: Id
  userId: Id
  /** 关联的笔记 id（把笔记也纳入知识库时使用） */
  noteId: Id | null
  fileName: string
  /** 扩展名，如 pdf / docx / md */
  fileType: string
  /** 字节数。数据库 BIGINT，前端展示时用 formatFileSize 转换 */
  fileSize: number
  storagePath: string
  chunkStatus: DocumentStatus
  errorMessage: string | null
  importedAt: IsoDateTime | null
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * 文档详情（**仅详情接口返回**）。
 *
 * <p><b>为什么要和列表类型分开</b>：文档解析出的纯文本可能很大（一本 PDF 几十万字），
 * 列表接口一次返回 20 条就会传输几 MB 而其中 99% 用不到。
 *
 * <p>所以约定：**列表接口不返回 `rawContent`，详情接口才返回**。
 * 用独立的类型而不是把字段标成可选 —— 后者会让调用方分不清
 * "这个字段可能没有"和"这个接口不返回它"。
 */
export interface KnowledgeDocumentDetail extends KnowledgeDocument {
  /** 解析出的纯文本 */
  rawContent: string | null
  /** 知识块数量（切分完成后才有） */
  chunkCount: number
}

/**
 * 知识块（数据库 `knowledge_chunk`）。
 *
 * <p>由文档切分而来，是 RAG 检索的最小单位。**前端一般不直接消费**，
 * 只有在展示"引用来源"时用到其 `sourceInfo`。
 */export interface KnowledgeChunk {
  id: Id
  documentId: Id
  knowledgeBaseId: Id
  chunkIndex: number
  chunkContent: string
  chunkTokenCount: number
  /** 引用溯源信息，用于回答里的"来自《XX.pdf》第 37 页" */
  sourceInfo: ChunkSourceInfo
}

/**
 * 引用溯源信息（数据库 `knowledge_chunk.source_info`，JSONB）。
 *
 * <p>它的字段名在《数据库设计规范》5.2 节有明确约定 ——
 * JSONB 灵活但没有字段约定就没有契约，后端写什么、前端怎么读全靠口头约定。
 */
export interface ChunkSourceInfo {
  sourceType: 'document' | 'note'
  documentId: Id | null
  noteId: Id | null
  /** 展示用文件名 */
  fileName: string
  /** 起始页（文档类才有） */
  pageStart: number | null
  pageEnd: number | null
  /** 标题层级路径，便于展示"来自第 3 章 > 事务管理" */
  headingPath: string[]
  chunkIndex: number
  /** 检索时的相似度，写入时可能为空 */
  score: number | null
}

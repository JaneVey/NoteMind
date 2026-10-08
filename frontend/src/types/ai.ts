import type { Id, IsoDateTime } from './common'
import type { ChunkSourceInfo } from './knowledge'

/**
 * AI 模块类型。
 *
 * <p>与后端 `com.notemind.ai` 模块、数据库表 `ai_*` / `prompt_shortcut` 对应。
 */

/**
 * AI 供应商配置（数据库 `ai_config`）。
 *
 * <p><b>注意</b>：`apiKey` **只会在创建/更新时提交，读取时后端只回显后四位**
 * （《后端开发规范》安全条款：敏感数据展示必须脱敏）。
 */
export interface AiConfig {
  id: Id
  userId: Id
  /** 供应商标识：siliconflow / deepseek / openai ... */
  providerName: string
  /** 展示名 */
  displayName: string
  /** 脱敏后的 Key，如 `sk-***abcd` */
  apiKeyMasked?: string
  apiBaseUrl: string
  /** 对话模型，如 deepseek-ai/DeepSeek-V4-Flash */
  chatModel: string
  /** 向量模型，如 BAAI/bge-m3（1024 维） */
  embeddingModel: string
  isActive: boolean
  sortOrder: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * 会话（数据库 `ai_conversation`）。
 *
 * <p><b>类型名说明</b>：后端实体叫 `AiConversation`，前端这里叫 `Conversation` ——
 * 因为在前端语境里 `ai/` 目录已经限定了范围，加前缀是冗余。
 * 这类"跨层不同名"必须登记在《项目结构与命名规范》的概念词典里，否则就是隐患。
 */
export interface Conversation {
  id: Id
  userId: Id
  title: string
  /** 绑定的知识库；为 null 表示普通对话（不检索） */
  knowledgeBaseId: Id | null
  providerName: string
  model: string
  /** 对话模式：chat 普通对话 / rag 知识库检索增强 */
  mode: 'chat' | 'rag'
  isFavorite: boolean
  messageCount: number
  tokenUsage: number
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

export type MessageRole = 'user' | 'assistant' | 'system'

/** 消息（数据库 `ai_message`） */
export interface ChatMessage {
  id?: Id
  conversationId?: Id
  role: MessageRole
  content: string
  /**
   * 思考链内容。
   *
   * <p>与正文分开存储：它可能和正文一样长，放进 `metadata` 会让 JSONB 显著膨胀，
   * 因此数据库里是独立列 `reasoning_content`。
   *
   * <p>注意：**流式响应默认不返回思考链**，必须显式传参
   * （见后端 `app.ai.thinking-param` 配置），这是供应商侧的行为差异。
   */
  reasoningContent?: string
  tokens?: number
  /** 引用来源（RAG 模式下才有） */
  citations?: ChunkSourceInfo[]
  createdAt?: IsoDateTime
  /** 仅前端使用的流式状态 */
  streaming?: boolean
  /** 仅前端使用：错误提示 */
  error?: string
}

/**
 * 快捷指令（数据库 `prompt_shortcut`）。
 *
 * <p>`prompt` 里可以包含占位符 `{selection}` —— 选中文本会被替换进去。
 * **注意不是 `{content}`**：项目此前 `init.sql` 与设计文档用了不同的占位符，
 * 不改的话 8 个预设指令会把字面量 `{content}` 直接发给大模型，静默失效。
 */
export interface PromptShortcut {
  id: Id
  userId: Id
  name: string
  prompt: string
  description: string | null
  /** 是否为系统预设（预设不可删除） */
  isPreset: boolean
  sortOrder: number
  isEnabled: boolean
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * SSE 流式分片。
 *
 * <p>与后端 `ChatStreamChunk` 一一对应，`type` 决定如何消费：
 * - `reasoning` → 追加到思考链（默认折叠展示）
 * - `content`   → 追加到正文
 * - `done`      → **显式结束标记**，前端必须据此收尾
 * - `error`     → 错误信息
 *
 * <p><b>为什么必须有 `done`</b>：Vite 开发代理**不会关闭 SSE 连接**
 * （直连后端 2.7 秒正常关闭，经代理会一直挂到超时）。
 * 如果前端只靠"流关闭"判断结束，界面会永远停在"生成中"。
 */
export interface ChatStreamChunk {
  type: 'reasoning' | 'content' | 'done' | 'error'
  text: string
}

/** 发起流式对话的请求体 */
export interface ChatRequest {
  content: string
  conversationId?: Id
  /** 指定知识库则走 RAG 检索 */
  knowledgeBaseId?: Id
}

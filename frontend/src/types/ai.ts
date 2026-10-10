import type { Id, IsoDateTime } from './common'
import type { ChunkSourceInfo } from './knowledge'

/**
 * AI 模块类型，对应后端 `com.notemind.ai` 与数据库表 `ai_*` / `prompt_shortcut`。
 */

/**
 * AI 供应商配置（数据库 `ai_config`）。
 *
 * <p>`apiKey` 只在创建/更新时提交，读取时后端只回显后四位。
 */
export interface AiConfig {
  id: Id
  userId: Id
  /** 供应商标识：siliconflow / deepseek / openai ... */
  providerName: string
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

/** 会话（数据库 `ai_conversation`）。后端实体叫 `AiConversation`，前端不加 `Ai` 前缀 */
export interface Conversation {
  id: Id
  userId: Id
  title: string
  /** 绑定的知识库；为 null 表示普通对话（不检索） */
  knowledgeBaseId: Id | null
  providerName: string
  model: string
  /** chat 普通对话 / rag 知识库检索增强 */
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
   * 思考链。数据库里是独立列 `reasoning_content` —— 它可能和正文一样长，
   * 塞进 JSONB 的 `metadata` 会让它显著膨胀。
   *
   * <p>流式响应默认不返回思考链，必须显式传参（见后端 `app.ai.thinking-param`）。
   */
  reasoningContent?: string
  tokens?: number
  /** 引用来源（RAG 模式下才有） */
  citations?: ChunkSourceInfo[]
  createdAt?: IsoDateTime
  /** 仅前端使用的流式状态 */
  streaming?: boolean
  /** 仅前端使用 */
  error?: string
}

/**
 * 快捷指令（数据库 `prompt_shortcut`）。
 *
 * <p>`prompt` 里的占位符是 `{selection}`，**不是 `{content}`** ——
 * 写错的话预设指令会把字面量直接发给大模型。
 */
export interface PromptShortcut {
  id: Id
  userId: Id
  name: string
  prompt: string
  description: string | null
  /** 系统预设不可删除 */
  isPreset: boolean
  sortOrder: number
  isEnabled: boolean
  createdAt: IsoDateTime
  updatedAt: IsoDateTime
}

/**
 * SSE 流式分片，与后端 `ChatStreamChunk` 一一对应。
 *
 * <p>`done` 是显式结束标记，前端必须据此收尾：Vite 开发代理不会关闭 SSE 连接，
 * 只靠"流关闭"判断结束会让界面永远停在"生成中"。
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

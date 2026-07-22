export type Id = string | number

export interface Notebook {
  id: Id
  name: string
  folders?: Folder[]
  [key: string]: unknown
}

export interface Folder {
  id: Id
  notebookId?: Id
  name: string
  [key: string]: unknown
}

export interface Note {
  id: Id
  folderId?: Id
  title: string
  content?: string
  updatedAt?: string
  createdAt?: string
  [key: string]: unknown
}

export interface KnowledgeBase {
  id: Id
  name: string
  description?: string
  documentCount?: number
  [key: string]: unknown
}

export type DocumentStatus = 'pending' | 'processing' | 'completed' | 'done' | 'parsed' | 'failed' | 'error'

export interface KnowledgeDocument {
  id: Id
  name: string
  fileName?: string
  type?: string
  fileType?: string
  size?: number
  fileSize?: number
  status: DocumentStatus
  content?: string
  createdAt?: string
  [key: string]: unknown
}

export interface Conversation {
  id: Id
  title: string
  isFavorite?: boolean
  updatedAt?: string
  createdAt?: string
  [key: string]: unknown
}

export type MessageRole = 'user' | 'assistant' | 'system'

export interface ChatMessage {
  id?: Id
  role: MessageRole
  content: string
  createdAt?: string
}

export interface UserInfo {
  userId: Id | null
  username: string
  nickname: string
  avatar: string
}

export interface LoginRequest { username: string; password: string }
export interface RegisterRequest extends LoginRequest { email: string; nickname: string }
export interface LoginResponse { token: string; userId?: Id; username?: string; nickname?: string; avatar?: string; user?: Partial<UserInfo> }

export interface AiConfig { id: Id; name: string; provider?: string; apiKey?: string; baseUrl?: string; model?: string; active?: boolean; [key: string]: unknown }
export interface PromptShortcut { id: Id; name: string; prompt: string; [key: string]: unknown }

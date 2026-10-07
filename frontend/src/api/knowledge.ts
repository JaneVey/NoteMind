import request from './request'
import type { Id, KnowledgeBase, KnowledgeDocument } from '@/types/domain'

export interface KnowledgeBasePayload { name: string; description?: string }
export function getKnowledgeBases(): Promise<KnowledgeBase[]> { return request.get('/knowledge/bases') }
export function createKnowledgeBase(data: KnowledgeBasePayload): Promise<KnowledgeBase> { return request.post('/knowledge/base', data) }
export function deleteKnowledgeBase(id: Id): Promise<void> { return request.delete(`/knowledge/base/${id}`) }
export function getDocuments(kbId: Id): Promise<KnowledgeDocument[]> { return request.get('/knowledge/documents', { params: { kbId } }) }
export function uploadDocument(kbId: Id, file: File): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', String(kbId))
  return request.post('/knowledge/document/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export function deleteDocument(id: Id): Promise<void> { return request.delete(`/knowledge/document/${id}`) }
export function reparseDocument(id: Id): Promise<void> { return request.post(`/knowledge/document/${id}/reparse`) }

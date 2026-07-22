import { defineStore } from 'pinia'
import { ref } from 'vue'
import { createKnowledgeBase as createBaseApi, deleteDocument as deleteDocApi, deleteKnowledgeBase as deleteBaseApi, getDocuments as fetchDocsApi, getKnowledgeBases as fetchBasesApi, reparseDocument as reparseDocApi, uploadDocument as uploadDocApi } from '@/api/knowledge'
import type { Id, KnowledgeBase, KnowledgeDocument } from '@/types/domain'

export const useKnowledgeStore = defineStore('knowledge', () => {
  const knowledgeBases = ref<KnowledgeBase[]>([])
  const currentKbId = ref<Id | null>(null)
  const documents = ref<KnowledgeDocument[]>([])
  const currentDocument = ref<KnowledgeDocument | null>(null)
  const loading = ref(false)
  async function fetchKnowledgeBases(): Promise<void> { loading.value = true; try { knowledgeBases.value = await fetchBasesApi() } catch { console.error('获取知识库列表失败') } finally { loading.value = false } }
  async function createKnowledgeBase(data: { name: string; description?: string }): Promise<KnowledgeBase | null> { try { const base = await createBaseApi(data); knowledgeBases.value.push(base); return base } catch { console.error('创建知识库失败'); return null } }
  async function deleteKnowledgeBase(id: Id): Promise<void> { try { await deleteBaseApi(id); knowledgeBases.value = knowledgeBases.value.filter((item) => item.id !== id); if (currentKbId.value === id) { currentKbId.value = null; documents.value = [] } } catch { console.error('删除知识库失败') } }
  function selectKnowledgeBase(id: Id): void { currentKbId.value = id; void fetchDocuments(id) }
  async function fetchDocuments(id: Id): Promise<void> { loading.value = true; try { documents.value = await fetchDocsApi(id) } catch { console.error('获取文档列表失败') } finally { loading.value = false } }
  async function uploadDocument(id: Id, file: File): Promise<KnowledgeDocument | null> { try { const document = await uploadDocApi(id, file); documents.value.push(document); return document } catch { console.error('上传失败'); return null } }
  async function deleteDocument(id: Id): Promise<void> { try { await deleteDocApi(id); documents.value = documents.value.filter((item) => item.id !== id) } catch { console.error('删除失败') } }
  async function reparseDocument(id: Id): Promise<void> { try { await reparseDocApi(id); const document = documents.value.find((item) => item.id === id); if (document) document.status = 'processing' } catch { console.error('重新解析失败') } }
  function backToList(): void { currentKbId.value = null; documents.value = [] }
  return { knowledgeBases, currentKbId, documents, currentDocument, loading, fetchKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase, selectKnowledgeBase, fetchDocuments, uploadDocument, deleteDocument, reparseDocument, backToList }
})

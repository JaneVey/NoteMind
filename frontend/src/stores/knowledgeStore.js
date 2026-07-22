import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getKnowledgeBases as fetchBasesApi,
  createKnowledgeBase as createBaseApi,
  deleteKnowledgeBase as deleteBaseApi,
  getDocuments as fetchDocsApi,
  uploadDocument as uploadDocApi,
  deleteDocument as deleteDocApi,
  reparseDocument as reparseDocApi,
} from '@/api/knowledge'

export const useKnowledgeStore = defineStore('knowledge', () => {
  /* ========== State ========== */
  const knowledgeBases = ref([])
  const currentKbId = ref(null)
  const documents = ref([])
  const currentDocument = ref(null)
  const loading = ref(false)

  /* ========== Actions ========== */

  /** 获取所有知识库 */
  async function fetchKnowledgeBases() {
    loading.value = true
    try {
      knowledgeBases.value = await fetchBasesApi()
    } catch (e) {
      console.error('获取知识库列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 创建知识库 */
  async function createKnowledgeBase(data) {
    try {
      const kb = await createBaseApi(data)
      knowledgeBases.value.push(kb)
      return kb
    } catch (e) {
      console.error('创建知识库失败')
      return null
    }
  }

  /** 删除知识库 */
  async function deleteKnowledgeBase(id) {
    try {
      await deleteBaseApi(id)
      knowledgeBases.value = knowledgeBases.value.filter((k) => k.id !== id)
      if (currentKbId.value === id) {
        currentKbId.value = null
        documents.value = []
      }
    } catch (e) {
      console.error('删除知识库失败')
    }
  }

  /** 选择知识库并加载文档 */
  function selectKnowledgeBase(kbId) {
    currentKbId.value = kbId
    fetchDocuments(kbId)
  }

  /** 获取文档列表 */
  async function fetchDocuments(kbId) {
    loading.value = true
    try {
      documents.value = await fetchDocsApi(kbId)
    } catch (e) {
      console.error('获取文档列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 上传文档 */
  async function uploadDocument(kbId, file) {
    try {
      const doc = await uploadDocApi(kbId, file)
      documents.value.push(doc)
      console.log('上传成功')
      return doc
    } catch (e) {
      console.error('上传失败')
      return null
    }
  }

  /** 删除文档 */
  async function deleteDocument(docId) {
    try {
      await deleteDocApi(docId)
      documents.value = documents.value.filter((d) => d.id !== docId)
      console.log('删除成功')
    } catch (e) {
      console.error('删除失败')
    }
  }

  /** 重新解析 */
  async function reparseDocument(docId) {
    try {
      await reparseDocApi(docId)
      const doc = documents.value.find((d) => d.id === docId)
      if (doc) doc.status = 'processing'
      console.log('已重新加入解析队列')
    } catch (e) {
      console.error('重新解析失败')
    }
  }

  /** 关闭详情回到列表 */
  function backToList() {
    currentKbId.value = null
    documents.value = []
  }

  return {
    // state
    knowledgeBases,
    currentKbId,
    documents,
    currentDocument,
    loading,
    // actions
    fetchKnowledgeBases,
    createKnowledgeBase,
    deleteKnowledgeBase,
    selectKnowledgeBase,
    fetchDocuments,
    uploadDocument,
    deleteDocument,
    reparseDocument,
    backToList,
  }
})

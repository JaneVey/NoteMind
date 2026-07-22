import request from './request'

/**
 * 获取所有知识库
 */
export function getKnowledgeBases() {
  return request.get('/knowledge/bases')
}

/**
 * 创建知识库
 */
export function createKnowledgeBase(data) {
  return request.post('/knowledge/base', data)
}

/**
 * 删除知识库
 */
export function deleteKnowledgeBase(id) {
  return request.delete(`/knowledge/base/${id}`)
}

/**
 * 获取知识库下的文档列表
 */
export function getDocuments(kbId) {
  return request.get('/knowledge/documents', { params: { kbId } })
}

/**
 * 上传文档到知识库（FormData）
 */
export function uploadDocument(kbId, file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('kbId', kbId)
  return request.post('/knowledge/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 删除文档
 */
export function deleteDocument(id) {
  return request.delete(`/knowledge/document/${id}`)
}

/**
 * 重新解析文档
 */
export function reparseDocument(id) {
  return request.post(`/knowledge/document/${id}/reparse`)
}

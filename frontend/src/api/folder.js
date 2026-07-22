import request from './request'

/**
 * 获取笔记本下的文件夹列表
 */
export function getFolders(notebookId) {
  return request.get('/folder/list', { params: { notebookId } })
}

/**
 * 创建文件夹
 */
export function createFolder(data) {
  return request.post('/folder', data)
}

/**
 * 更新文件夹
 */
export function updateFolder(id, data) {
  return request.put(`/folder/${id}`, data)
}

/**
 * 删除文件夹
 */
export function deleteFolder(id) {
  return request.delete(`/folder/${id}`)
}

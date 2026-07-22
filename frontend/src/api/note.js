import request from './request'

/**
 * 获取所有笔记本（仓库）
 */
export function getNotebooks() {
  return request.get('/note/notebooks')
}

/**
 * 获取指定文件夹下的笔记列表
 */
export function getNotes(folderId) {
  return request.get('/note/list', { params: { folderId } })
}

/**
 * 获取单篇笔记详情
 */
export function getNote(id) {
  return request.get(`/note/${id}`)
}

/**
 * 创建笔记
 */
export function createNote(data) {
  return request.post('/note', data)
}

/**
 * 更新笔记
 */
export function updateNote(id, data) {
  return request.put(`/note/${id}`, data)
}

/**
 * 删除笔记
 */
export function deleteNote(id) {
  return request.delete(`/note/${id}`)
}

import request from './request'
import type { Id } from '@/types/common'
import type { Folder, FolderPayload } from '@/types/note'

export function getFolders(notebookId: Id): Promise<Folder[]> {
  return request.get('/folders', { params: { notebookId } })
}

export function createFolder(data: FolderPayload): Promise<Folder> {
  return request.post('/folders', data)
}

export function updateFolder(id: Id, data: Partial<FolderPayload>): Promise<Folder> {
  return request.put(`/folders/${id}`, data)
}

export function deleteFolder(id: Id): Promise<void> {
  return request.delete(`/folders/${id}`)
}

import request from './request'
import type { Id } from '@/types/common'
import type { Folder } from '@/types/note'

export type FolderPayload = Pick<Folder, 'name'> & Partial<Pick<Folder, 'notebookId'>>
export function getFolders(notebookId: Id): Promise<Folder[]> { return request.get('/folder/list', { params: { notebookId } }) }
export function createFolder(data: FolderPayload): Promise<Folder> { return request.post('/folder', data) }
export function updateFolder(id: Id, data: Partial<FolderPayload>): Promise<Folder> { return request.put(`/folder/${id}`, data) }
export function deleteFolder(id: Id): Promise<void> { return request.delete(`/folder/${id}`) }

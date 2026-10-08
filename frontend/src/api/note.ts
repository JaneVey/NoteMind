import request from './request'
import type { Id } from '@/types/common'
import type { Folder, Note, Notebook, SaveNoteRequest } from '@/types/note'

/**
 * 新建/更新笔记的请求体。
 *
 * <p>直接用类型文件里的 `SaveNoteRequest`，而不是在这里 `Pick<Note, ...>` ——
 * 后者会让"接口契约"散落在 api 层，改一个字段要同时找类型文件和 api 文件。
 */
export type NotePayload = SaveNoteRequest

export function getNotebooks(): Promise<Notebook[]> {
  return request.get('/note/notebooks')
}

export function getNotes(folderId: Id): Promise<Note[]> {
  return request.get('/note/list', { params: { folderId } })
}

export function getNote(id: Id): Promise<Note> {
  return request.get(`/note/${id}`)
}

export function createNote(data: NotePayload): Promise<Note> {
  return request.post('/note', data)
}

export function updateNote(id: Id, data: Partial<NotePayload>): Promise<Note> {
  return request.put(`/note/${id}`, data)
}

export function deleteNote(id: Id): Promise<void> {
  return request.delete(`/note/${id}`)
}

export type { Folder }

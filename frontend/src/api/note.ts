import request from './request'
import type { Folder, Id, Notebook, Note } from '@/types/domain'

export type NotePayload = Pick<Note, 'title' | 'content'> & Partial<Pick<Note, 'folderId'>>
export function getNotebooks(): Promise<Notebook[]> { return request.get('/note/notebooks') }
export function getNotes(folderId: Id): Promise<Note[]> { return request.get('/note/list', { params: { folderId } }) }
export function getNote(id: Id): Promise<Note> { return request.get(`/note/${id}`) }
export function createNote(data: NotePayload): Promise<Note> { return request.post('/note', data) }
export function updateNote(id: Id, data: Partial<NotePayload>): Promise<Note> { return request.put(`/note/${id}`, data) }
export function deleteNote(id: Id): Promise<void> { return request.delete(`/note/${id}`) }
export type { Folder }

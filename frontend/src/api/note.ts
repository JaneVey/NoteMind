import request from './request'
import type { Id } from '@/types/common'
import type {
  CreateNoteRequest, MoveNoteRequest, Note, NoteListItem, NoteRef, UpdateNoteRequest,
} from '@/types/note'

export function getNotes(
  notebookId: Id,
  folderId?: Id | null,
  keyword?: string,
): Promise<NoteListItem[]> {
  return request.get('/notes', {
    params: {
      notebookId,
      folderId: folderId ?? undefined,
      keyword: keyword || undefined,
    },
  })
}

export function getNote(id: Id): Promise<Note> {
  return request.get(`/notes/${id}`)
}

export function createNote(data: CreateNoteRequest): Promise<Note> {
  return request.post('/notes', data)
}

export function updateNote(id: Id, data: UpdateNoteRequest): Promise<Note> {
  return request.patch(`/notes/${id}`, data)
}

export function deleteNote(id: Id): Promise<void> {
  return request.delete(`/notes/${id}`)
}

export function moveNote(id: Id, data: MoveNoteRequest): Promise<Note> {
  return request.post(`/notes/${id}/move`, data)
}

export function getNoteLinks(id: Id): Promise<NoteRef[]> {
  return request.get(`/notes/${id}/links`)
}

export function getNoteBacklinks(id: Id): Promise<NoteRef[]> {
  return request.get(`/notes/${id}/backlinks`)
}

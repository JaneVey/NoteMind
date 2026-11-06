import request from './request'
import type { Id } from '@/types/common'
import type { Notebook, NotebookPayload, UpdateNotebookRequest } from '@/types/note'

export function getNotebooks(): Promise<Notebook[]> {
  return request.get('/notebooks')
}

export function createNotebook(data: NotebookPayload): Promise<Notebook> {
  return request.post('/notebooks', data)
}

export function updateNotebook(id: Id, data: UpdateNotebookRequest): Promise<Notebook> {
  return request.patch(`/notebooks/${id}`, data)
}

export function deleteNotebook(id: Id, force = false): Promise<void> {
  return request.delete(`/notebooks/${id}`, { params: force ? { force: true } : undefined })
}

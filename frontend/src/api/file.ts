import request from './request'

export type UploadScope = 'notebook-icon' | 'note-image' | 'avatar'

export interface UploadResult {
  url: string
}

export function uploadImage(file: File, scope: UploadScope = 'notebook-icon'): Promise<UploadResult> {
  const form = new FormData()
  form.append('file', file)
  return request.post('/files/images', form, { params: { scope } })
}

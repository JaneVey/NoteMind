import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { createFolder as createFolderApi, deleteFolder as deleteFolderApi, getFolders as fetchFoldersApi } from '@/api/folder'
import { createNote as createNoteApi, deleteNote as deleteNoteApi, getNote as fetchNoteApi, getNotebooks as fetchNotebooksApi, getNotes as fetchNotesApi, updateNote as updateNoteApi } from '@/api/note'
import type { Folder, Id, Notebook, Note } from '@/types/domain'

export const useNoteStore = defineStore('note', () => {
  const notebooks = ref<Notebook[]>([])
  const currentNotebookId = ref<Id | null>(null)
  const folders = ref<Folder[]>([])
  const currentFolderId = ref<Id | null>(null)
  const notes = ref<Note[]>([])
  const currentNote = ref<Note | null>(null)
  const loading = ref(false)
  const currentNotebook = computed(() => notebooks.value.find((item) => item.id === currentNotebookId.value) ?? null)
  const currentFolder = computed(() => folders.value.find((item) => item.id === currentFolderId.value) ?? null)
  async function fetchNotebooks(): Promise<void> { loading.value = true; try { notebooks.value = await fetchNotebooksApi(); if (notebooks.value.length && !currentNotebookId.value) currentNotebookId.value = notebooks.value[0].id } catch { console.error('获取笔记本列表失败') } finally { loading.value = false } }
  async function fetchFolders(id: Id): Promise<void> { loading.value = true; try { folders.value = await fetchFoldersApi(id) } catch { console.error('获取文件夹列表失败') } finally { loading.value = false } }
  function selectNotebook(id: Id): void { currentNotebookId.value = id; currentFolderId.value = null; notes.value = []; currentNote.value = null; void fetchFolders(id) }
  function selectFolder(id: Id): void { currentFolderId.value = id; currentNote.value = null; notes.value = []; void fetchNotes(id) }
  async function fetchNotes(id: Id): Promise<void> { loading.value = true; try { notes.value = await fetchNotesApi(id) } catch { console.error('获取笔记列表失败') } finally { loading.value = false } }
  async function openNote(id: Id): Promise<void> { loading.value = true; try { currentNote.value = await fetchNoteApi(id) } catch { console.error('获取笔记详情失败') } finally { loading.value = false } }
  async function createNote(data: Pick<Note, 'title' | 'content'> & Partial<Pick<Note, 'folderId'>>): Promise<Note | null> { try { const note = await createNoteApi(data); notes.value.unshift(note); return note } catch { console.error('创建笔记失败'); return null } }
  async function saveNote(id: Id, data: Partial<Pick<Note, 'title' | 'content' | 'folderId'>>): Promise<boolean> { try { const updated = await updateNoteApi(id, data); currentNote.value = updated; const index = notes.value.findIndex((item) => item.id === id); if (index >= 0) notes.value[index] = { ...notes.value[index], ...updated }; return true } catch { console.error('保存笔记失败'); return false } }
  async function createFolder(data: Pick<Folder, 'name'> & Partial<Pick<Folder, 'notebookId'>>): Promise<Folder | null> { try { const folder = await createFolderApi(data); folders.value.push(folder); return folder } catch { console.error('创建文件夹失败'); return null } }
  async function deleteFolder(id: Id): Promise<void> { try { await deleteFolderApi(id); folders.value = folders.value.filter((item) => item.id !== id); if (currentFolderId.value === id) { currentFolderId.value = null; notes.value = [] } } catch { console.error('删除文件夹失败') } }
  async function deleteNote(id: Id): Promise<void> { try { await deleteNoteApi(id); notes.value = notes.value.filter((item) => item.id !== id); if (currentNote.value?.id === id) currentNote.value = null } catch { console.error('删除笔记失败') } }
  function renameNotebook(id: Id, name: string): void { const notebook = notebooks.value.find((item) => item.id === id); if (notebook) notebook.name = name }
  return { notebooks, currentNotebookId, folders, currentFolderId, notes, currentNote, loading, currentNotebook, currentFolder, fetchNotebooks, fetchFolders, selectNotebook, selectFolder, fetchNotes, openNote, createNote, saveNote, createFolder, deleteFolder, deleteNote, renameNotebook }
})

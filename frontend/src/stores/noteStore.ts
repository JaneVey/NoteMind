import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ApiError } from '@/api/request'
import {
  createFolder as createFolderApi, deleteFolder as deleteFolderApi,
  getFolders as fetchFoldersApi, updateFolder as updateFolderApi,
} from '@/api/folder'
import {
  createNote as createNoteApi, deleteNote as deleteNoteApi, getNote as fetchNoteApi,
  getNotes as fetchNotesApi, moveNote as moveNoteApi, updateNote as updateNoteApi,
} from '@/api/note'
import {
  createNotebook as createNotebookApi, deleteNotebook as deleteNotebookApi,
  getNotebooks as fetchNotebooksApi, updateNotebook as updateNotebookApi,
} from '@/api/notebook'
import type { Id } from '@/types/common'
import type {
  Folder, FolderPayload, MoveNoteRequest, Note, NoteListItem, Notebook, NotebookPayload,
  UpdateNoteRequest,
} from '@/types/note'

export const useNoteStore = defineStore('note', () => {
  const notebooks = ref<Notebook[]>([])
  const currentNotebookId = ref<Id | null>(null)
  const folders = ref<Folder[]>([])
  const notes = ref<NoteListItem[]>([])
  const currentNote = ref<Note | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)
  /** 最近一次失败的业务错误码，供调用方对 3004 之类的分支做判断 */
  const errorCode = ref<number | null>(null)

  const currentNotebook = computed(
    () => notebooks.value.find((item) => item.id === currentNotebookId.value) ?? null,
  )

  function fail(action: string, cause: unknown): void {
    errorCode.value = cause instanceof ApiError ? cause.code : null
    error.value = cause instanceof Error ? `${action}：${cause.message}` : `${action}失败`
  }

  async function loadNotebookData(): Promise<void> {
    if (currentNotebookId.value === null) return
    loading.value = true
    try {
      const [folderList, noteList] = await Promise.all([
        fetchFoldersApi(currentNotebookId.value),
        fetchNotesApi(currentNotebookId.value),
      ])
      folders.value = folderList
      notes.value = noteList
    } catch (cause) {
      fail('加载笔记数据', cause)
    } finally {
      loading.value = false
    }
  }

  async function bootstrap(): Promise<void> {
    loading.value = true
    try {
      notebooks.value = await fetchNotebooksApi()
      if (notebooks.value.length > 0) {
        currentNotebookId.value = notebooks.value[0].id
      }
    } catch (cause) {
      fail('加载笔记本', cause)
      return
    } finally {
      loading.value = false
    }
    await loadNotebookData()
  }

  async function selectNotebook(id: Id): Promise<void> {
    if (currentNotebookId.value === id) return
    currentNotebookId.value = id
    currentNote.value = null
    notes.value = []
    folders.value = []
    await loadNotebookData()
  }

  async function createNotebook(payload: NotebookPayload): Promise<Notebook | null> {
    try {
      const notebook = await createNotebookApi(payload)
      notebooks.value.push(notebook)
      await selectNotebook(notebook.id)
      return notebook
    } catch (cause) {
      fail('创建笔记本', cause)
      return null
    }
  }

  async function updateNotebook(id: Id, payload: NotebookPayload): Promise<boolean> {
    try {
      const updated = await updateNotebookApi(id, payload)
      const index = notebooks.value.findIndex((item) => item.id === id)
      if (index >= 0) notebooks.value[index] = updated
      return true
    } catch (cause) {
      fail('更新笔记本', cause)
      return false
    }
  }

  async function deleteNotebook(id: Id, force = false): Promise<boolean> {
    try {
      await deleteNotebookApi(id, force)
      errorCode.value = null
      notebooks.value = notebooks.value.filter((item) => item.id !== id)
      if (currentNotebookId.value === id) {
        currentNotebookId.value = notebooks.value[0]?.id ?? null
        currentNote.value = null
        folders.value = []
        notes.value = []
        await loadNotebookData()
      }
      return true
    } catch (cause) {
      fail('删除笔记本', cause)
      return false
    }
  }

  async function createFolder(name: string): Promise<Folder | null> {
    if (currentNotebookId.value === null) return null
    try {
      const folder = await createFolderApi({ notebookId: currentNotebookId.value, name })
      folders.value.push(folder)
      return folder
    } catch (cause) {
      fail('创建文件夹', cause)
      return null
    }
  }

  async function renameFolder(id: Id, payload: FolderPayload): Promise<boolean> {
    try {
      const updated = await updateFolderApi(id, payload)
      const index = folders.value.findIndex((item) => item.id === id)
      if (index >= 0) folders.value[index] = updated
      return true
    } catch (cause) {
      fail('重命名文件夹', cause)
      return false
    }
  }

  async function deleteFolder(id: Id): Promise<boolean> {
    try {
      await deleteFolderApi(id)
      folders.value = folders.value.filter((item) => item.id !== id)
      await loadNotebookData()
      return true
    } catch (cause) {
      fail('删除文件夹', cause)
      return false
    }
  }

  async function fetchNotes(keyword?: string): Promise<void> {
    if (currentNotebookId.value === null) return
    loading.value = true
    try {
      notes.value = await fetchNotesApi(currentNotebookId.value, null, keyword)
    } catch (cause) {
      fail('搜索笔记', cause)
    } finally {
      loading.value = false
    }
  }

  async function openNote(id: Id): Promise<Note | null> {
    try {
      currentNote.value = await fetchNoteApi(id)
      return currentNote.value
    } catch (cause) {
      fail('打开笔记', cause)
      return null
    }
  }

  async function createNote(folderId: Id | null = null): Promise<Note | null> {
    if (currentNotebookId.value === null) return null
    try {
      const note = await createNoteApi({
        notebookId: currentNotebookId.value,
        folderId,
        title: '无标题笔记',
        contentMd: '',
      })
      await loadNotebookData()
      currentNote.value = note
      return note
    } catch (cause) {
      fail('创建笔记', cause)
      return null
    }
  }

  async function saveNote(id: Id, data: UpdateNoteRequest): Promise<boolean> {
    saving.value = true
    try {
      const updated = await updateNoteApi(id, data)
      currentNote.value = updated
      const index = notes.value.findIndex((item) => item.id === id)
      if (index >= 0) {
        notes.value[index] = {
          ...notes.value[index],
          title: updated.title,
          wordCount: updated.wordCount,
          pinned: updated.pinned,
          updatedAt: updated.updatedAt,
          excerpt: updated.contentMd.slice(0, 200),
        }
      }
      return true
    } catch (cause) {
      fail('保存笔记', cause)
      return false
    } finally {
      saving.value = false
    }
  }

  async function deleteNote(id: Id): Promise<boolean> {
    try {
      await deleteNoteApi(id)
      notes.value = notes.value.filter((item) => item.id !== id)
      if (currentNote.value?.id === id) {
        currentNote.value = null
      }
      return true
    } catch (cause) {
      fail('删除笔记', cause)
      return false
    }
  }

  async function moveNote(id: Id, payload: MoveNoteRequest): Promise<boolean> {
    try {
      await moveNoteApi(id, payload)
      await loadNotebookData()
      return true
    } catch (cause) {
      fail('移动笔记', cause)
      return false
    }
  }

  function reset(): void {
    notebooks.value = []
    currentNotebookId.value = null
    folders.value = []
    notes.value = []
    currentNote.value = null
    error.value = null
  }

  return {
    notebooks,
    currentNotebookId,
    folders,
    notes,
    currentNote,
    loading,
    saving,
    error,
    errorCode,
    currentNotebook,
    reset,
    bootstrap,
    selectNotebook,
    createNotebook,
    updateNotebook,
    deleteNotebook,
    createFolder,
    renameFolder,
    deleteFolder,
    fetchNotes,
    openNote,
    createNote,
    saveNote,
    deleteNote,
    moveNote,
  }
})

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getNotebooks as fetchNotebooksApi,
  getNotes as fetchNotesApi,
  getNote as fetchNoteApi,
  createNote as createNoteApi,
  updateNote as updateNoteApi,
  deleteNote as deleteNoteApi,
} from '@/api/note'
import {
  getFolders as fetchFoldersApi,
  createFolder as createFolderApi,
  deleteFolder as deleteFolderApi,
} from '@/api/folder'

export const useNoteStore = defineStore('note', () => {
  /* ========== State ========== */
  const notebooks = ref([])
  const currentNotebookId = ref(null)
  const folders = ref([])
  const currentFolderId = ref(null)
  const notes = ref([])
  const currentNote = ref(null)
  const loading = ref(false)

  /* ========== Getters ========== */
  const currentNotebook = computed(() =>
    notebooks.value.find((n) => n.id === currentNotebookId.value) || null
  )

  const currentFolder = computed(() =>
    folders.value.find((f) => f.id === currentFolderId.value) || null
  )

  /* ========== Actions ========== */

  /** 获取所有笔记本 */
  async function fetchNotebooks() {
    loading.value = true
    try {
      notebooks.value = await fetchNotebooksApi()
      if (notebooks.value.length > 0 && !currentNotebookId.value) {
        currentNotebookId.value = notebooks.value[0].id
      }
    } catch (e) {
      console.error('获取笔记本列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 获取当前笔记本下的文件夹列表 */
  async function fetchFolders(notebookId) {
    if (!notebookId) return
    loading.value = true
    try {
      folders.value = await fetchFoldersApi(notebookId)
    } catch (e) {
      console.error('获取文件夹列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 选择笔记本 */
  function selectNotebook(notebookId) {
    currentNotebookId.value = notebookId
    currentFolderId.value = null
    notes.value = []
    currentNote.value = null
    fetchFolders(notebookId)
  }

  /** 选择文件夹 */
  function selectFolder(folderId) {
    currentFolderId.value = folderId
    currentNote.value = null
    notes.value = []
    fetchNotes(folderId)
  }

  /** 获取笔记列表 */
  async function fetchNotes(folderId) {
    if (!folderId) return
    loading.value = true
    try {
      notes.value = await fetchNotesApi(folderId)
    } catch (e) {
      console.error('获取笔记列表失败')
    } finally {
      loading.value = false
    }
  }

  /** 打开笔记 */
  async function openNote(noteId) {
    loading.value = true
    try {
      currentNote.value = await fetchNoteApi(noteId)
    } catch (e) {
      console.error('获取笔记详情失败')
    } finally {
      loading.value = false
    }
  }

  /** 创建笔记 */
  async function createNote(data) {
    try {
      const note = await createNoteApi(data)
      notes.value.unshift(note)
      return note
    } catch (e) {
      console.error('创建笔记失败')
      return null
    }
  }

  /** 保存笔记 */
  async function saveNote(id, data) {
    try {
      const updated = await updateNoteApi(id, data)
      currentNote.value = updated
      const idx = notes.value.findIndex((n) => n.id === id)
      if (idx !== -1) {
        notes.value[idx] = { ...notes.value[idx], ...data }
      }
      return true
    } catch (e) {
      console.error('保存笔记失败')
      return false
    }
  }

  /** 创建文件夹 */
  async function createFolder(data) {
    try {
      const folder = await createFolderApi(data)
      folders.value.push(folder)
      return folder
    } catch (e) {
      console.error('创建文件夹失败')
      return null
    }
  }

  /** 删除文件夹 */
  async function deleteFolder(folderId) {
    try {
      await deleteFolderApi(folderId)
      folders.value = folders.value.filter((f) => f.id !== folderId)
      if (currentFolderId.value === folderId) {
        currentFolderId.value = null
        notes.value = []
      }
    } catch (e) {
      console.error('删除文件夹失败')
    }
  }

  /** 删除笔记 */
  async function deleteNote(noteId) {
    try {
      await deleteNoteApi(noteId)
      notes.value = notes.value.filter((n) => n.id !== noteId)
      if (currentNote.value?.id === noteId) {
        currentNote.value = null
      }
    } catch (e) {
      console.error('删除笔记失败')
    }
  }

  /** 重命名笔记本（本地更新） */
  function renameNotebook(notebookId, name) {
    const nb = notebooks.value.find((n) => n.id === notebookId)
    if (nb) nb.name = name
  }

  return {
    // state
    notebooks,
    currentNotebookId,
    folders,
    currentFolderId,
    notes,
    currentNote,
    loading,
    // getters
    currentNotebook,
    currentFolder,
    // actions
    fetchNotebooks,
    fetchFolders,
    selectNotebook,
    selectFolder,
    fetchNotes,
    openNote,
    createNote,
    saveNote,
    createFolder,
    deleteFolder,
    deleteNote,
    renameNotebook,
  }
})

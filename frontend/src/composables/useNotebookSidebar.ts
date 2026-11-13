import { ref } from 'vue'

/**
 * 笔记侧栏的折叠状态。
 *
 * <p>折叠后侧栏整块隐藏，展开入口在全局导航的头像上，因此状态需要跨组件共享 ——
 * 用模块级 ref，与 useAuthDialog 的用法一致。
 */
const collapsed = ref(false)

export function useNotebookSidebar() {
  return {
    collapsed,
    collapse: () => { collapsed.value = true },
    expand: () => { collapsed.value = false },
  }
}

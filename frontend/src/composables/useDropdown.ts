import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * 下拉菜单/弹出面板的开关与「点击外部关闭」。
 *
 * <p><b>为什么抽这个 composable</b>：`Notebook.vue` 里曾经有 **10 处以上**
 * 各写一份的 `showXxxMenu` + `xxxMenuRef` + 自己的 `document.addEventListener`。
 * 这是 Vue 3 组合式 API 最典型的适用场景 —— 一段**有状态的逻辑**，与组件无关，可以复用。
 *
 * <p><b>为什么用全局注册表而不是每个实例各加一个监听器</b>：
 * 如果每个下拉各自 `document.addEventListener`，页面上有 10 个下拉就有 10 个文档级监听器，
 * 每次点击都要跑 10 次 `contains()` 判断。
 * 这里改成**模块级单例监听器**：无论页面上有多少个下拉，文档上始终只有一个监听器，
 * 它遍历注册表逐个判断，同时把**所有**失去焦点的下拉一起关掉
 * （后者其实更符合直觉 —— 点开另一个菜单时，上一个应该自动关闭）。
 */
interface DropdownEntry {
  /** 该下拉的根元素；点击发生在其内部时不关闭。用 getter 是因为 ref 在挂载后才指向真实元素 */
  readonly root: HTMLElement | null
  close: () => void
}

/** 当前处于打开状态的下拉注册表 */
const registry = new Set<DropdownEntry>()

/** 文档级监听器是否已挂载（整个应用只需挂一次） */
let listenerAttached = false

function handlePointerDown(event: PointerEvent): void {
  const target = event.target as Node | null
  if (!target) return

  // 遍历副本：entry.close() 会把自己从 registry 中移除
  for (const entry of [...registry]) {
    // root 理论上不会为 null（只有拿到元素才会注册），但守卫一下更安全
    if (entry.root && !entry.root.contains(target)) {
      entry.close()
    }
  }
}

/**
 * 创建一个下拉的开关状态。
 *
 * @param rootRef 下拉根元素的 ref（必须是包含「触发按钮 + 面板」的最外层元素）
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const rootRef = ref<HTMLElement | null>(null)
 * const { isOpen, toggle, close } = useDropdown(rootRef)
 * </script>
 *
 * <template>
 *   <div ref="rootRef">
 *     <button @click="toggle">菜单</button>
 *     <div v-if="isOpen">...</div>
 *   </div>
 * </template>
 * ```
 */
export function useDropdown(rootRef: Ref<HTMLElement | null>) {
  const isOpen = ref(false)

  const entry: DropdownEntry = {
    get root() {
      return rootRef.value
    },
    // 注意这里调用的是外层的 close()（函数声明会提升），
    // 而不是直接改 isOpen —— 否则「点击外部关闭」时只关了面板却没注销注册表，
    // 条目会一直留到组件卸载
    close: () => close(),
  }

  function open(): void {
    if (!rootRef.value) return
    if (!listenerAttached) {
      // 用捕获阶段：保证先于触发按钮自身的 click 处理器执行，
      // 这样「点 A 打开时自动关闭 B」与「点自身切换开关」两种行为都正确
      document.addEventListener('pointerdown', handlePointerDown, true)
      listenerAttached = true
    }
    registry.add(entry)
    isOpen.value = true
  }

  function close(): void {
    registry.delete(entry)
    isOpen.value = false
  }

  function toggle(): void {
    if (isOpen.value) close()
    else open()
  }

  onMounted(() => {
    // 若初始就是打开的（少见），补一次注册
    if (isOpen.value) open()
  })

  onBeforeUnmount(() => {
    // 卸载时注销，避免注册表里留下悬空条目
    registry.delete(entry)
    // 若这是最后一个使用者的场景（本项目是单页应用，监听器常驻也无妨），
    // 这里不做 removeEventListener —— 全局只挂一次，重复增删反而容易出错
  })

  return { isOpen, open, close, toggle }
}

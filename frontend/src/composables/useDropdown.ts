import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * 下拉菜单/弹出面板的开关与「点击外部关闭」。
 *
 * <p>抽出来是因为 Notebook.vue 里曾有十处以上各写一份的
 * showXxxMenu + xxxMenuRef + 自己的 document.addEventListener。
 *
 * <p>用模块级单例监听器，而不是每个下拉各加一个：否则页面上有 10 个下拉就有 10 个
 * 文档级监听器、每次点击跑 10 次 contains()。单例监听器遍历注册表逐个判断，
 * 顺便把所有失去焦点的下拉一起关掉（点开另一个菜单时上一个应当自动关闭）。
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
    // 这里必须调用外层的 close()（函数声明会提升）。
    // 直接改 isOpen 会让「点击外部关闭」只关面板不注销注册表，条目留到组件卸载
    close: () => close(),
  }

  function open(): void {
    if (!rootRef.value) return
    if (!listenerAttached) {
      // 捕获阶段：保证先于触发按钮自身的 click 处理器执行，
      // 「点 A 打开时自动关闭 B」与「点自身切换开关」才都正确
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
    // 卸载时注销，避免注册表里留下悬空条目。
    // 不做 removeEventListener：全局只挂一次，重复增删反而容易出错
    registry.delete(entry)
  })

  return { isOpen, open, close, toggle }
}

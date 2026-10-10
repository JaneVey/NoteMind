import type { Router, RouteLocationNormalized } from 'vue-router'

/**
 * 路由插件：只负责设置页面标题。
 *
 * <h3>⚠️ 这里曾经有一层「登录拦截」守卫，已整体移除</h3>
 *
 * 旧实现是：`beforeEach` 里检查 token，未登录一律跳到登录页。
 * 那是**后台管理系统**的思路 —— 反正登录了才有活干。
 * 但 NoteMind 是 C 端产品，市面产品（ChatGPT、Claude、Notion）都是
 * **先让用户用起来，到需要身份的时候才索取**。
 *
 * <p>「需要登录」的判断已经从**进页面**下沉到**做动作**，
 * 入口是 {@code composables/useAuthGuard.ts} 的 `requireAuth()`。
 * 这样做的三个好处：
 * <ol>
 *   <li>「哪里需要登录」在代码里是显式的，一眼可见，不依赖路由表这种间接映射</li>
 *   <li>可以做到「登录后自动续做」—— 弹框登录完，原本被打断的动作继续执行</li>
 *   <li>弹框而非整页跳转，不破坏用户所处的上下文</li>
 * </ol>
 *
 * <p>如果再保留一层全拦守卫，就会与动作层的判断**自相矛盾**：
 * 守卫会把用户从页面赶走，而动作层的本意是让他继续待着。
 */
export default function setupRouter(router: Router): void {
  router.afterEach((to: RouteLocationNormalized) => {
    document.title = to.meta?.title ? `${to.meta.title} - NoteMind` : 'NoteMind'
  })
}

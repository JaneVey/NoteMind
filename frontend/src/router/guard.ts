import type { Router, RouteLocationNormalized } from 'vue-router'

/**
 * 路由插件：只负责设置页面标题。
 *
 * <p>不要再加「登录拦截」守卫。NoteMind 是 C 端产品，需要登录的判断放在动作层
 * （`composables/useAuthGuard.ts` 的 `requireAuth()`）—— 那里「哪里需要登录」是显式的，
 * 也能做到「登录后自动续做」，还不破坏当前上下文。
 *
 * <p>再加一层全拦守卫会和动作层自相矛盾：守卫把用户赶出页面，而动作层的本意是让他继续待着。
 */
export default function setupRouter(router: Router): void {
  router.afterEach((to: RouteLocationNormalized) => {
    document.title = to.meta?.title ? `${to.meta.title} - NoteMind` : 'NoteMind'
  })
}

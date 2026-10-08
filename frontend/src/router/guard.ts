import type { NavigationGuardNext, Router, RouteLocationNormalized } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'

const whiteList = ['/login', '/register']

/**
 * 路由守卫（登录拦截）。
 *
 * <p><b>变更记录（2026-10-08）—— 移除开发环境放行</b>
 *
 * <p>此前实现的第二行是：
 * <pre>
 *   if (import.meta.env.DEV) {
 *     next()        // 开发环境直接放行
 *     return
 *   }
 * </pre>
 *
 * <p>这带来一个很隐蔽的问题：<b>页面能进，但接口返回 401</b>。
 * 因为后端 Spring Security 对业务接口一律要求 JWT，
 * 而前端跳过了登录引导，localStorage 里根本没有 Token，
 * 于是用户会看到「未登录或登录已过期」，却完全不知道该先去登录 ——
 * 看起来就像"登录功能没做"。
 *
 * <p>同时它也让开发阶段无法验证真实的登录流程。
 * 既然用户系统后端已经完成，就没有理由在开发环境绕过它。
 *
 * <p>另外补充了 {@code redirect} 查询参数：被拦截时记住原目标，
 * 登录成功后回到该页面，而不是一律跳首页。
 */
export default function setupGuard(router: Router) {
  router.beforeEach(
    (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
      // 直接读 localStorage，避免依赖 store 是否已初始化
      const hasToken = Boolean(localStorage.getItem(TOKEN_KEY))
      const isWhitelist = whiteList.includes(to.path)

      if (hasToken && isWhitelist) {
        // 已登录还去登录页 → 回首页
        next('/')
        return
      }
      if (hasToken || isWhitelist) {
        next()
        return
      }
      next({ path: '/login', query: { redirect: to.fullPath } })
    },
  )

  router.afterEach((to: RouteLocationNormalized) => {
    document.title = to.meta?.title ? `${to.meta.title} - NoteMind` : 'NoteMind'
  })
}

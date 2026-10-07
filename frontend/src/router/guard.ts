import type { NavigationGuardNext, Router, RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const whiteList = ['/login', '/register']

export default function setupGuard(router: Router) {
  router.beforeEach((to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
    if (import.meta.env.DEV) {
      next()
      return
    }

    const authStore = useAuthStore()
    const hasToken = authStore.getToken()
    const isWhitelist = whiteList.includes(to.path)

    if (hasToken) {
      if (isWhitelist) {
        next('/')
      } else {
        next()
      }
    } else {
      if (isWhitelist) {
        next()
      } else {
        next('/login')
      }
    }
  })

  router.afterEach((to: RouteLocationNormalized) => {
    document.title = to.meta?.title
      ? `${to.meta.title} - NoteMind`
      : 'NoteMind'
  })
}

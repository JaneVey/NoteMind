import { useAuthStore } from '@/stores/authStore'

const whiteList = ['/login', '/register']

export default function setupGuard(router) {
  router.beforeEach((to, _from, next) => {
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

  router.afterEach((to) => {
    document.title = to.meta?.title
      ? `${to.meta.title} - NoteMind`
      : 'NoteMind'
  })
}

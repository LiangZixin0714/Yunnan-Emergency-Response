import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleValue } from '@/types/enums'

export function setupRouterGuard(router: Router): void {
  router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }

    if (to.path === '/login' && authStore.isLoggedIn) {
      next({ path: '/home' })
      return
    }

    const roles = to.meta.roles as UserRoleValue[] | undefined
    if (roles && roles.length > 0 && authStore.isLoggedIn) {
      if (!roles.includes(authStore.roleName as UserRoleValue)) {
        next({ path: '/403' })
        return
      }
    }

    next()
  })
}
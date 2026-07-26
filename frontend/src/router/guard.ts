// import type { Router } from 'vue-router'
// import { useAuthStore } from '@/stores/auth'
// import type { UserRoleValue } from '@/types/enums'

// export function setupRouterGuard(router: Router): void {
//   router.beforeEach((to, _from, next) => {
//     const authStore = useAuthStore()

//     if (to.meta.requiresAuth && !authStore.isLoggedIn) {
//       next({ path: '/login', query: { redirect: to.fullPath } })
//       return
//     }

//     if (to.path === '/login' && authStore.isLoggedIn) {
//       next({ path: '/home' })
//       return
//     }

//     const roles = to.meta.roles as UserRoleValue[] | undefined
//     if (roles && roles.length > 0 && authStore.isLoggedIn) {
//       if (!roles.includes(authStore.roleName as UserRoleValue)) {
//         next({ path: '/403' })
//         return
//       }
//     }

//     next()
//   })
// }
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { UserRoleValue } from '@/types/enums'

export function setupRouterGuard(router: Router): void {
  // 1. 去掉参数里的 next
  router.beforeEach((to, _from) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
      // 2. 用 return 代替 next()
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    if (to.path === '/login' && authStore.isLoggedIn) {
      return { path: '/home' }
    }

    const roles = to.meta.roles as UserRoleValue[] | undefined
    if (roles && roles.length > 0 && authStore.isLoggedIn) {
      if (!roles.includes(authStore.roleName as UserRoleValue)) {
        return { path: '/403' }
      }
    }

    // 3. 放行时直接什么都不返回（或 return true）
    return true
  })
}
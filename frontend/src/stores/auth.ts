import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types/user'
import type { UserRoleValue } from '@/types/enums'
import { login as loginApi, register as registerApi } from '@/api/login'
import { setStoredToken, clearStoredToken } from '@/utils/token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const realName = ref<string>('')
  const roleName = ref<UserRoleValue | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed<UserRoleValue | null>(() => roleName.value)

  function setToken(newToken: string): void {
    token.value = newToken
    setStoredToken(newToken)
  }

  function clearAuth(): void {
    token.value = null
    realName.value = ''
    roleName.value = null
    clearStoredToken()
  }

  async function login(params: LoginParams): Promise<void> {
    const res = (await loginApi(params)) as unknown as LoginResult
    setToken(res.token)
    realName.value = res.realName
    roleName.value = res.roleName as UserRoleValue
  }

  async function register(params: RegisterParams): Promise<UserInfo> {
    const res = (await registerApi(params)) as unknown as UserInfo
    return res
  }

  function logout(): void {
    clearAuth()
  }

  return {
    token,
    realName,
    roleName,
    isLoggedIn,
    userRole,
    setToken,
    clearAuth,
    login,
    register,
    logout,
  }
})

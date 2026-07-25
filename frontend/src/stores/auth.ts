import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginParams, LoginResult, RegisterParams, UserInfo } from '@/types/user'
import type { UserRoleValue } from '@/types/enums'
import { login as loginApi, register as registerApi } from '@/api/login'
import { getStoredToken, setStoredToken, clearStoredToken } from '@/utils/token'

const USER_INFO_KEY = 'auth_user_info'

function getStoredUserInfo(): { userId: number; username: string; realName: string; roleName: UserRoleValue } | null {
  const raw = sessionStorage.getItem(USER_INFO_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) } catch { return null }
}

function setStoredUserInfo(info: { userId: number; username: string; realName: string; roleName: UserRoleValue }): void {
  sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

function clearStoredUserInfo(): void {
  sessionStorage.removeItem(USER_INFO_KEY)
}

const storedToken = getStoredToken()
const storedUserInfo = getStoredUserInfo()

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(storedToken)
  const userId = ref<number | null>(storedUserInfo?.userId ?? null)
  const username = ref<string>(storedUserInfo?.username ?? '')
  const realName = ref<string>(storedUserInfo?.realName ?? '')
  const roleName = ref<UserRoleValue | null>(storedUserInfo?.roleName ?? null)

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed<UserRoleValue | null>(() => roleName.value)

  function setToken(newToken: string): void {
    token.value = newToken
    setStoredToken(newToken)
  }

  function clearAuth(): void {
    token.value = null
    userId.value = null
    username.value = ''
    realName.value = ''
    roleName.value = null
    clearStoredToken()
    clearStoredUserInfo()
  }

  async function login(params: LoginParams): Promise<void> {
    const res = (await loginApi(params)) as unknown as LoginResult
    setToken(res.token)
    userId.value = res.userId
    username.value = res.username
    realName.value = res.realName
    roleName.value = res.roleName as UserRoleValue
    setStoredUserInfo({ userId: res.userId, username: res.username, realName: res.realName, roleName: res.roleName as UserRoleValue })
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
    userId,
    username,
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

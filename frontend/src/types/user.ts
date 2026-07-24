import type { UserRoleValue } from './enums'

export interface UserInfo {
  id: number
  username: string
  email: string | null
  phone: string | null
  realName: string | null
  roleId: number
  status: number
  createdAt: string
  updatedAt: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  username: string
  realName: string
  roleName: UserRoleValue
}

export interface RegisterParams {
  username: string
  password: string
  realName?: string
}

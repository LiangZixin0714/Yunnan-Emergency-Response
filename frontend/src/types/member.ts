import type { UserRoleValue } from './enums'

export interface MemberInfo {
  userId: number
  username: string
  realName: string | null
  email: string | null
  phone: string | null
  roleName: UserRoleValue
  status: number
  registeredAt: string
  lastLoginAt: string | null
}

export interface ChangeRoleParams {
  userId: number
  targetRole: UserRoleValue
}
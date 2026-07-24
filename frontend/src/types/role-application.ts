import type { UserRoleValue } from './enums'

export interface RoleApplication {
  id: number
  applicationId: string
  userId: number
  username: string
  realName: string | null
  targetRole: Exclude<UserRoleValue, 'VIEWER'>
  reason: string
  status: string
  reviewerId: number | null
  reviewerName: string | null
  rejectReason: string | null
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface RoleApplicationSubmitData {
  targetRole: Exclude<UserRoleValue, 'VIEWER'>
  reason: string
}
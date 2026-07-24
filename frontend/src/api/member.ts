import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { MemberInfo, ChangeRoleParams } from '@/types/member'

export function getMemberList(params?: { keyword?: string; roleName?: string }): Promise<ApiResponse<MemberInfo[]>> {
  return request.get('/member/list', { params })
}

export function changeMemberRole(data: ChangeRoleParams): Promise<ApiResponse<{ userId: number; roleName: string }>> {
  return request.post('/member/change-role', data)
}
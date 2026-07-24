import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { RoleApplication, RoleApplicationSubmitData } from '@/types/role-application'

export function submitRoleApplication(data: RoleApplicationSubmitData): Promise<ApiResponse<RoleApplication>> {
  return request.post('/role-application/submit', data)
}

export function getRoleApplicationList(params?: { userId?: number; status?: string }): Promise<ApiResponse<RoleApplication[]>> {
  return request.get('/role-application/list', { params })
}

export function reviewRoleApplication(data: { id: number; approved: boolean; reason?: string; reviewerId?: number }): Promise<ApiResponse<RoleApplication>> {
  return request.post('/role-application/review', data)
}
import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Resource, ResourceListParams, DispatchOrder } from '@/types/resource'

export function getResourceList(params?: ResourceListParams): Promise<ApiResponse<Resource[]>> {
  return request.get('/resource/list', { params })
}

export function createResource(data: {
  resourceName: string
  resourceType?: string
  unit?: string
  totalStock?: number
  availableStock?: number
  location?: string
  description?: string
}): Promise<ApiResponse<Resource>> {
  return request.post('/resource', data)
}

export function updateResource(id: number, data: {
  resourceName?: string
  resourceType?: string
  unit?: string
  totalStock?: number
  availableStock?: number
  location?: string
  description?: string
  status?: string
}): Promise<ApiResponse<Resource>> {
  return request.put(`/resource/${id}`, data)
}

export function deleteResource(id: number): Promise<ApiResponse<{ deleted: boolean }>> {
  return request.delete(`/resource/${id}`)
}

export function getDispatchOrders(): Promise<ApiResponse<DispatchOrder[]>> {
  return request.get('/resource/dispatch-records')
}

export function lockResource(data: {
  resourceId: string
  quantity: number
  incidentId?: string
  planId?: string
  remark?: string
}): Promise<ApiResponse<any>> {
  return request.post('/resource/lock', data)
}

export function allocateResource(data: {
  resourceId: string
  quantity: number
  incidentId?: string
  planId?: string
  remark?: string
}): Promise<ApiResponse<any>> {
  return request.post('/resource/allocate', data)
}

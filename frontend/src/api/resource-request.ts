import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { ResourceRequest, ResourceRequestItem } from '@/types/resource'

export function submitResourceRequest(data: {
  incidentId: string
  items: ResourceRequestItem[]
}): Promise<ApiResponse<ResourceRequest[]>> {
  return request.post('/resource-request/submit', data)
}

export function getPendingResourceRequests(): Promise<ApiResponse<ResourceRequest[]>> {
  return request.get('/resource-request/pending')
}

export function getResourceRequestsByIncident(incidentId: string): Promise<ApiResponse<ResourceRequest[]>> {
  return request.get('/resource-request/list', { params: { incidentId } })
}

export function approveResourceRequest(id: number): Promise<ApiResponse<ResourceRequest>> {
  return request.post('/resource-request/approve', { id })
}

export function rejectResourceRequest(id: number, reason?: string): Promise<ApiResponse<ResourceRequest>> {
  return request.post('/resource-request/reject', { id, reason })
}

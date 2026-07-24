import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { ResourceRequest, ResourceRequestItem } from '@/types/resource'

export function submitResourceRequest(data: {
  incidentId: string
  items: ResourceRequestItem[]
}): Promise<ApiResponse<ResourceRequest[]>> {
  return request.post('/resource-request/submit', data)
}

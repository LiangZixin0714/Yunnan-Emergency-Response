import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Resource, ResourceListParams, DispatchOrder } from '@/types/resource'

export function getResourceList(params?: ResourceListParams): Promise<ApiResponse<Resource[]>> {
  return request.get('/resource/list', { params })
}

export function getDispatchOrders(): Promise<ApiResponse<DispatchOrder[]>> {
  return request.get('/dispatch/list')
}

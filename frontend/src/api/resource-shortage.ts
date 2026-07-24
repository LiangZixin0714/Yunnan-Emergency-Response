import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { ResourceShortageWarning } from '@/types/resource'

export function getShortageWarningList(): Promise<ApiResponse<ResourceShortageWarning[]>> {
  return request.get('/resource-shortage/list')
}

export function handleShortageWarning(data: { id: number; action: 'replenish' | 'reject'; handledBy?: number }): Promise<ApiResponse<ResourceShortageWarning>> {
  return request.post('/resource-shortage/handle', data)
}
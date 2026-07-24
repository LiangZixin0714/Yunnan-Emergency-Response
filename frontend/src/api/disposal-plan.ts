import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { DisposalPlan } from '@/types/disposal-plan'

export function getDisposalPlanList(params?: { incidentId?: string }): Promise<ApiResponse<DisposalPlan[]>> {
  return request.get('/disposal-plan/list', { params })
}

export function submitDisposalPlan(data: { id: number; planContent?: string; incidentId?: string; submittedBy?: number }): Promise<ApiResponse<DisposalPlan>> {
  return request.post('/disposal-plan/submit', data)
}

export function saveDisposalPlan(data: { id: number; planContent: string; incidentId?: string }): Promise<ApiResponse<DisposalPlan>> {
  return request.post('/disposal-plan/save-draft', data)
}

export function rejectDisposalPlan(data: { id: number; rejectReason: string; incidentId?: string }): Promise<ApiResponse<DisposalPlan>> {
  return request.post('/disposal-plan/reject', data)
}
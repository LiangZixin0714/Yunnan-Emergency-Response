import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Incident, IncidentReportData, IncidentReportResult, IncidentListParams, IncidentListResult } from '@/types/incident'
import { buildIncidentFormData } from '@/utils/upload'

export function getIncidentList(params: IncidentListParams): Promise<ApiResponse<IncidentListResult>> {
  return request.get('/incident/list', { params })
}

export function getIncidentDetail(incidentId: string): Promise<ApiResponse<Incident>> {
  return request.get('/incident/detail', { params: { incidentId } })
}

export function reportIncident(data: IncidentReportData): Promise<ApiResponse<IncidentReportResult>> {
  const formData = buildIncidentFormData(data)
  return request.post('/incident/report', formData)
}

export function completeIncident(incidentId: string): Promise<ApiResponse<Incident>> {
  return request.post('/incident/complete', null, { params: { incidentId } })
}

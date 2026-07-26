import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { AgentRun, Citation, AuditLog, PageResult } from '@/types/agent'

export function getAgentRunList(params: {
  page: number
  size: number
  incidentId?: string
  agentName?: string
  status?: string
}): Promise<PageResult<AgentRun>> {
  return request.get('/agent/runs', { params })
}

export function getAgentRunDetail(id: number): Promise<AgentRun> {
  return request.get(`/agent/runs/${id}`)
}

export function getAgentRunCitations(id: number): Promise<Citation[]> {
  return request.get(`/agent/runs/${id}/citations`)
}

export function getAuditLogList(params: {
  page: number
  size: number
  userId?: number
  module?: string
  action?: string
}): Promise<PageResult<AuditLog>> {
  return request.get('/audit/logs', { params })
}

export function getAuditLogDetail(id: number): Promise<AuditLog> {
  return request.get(`/audit/logs/${id}`)
}
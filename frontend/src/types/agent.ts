export interface AgentRun {
  id: number
  runId: string
  incidentId: string
  agentName: string
  inputParams: string
  outputResult: string
  status: string
  errorMessage: string
  startTime: string
  endTime: string
  createdAt: string
}

export interface Citation {
  id: number
  citationId: string
  incidentId: string
  runId: string
  sourceText: string
  sourceUrl: string
  relevanceScore: number
  createdAt: string
}

export interface AuditLog {
  id: number
  userId: number
  username: string
  module: string
  action: string
  actionType: string
  targetType: string
  targetId: string
  requestParams: string
  detail: string
  result: string
  ipAddress: string
  duration: number
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}
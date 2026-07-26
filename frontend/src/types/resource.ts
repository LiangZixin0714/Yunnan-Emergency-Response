import type { ResourceTypeValue, ResourceStatusValue } from './enums'

export interface Resource {
  id: number
  resourceId: string
  resourceName: string
  resourceType: ResourceTypeValue
  totalStock: number
  availableStock: number
  lockedStock: number
  unit: string | null
  status: ResourceStatusValue
  location: string | null
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface ResourceListParams {
  resourceType?: ResourceTypeValue
}

export interface DispatchOrder {
  id: number
  orderId: string
  incidentId: string
  content: string | null
  targetUsers: string | null
  priority: string | null
  status: string
  dispatchTime: string | null
  createdAt: string
  updatedAt: string
}

export interface DataSource {
  id: number
  sourceId: string
  sourceName: string
  sourceType: string | null
  url: string | null
  status: number
  createdAt: string
  updatedAt: string
}

export interface AgentRun {
  id: number
  runId: string
  incidentId: string | null
  agentName: string | null
  inputParams: string | null
  outputResult: string | null
  status: string | null
  errorMessage: string | null
  startTime: string | null
  endTime: string | null
  createdAt: string
}

export interface Citation {
  id: number
  citationId: string
  incidentId: string | null
  sourceText: string | null
  sourceUrl: string | null
  relevanceScore: number | null
  createdAt: string
}

export interface AuditLog {
  id: number
  userId: number | null
  action: string | null
  targetType: string | null
  targetId: string | null
  detail: string | null
  ipAddress: string | null
  createdAt: string
}

export interface ResourceShortageWarning {
  id: number
  warningId: string
  incidentId: string
  orderId: string
  resourceType: ResourceTypeValue
  requiredQuantity: number
  availableQuantity: number
  shortageQuantity: number
  reportedBy: number | null
  reportedAt: string | null
  status: string
  handledBy: number | null
  handledAt: string | null
  handleResult: string | null
}

export interface ResourceRequest {
  id: number
  requestId: string
  incidentId: string
  resourceId: string
  resourceName: string
  resourceType: string | null
  quantity: number
  unit: string | null
  priority: string | null
  destination: string | null
  status: string
  remark: string | null
  requesterId: number | null
  createdAt: string | null
  updatedAt: string | null
}

export interface ResourceRequestItem {
  resourceId: string
  resourceName: string
  quantity: number
}

import type { DisasterTypeValue, IncidentLevelValue, IncidentStatusValue, DisposalPlanStatusValue, DispatchOrderStatusValue } from './enums'

export interface Incident {
  id: number
  incidentId: string
  incidentName: string
  disasterType: DisasterTypeValue
  incidentLevel: IncidentLevelValue
  occurTime: string | null
  location: string | null
  description: string | null
  status: IncidentStatusValue
  imageUrls: string | null
  reporterId: number | null
  reportTime: string | null
  deathCount: number | null
  propertyLoss: number | null
  disposalPlanStatus: DisposalPlanStatusValue | null
  resourceDispatchStatus: DispatchOrderStatusValue | null
  createdAt: string
  updatedAt: string
}

export interface IncidentReportData {
  incidentName: string
  disasterType: DisasterTypeValue
  incidentLevel?: IncidentLevelValue
  occurTime?: string
  location?: string
  description?: string
  deathCount?: number
  propertyLoss?: number
  images?: File[]
}

export interface IncidentReportResult {
  incidentId: string
  imageUrls: string[]
}

export interface IncidentListParams {
  page: number
  size: number
  disasterType?: DisasterTypeValue
  incidentLevel?: IncidentLevelValue
  status?: IncidentStatusValue
  keyword?: string
}

export interface IncidentListResult {
  total: number
  list: Incident[]
}

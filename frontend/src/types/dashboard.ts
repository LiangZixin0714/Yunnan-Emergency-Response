import type { Incident } from './incident'
import type { Resource } from './resource'
import type { DisasterTypeValue, IncidentLevelValue, IncidentStatusValue } from './enums'

export interface DashboardOverview {
  todayCount: number
  activeCount: number
  completedCount: number
}

export interface DashboardTrend {
  dates: string[]
  counts: number[]
}

export interface DashboardDistribution {
  types: string[]
  counts: number[]
}

export interface MapIncident {
  incidentId: string
  incidentName: string
  disasterType: DisasterTypeValue
  incidentLevel: IncidentLevelValue
  status: IncidentStatusValue
  latitude: number | null
  longitude: number | null
  occurTime?: string | null
  location?: string | null
  disposalPlanStatus?: string | null
  resourceDispatchStatus?: string | null
  deathCount?: number | null
  propertyLoss?: number | null
}

export interface ScreenData {
  statistics: DashboardOverview
  incidents: Incident[]
  resources: Resource[]
  mapIncidents?: MapIncident[]
}
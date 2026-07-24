import type { PlanStatusValue } from './enums'

export interface Plan {
  id: number
  planId: string
  incidentId: string
  planTitle: string
  planContent: string | null
  generateTime: string | null
  status: PlanStatusValue
  createdAt: string
  updatedAt: string
}

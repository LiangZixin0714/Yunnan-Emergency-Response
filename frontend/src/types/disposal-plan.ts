import type { DisposalPlanStatusValue } from './enums'

export interface DisposalPlan {
  id: number
  disposalPlanId: string
  incidentId: string
  planContent: string | null
  status: DisposalPlanStatusValue
  submittedBy: number | null
  submittedAt: string | null
  rejectReason: string | null
  createdAt: string
  updatedAt: string
}
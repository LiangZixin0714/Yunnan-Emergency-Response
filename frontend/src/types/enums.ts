export const DisasterType = {
  EARTHQUAKE: 'earthquake',
  MUDSLIDE: 'mudslide',
  FLOOD: 'flood',
  DROUGHT: 'drought',
  LANDSLIDE: 'landslide',
  FIRE: 'fire',
  OTHER: 'other',
} as const

export type DisasterTypeValue = (typeof DisasterType)[keyof typeof DisasterType]

export const DisasterTypeLabel: Record<DisasterTypeValue, string> = {
  earthquake: '地震',
  mudslide: '泥石流',
  flood: '洪涝',
  drought: '干旱',
  landslide: '山体滑坡',
  fire: '森林火灾',
  other: '其他',
}

export const IncidentLevel = {
  LEVEL_I: 'I',
  LEVEL_II: 'II',
  LEVEL_III: 'III',
  LEVEL_IV: 'IV',
} as const

export type IncidentLevelValue = (typeof IncidentLevel)[keyof typeof IncidentLevel]

export const IncidentLevelLabel: Record<IncidentLevelValue, string> = {
  I: 'Ⅰ级（特别重大）',
  II: 'Ⅱ级（重大）',
  III: 'Ⅲ级（较大）',
  IV: 'Ⅳ级（一般）',
}

export const IncidentStatus = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  PROCESSING: 'processing',
  COMPLETED: 'completed',
} as const

export type IncidentStatusValue = (typeof IncidentStatus)[keyof typeof IncidentStatus]

export const IncidentStatusLabel: Record<IncidentStatusValue, string> = {
  pending: '待确认',
  confirmed: '已确认',
  processing: '处置中',
  completed: '已结束',
}

export const IncidentStatusTagType: Record<IncidentStatusValue, string> = {
  pending: 'info',
  confirmed: '',
  processing: 'danger',
  completed: 'success',
}

export const PlanStatus = {
  DRAFT: 'draft',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const

export type PlanStatusValue = (typeof PlanStatus)[keyof typeof PlanStatus]

export const PlanStatusLabel: Record<PlanStatusValue, string> = {
  draft: '草稿',
  approved: '已审批',
  rejected: '已驳回',
}

export const PlanStatusTagType: Record<PlanStatusValue, string> = {
  draft: 'info',
  approved: 'success',
  rejected: 'danger',
}

export const UserRole = {
  ADMIN: 'ADMIN',
  OPERATOR: 'OPERATOR',
  RESOURCE_MANAGER: 'RESOURCE_MANAGER',
  VIEWER: 'VIEWER',
} as const

export type UserRoleValue = (typeof UserRole)[keyof typeof UserRole]

export const UserRoleLabel: Record<UserRoleValue, string> = {
  ADMIN: '系统管理员',
  OPERATOR: '应急指挥人员',
  RESOURCE_MANAGER: '资源管理员',
  VIEWER: '普通信息员',
}

export const ResourceType = {
  TEAM: 'team',
  MEDICAL: 'medical',
  VEHICLE: 'vehicle',
  SHELTER: 'shelter',
  OTHER: 'other',
} as const

export type ResourceTypeValue = (typeof ResourceType)[keyof typeof ResourceType]

export const ResourceTypeLabel: Record<ResourceTypeValue, string> = {
  team: '救援队伍',
  medical: '医疗物资',
  vehicle: '运输车辆',
  shelter: '避难场所',
  other: '其他',
}

export const ResourceStatus = {
  AVAILABLE: 'available',
  DISPATCHED: 'dispatched',
  USED: 'used',
} as const

export type ResourceStatusValue = (typeof ResourceStatus)[keyof typeof ResourceStatus]

export const ResourceStatusLabel: Record<ResourceStatusValue, string> = {
  available: '可用',
  dispatched: '调度中',
  used: '已使用',
}

export const ALLOWED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
] as const

export const MAX_IMAGE_SIZE: number = 10 * 1024 * 1024

export const MAX_IMAGE_COUNT: number = 5

export const IMAGE_ACCEPT_STRING: string = '.jpg,.jpeg,.png,.gif,.webp'

export const ResourceStatusTagType: Record<ResourceStatusValue, string> = {
  available: 'success',
  dispatched: 'info',
  used: 'warning',
}

export const DispatchOrderStatus = {
  PENDING: 'pending',
  DISPATCHING: 'dispatching',
  EXECUTING: 'executing',
  COMPLETED: 'completed',
  SHORTAGE: 'shortage',
  IDLE: 'idle',
} as const

export type DispatchOrderStatusValue = (typeof DispatchOrderStatus)[keyof typeof DispatchOrderStatus]

export const DispatchOrderStatusLabel: Record<string, string> = {
  pending: '待执行',
  dispatching: '调度中',
  executing: '执行中',
  completed: '已完成',
  shortage: '资源不足',
  idle: '空闲',
}

export const DispatchOrderStatusTagType: Record<string, string> = {
  pending: 'danger',
  dispatching: 'info',
  executing: 'warning',
  completed: 'success',
  shortage: 'danger',
  idle: 'info',
}

export const DisposalPlanStatus = {
  DRAFT: 'draft',
  SUBMITTED: 'submitted',
  ACCEPTED: 'accepted',
  REJECTED: 'rejected',
  RESUBMITTED: 'resubmitted',
  NONE: '',
} as const

export type DisposalPlanStatusValue = (typeof DisposalPlanStatus)[keyof typeof DisposalPlanStatus]

export const DisposalPlanStatusLabel: Record<string, string> = {
  draft: '草稿',
  submitted: '待审核',
  accepted: '已通过',
  rejected: '已驳回',
  resubmitted: '重新提交',
  '': '未拟定',
}

export const DisposalPlanStatusTagType: Record<string, string> = {
  draft: 'info',
  submitted: 'warning',
  accepted: 'success',
  rejected: 'danger',
  resubmitted: 'warning',
  '': 'info',
}

export const ResourceDispatchStatus = {
  IDLE: 'idle',
  DISPATCHING: 'dispatching',
  EXECUTING: 'executing',
  SHORTAGE: 'shortage',
  COMPLETED: 'completed',
  NONE: '',
} as const

export type ResourceDispatchStatusValue = (typeof ResourceDispatchStatus)[keyof typeof ResourceDispatchStatus]

export const ResourceDispatchStatusLabel: Record<string, string> = {
  idle: '空闲',
  dispatching: '调度中',
  executing: '执行中',
  shortage: '资源不足',
  completed: '已完成',
  '': '未调度',
}

export const ResourceDispatchStatusTagType: Record<string, string> = {
  idle: 'info',
  dispatching: 'warning',
  executing: 'warning',
  shortage: 'danger',
  completed: 'success',
  '': 'info',
}

export const ApplicationStatus = {
  PENDING: 'pending',
  APPROVED: 'approved',
  RECEIVED: 'received',
  REJECTED: 'rejected',
} as const

export type ApplicationStatusValue = (typeof ApplicationStatus)[keyof typeof ApplicationStatus]

export const ApplicationStatusLabel: Record<ApplicationStatusValue, string> = {
  pending: '待审核',
  approved: '已通过',
  received: '已接收',
  rejected: '已驳回',
}

export const ApplicationStatusTagType: Record<ApplicationStatusValue, string> = {
  pending: 'warning',
  approved: 'success',
  received: 'info',
  rejected: 'danger',
}
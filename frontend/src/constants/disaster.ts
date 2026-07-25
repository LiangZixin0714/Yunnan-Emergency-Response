import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'

export const DisasterTypeColorMap: Record<DisasterTypeValue, string> = {
  earthquake: '#FF0000',
  mudslide: '#8B4513',
  flood: '#0000FF',
  drought: '#FFD700',
  landslide: '#000000',
  fire: '#008000',
  other: '#FFFFFF',
} as const

export const IncidentLevelSizeMap: Record<IncidentLevelValue, number> = {
  I: 24,
  II: 18,
  III: 14,
  IV: 10,
} as const
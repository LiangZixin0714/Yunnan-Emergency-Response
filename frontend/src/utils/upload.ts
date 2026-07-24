import type { IncidentReportData } from '@/types/incident'

function formatDateTime(value: string | Date): string {
  const d = value instanceof Date ? value : new Date(value)
  if (isNaN(d.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function buildIncidentFormData(data: IncidentReportData): FormData {
  const formData = new FormData()

  formData.append('incidentName', data.incidentName)
  formData.append('disasterType', data.disasterType)
  if (data.incidentLevel) formData.append('incidentLevel', data.incidentLevel)
  if (data.occurTime) formData.append('occurTime', formatDateTime(data.occurTime))
  if (data.location) formData.append('location', data.location)
  if (data.description) formData.append('description', data.description)

  if (data.images?.length) {
    data.images.forEach((file) => {
      formData.append('images', file)
    })
  }

  return formData
}
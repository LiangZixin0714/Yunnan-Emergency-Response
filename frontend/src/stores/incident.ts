import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Incident } from '@/types/incident'
import type { IncidentListParams, IncidentReportData, IncidentReportResult } from '@/types/incident'
import { getIncidentList, getIncidentDetail, reportIncident } from '@/api/incident'

export const useIncidentStore = defineStore('incident', () => {
  const incidentList = ref<Incident[]>([])
  const currentIncident = ref<Incident | null>(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchList(params: IncidentListParams): Promise<void> {
    loading.value = true
    try {
      const res = await getIncidentList(params) as unknown as { total: number; list: Incident[] }
      incidentList.value = res.list
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id: string): Promise<void> {
    loading.value = true
    try {
      const res = await getIncidentDetail(id) as unknown as Incident
      currentIncident.value = res
    } finally {
      loading.value = false
    }
  }

  async function report(data: IncidentReportData): Promise<IncidentReportResult | undefined> {
    const res = await reportIncident(data) as unknown as IncidentReportResult
    return res
  }

  return {
    incidentList,
    currentIncident,
    total,
    loading,
    fetchList,
    fetchDetail,
    report,
  }
})
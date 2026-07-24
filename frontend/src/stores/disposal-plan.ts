import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DisposalPlan } from '@/types/disposal-plan'
import { getDisposalPlanList, submitDisposalPlan, rejectDisposalPlan, saveDisposalPlan } from '@/api/disposal-plan'

export const useDisposalPlanStore = defineStore('disposal-plan', () => {
  const disposalPlanList = ref<DisposalPlan[]>([])
  const currentDisposalPlan = ref<DisposalPlan | null>(null)
  const loading = ref(false)

  async function fetchList(incidentId?: string): Promise<void> {
    loading.value = true
    try {
      const res = await getDisposalPlanList(incidentId ? { incidentId } : undefined) as unknown as DisposalPlan[]
      disposalPlanList.value = res
    } finally {
      loading.value = false
    }
  }

  async function submitDisposalPlanAction(id: number, planContent?: string, incidentId?: string): Promise<void> {
    await submitDisposalPlan({ id, planContent, incidentId })
  }

  async function saveDisposalPlanAction(id: number, planContent: string, incidentId?: string): Promise<void> {
    await saveDisposalPlan({ id, planContent, incidentId })
  }

  async function rejectDisposalPlanAction(id: number, reason: string, incidentId?: string): Promise<void> {
    await rejectDisposalPlan({ id, rejectReason: reason, incidentId })
  }

  return {
    disposalPlanList,
    currentDisposalPlan,
    loading,
    fetchList,
    submitDisposalPlan: submitDisposalPlanAction,
    saveDisposalPlan: saveDisposalPlanAction,
    rejectDisposalPlan: rejectDisposalPlanAction,
  }
})
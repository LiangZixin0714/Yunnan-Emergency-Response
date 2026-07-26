import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Plan } from '@/types/plan'
import { getPlanList, getPlanDetail, streamPlan, deletePlan } from '@/api/plan'
import { getStoredToken } from '@/utils/token'

export const usePlanStore = defineStore('plan', () => {
  const planList = ref<Plan[]>([])
  const currentPlan = ref<Plan | null>(null)

  const streamingContent = ref('')
  const streaming = ref(false)

  async function fetchList(incidentId: string): Promise<void> {
    const res = await getPlanList(incidentId) as unknown as Plan[]
    planList.value = res
  }


  async function fetchDetail(planId: string): Promise<void> {
    const res = await getPlanDetail(planId) as unknown as Plan
    currentPlan.value = res
  }

  function startStream(incidentId: string): void {
    streaming.value = true
    streamingContent.value = ''
    const token = getStoredToken() || ''
    console.log('startStream: incidentId:', incidentId, 'token:', token ? 'has token' : 'no token')

    streamPlan(
      incidentId,
      token,
      (text: string) => { 
        console.log('startStream: received chunk, length:', text.length)
        streamingContent.value += text 
      },
      () => { 
        console.log('startStream: done')
        streaming.value = false 
      },
      (err) => { 
        console.log('startStream: error:', err)
        streaming.value = false 
      },
    )
  }

  function stopStream(): void {
    streaming.value = false
  }

  async function removePlan(planId: string): Promise<void> {
    await deletePlan(planId)
    const index = planList.value.findIndex(p => p.planId === planId)
    if (index !== -1) {
      planList.value.splice(index, 1)
    }
    if (currentPlan.value?.planId === planId) {
      currentPlan.value = null
    }
  }

  return {
    planList,
    currentPlan,
    streamingContent,
    streaming,
    fetchList,
    fetchDetail,
    startStream,
    stopStream,
    removePlan,
  }
})

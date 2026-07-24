import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Plan } from '@/types/plan'
import { getPlanList, getPlanDetail, streamPlan } from '@/api/plan'
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

    streamPlan(
      incidentId,
      token,
      (text: string) => { streamingContent.value += text },
      () => { streaming.value = false },
      () => { streaming.value = false },
    )
  }

  function stopStream(): void {
    streaming.value = false
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
  }
})

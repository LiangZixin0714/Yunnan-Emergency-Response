import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Resource, ResourceListParams, DispatchOrder } from '@/types/resource'
import { getResourceList, getDispatchOrders } from '@/api/resource'

export const useResourceStore = defineStore('resource', () => {
  const resourceList = ref<Resource[]>([])
  const dispatchOrders = ref<DispatchOrder[]>([])
  const loading = ref(false)

  async function fetchList(params?: ResourceListParams): Promise<void> {
    loading.value = true
    try {
      const res = await getResourceList(params) as unknown as Resource[]
      resourceList.value = res
    } finally {
      loading.value = false
    }
  }

  async function fetchDispatchOrders(): Promise<void> {
    try {
      const res = await getDispatchOrders() as unknown as DispatchOrder[]
      dispatchOrders.value = res
    } catch {
      dispatchOrders.value = []
    }
  }

  return {
    resourceList,
    dispatchOrders,
    loading,
    fetchList,
    fetchDispatchOrders,
  }
})

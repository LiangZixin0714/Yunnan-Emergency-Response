import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Resource, ResourceListParams, DispatchOrder } from '@/types/resource'
import { getResourceList, getDispatchOrders, createResource, updateResource, deleteResource, lockResource as lockResourceApi } from '@/api/resource'

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

  async function createNewResource(data: {
    resourceName: string
    resourceType?: string
    unit?: string
    totalStock?: number
    availableStock?: number
    location?: string
    description?: string
  }): Promise<Resource> {
    const res = await createResource(data) as unknown as Resource
    resourceList.value.push(res)
    return res
  }

  async function updateExistingResource(id: number, data: {
    resourceName?: string
    resourceType?: string
    unit?: string
    totalStock?: number
    availableStock?: number
    location?: string
    description?: string
    status?: string
  }): Promise<Resource> {
    const res = await updateResource(id, data) as unknown as Resource
    const idx = resourceList.value.findIndex((r) => r.id === id)
    if (idx !== -1) {
      resourceList.value[idx] = res
    }
    return res
  }

  async function removeResource(id: number): Promise<void> {
    await deleteResource(id)
    const idx = resourceList.value.findIndex((r) => r.id === id)
    if (idx !== -1) {
      resourceList.value.splice(idx, 1)
    }
  }

  async function lockResource(data: {
    resourceId: string
    quantity: number
    incidentId?: string
    planId?: string
    remark?: string
  }): Promise<Resource | null> {
    const res = await lockResourceApi(data) as unknown as { success: boolean; resource: Resource }
    if (res && res.success && res.resource) {
      const idx = resourceList.value.findIndex((r) => r.resourceId === data.resourceId)
      if (idx !== -1) {
        resourceList.value[idx] = res.resource
      }
      return res.resource
    }
    return null
  }

  return {
    resourceList,
    dispatchOrders,
    loading,
    fetchList,
    fetchDispatchOrders,
    createNewResource,
    updateExistingResource,
    removeResource,
    lockResource,
  }
})

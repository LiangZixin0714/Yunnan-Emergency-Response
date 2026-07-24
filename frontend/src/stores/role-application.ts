import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RoleApplication, RoleApplicationSubmitData } from '@/types/role-application'
import { submitRoleApplication, getRoleApplicationList, reviewRoleApplication } from '@/api/role-application'

export const useRoleApplicationStore = defineStore('role-application', () => {
  const applicationList = ref<RoleApplication[]>([])
  const myApplications = ref<RoleApplication[]>([])
  const loading = ref(false)

  async function fetchList(params?: { userId?: number; status?: string }): Promise<void> {
    loading.value = true
    try {
      const res = await getRoleApplicationList(params) as unknown as RoleApplication[]
      applicationList.value = res
    } finally {
      loading.value = false
    }
  }

  async function fetchMyApplications(userId: number): Promise<void> {
    loading.value = true
    try {
      const res = await getRoleApplicationList({ userId, status: 'pending' }) as unknown as RoleApplication[]
      myApplications.value = res
    } finally {
      loading.value = false
    }
  }

  async function submitApplication(data: RoleApplicationSubmitData): Promise<void> {
    await submitRoleApplication(data)
  }

  async function reviewApplication(id: number, approved: boolean, reason?: string): Promise<void> {
    await reviewRoleApplication({ id, approved, reason })
  }

  return {
    applicationList,
    myApplications,
    loading,
    fetchList,
    fetchMyApplications,
    submitApplication,
    reviewApplication,
  }
})
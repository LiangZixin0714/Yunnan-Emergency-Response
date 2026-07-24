import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RoleApplication, RoleApplicationSubmitData } from '@/types/role-application'
import { submitRoleApplication, getRoleApplicationList, reviewRoleApplication, receiveRoleApplication } from '@/api/role-application'

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

  async function fetchMyApplications(userId?: number): Promise<void> {
    loading.value = true
    try {
      const res = await getRoleApplicationList(userId ? { userId } : undefined) as unknown as RoleApplication[]
      myApplications.value = res
    } finally {
      loading.value = false
    }
  }

  async function submitApplication(data: RoleApplicationSubmitData, userId: number): Promise<void> {
    await submitRoleApplication({ ...data, userId })
  }

  async function reviewApplication(id: number, approved: boolean, reason?: string): Promise<void> {
    await reviewRoleApplication({ id, approved, reason })
  }

  async function receiveApplication(id: number): Promise<void> {
    await receiveRoleApplication({ id })
  }

  return {
    applicationList,
    myApplications,
    loading,
    fetchList,
    fetchMyApplications,
    submitApplication,
    reviewApplication,
    receiveApplication,
  }
})
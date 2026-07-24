import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MemberInfo } from '@/types/member'
import { getMemberList, changeMemberRole } from '@/api/member'

export const useMemberStore = defineStore('member', () => {
  const memberList = ref<MemberInfo[]>([])
  const loading = ref(false)

  async function fetchList(params?: { keyword?: string; roleName?: string }): Promise<void> {
    loading.value = true
    try {
      const res = await getMemberList(params) as unknown as MemberInfo[]
      memberList.value = res
    } finally {
      loading.value = false
    }
  }

  async function changeRole(userId: number, targetRole: string): Promise<void> {
    await changeMemberRole({ userId, targetRole } as any)
  }

  return {
    memberList,
    loading,
    fetchList,
    changeRole,
  }
})
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useMemberStore } from '@/stores/member'
import { UserRoleLabel } from '@/types/enums'
import type { UserRoleValue } from '@/types/enums'
import { ElMessage } from 'element-plus'

const memberStore = useMemberStore()

const roleDialogVisible = ref(false)
const selectedMember = ref<{ userId: number; username: string; roleName: UserRoleValue } | null>(null)
const newRole = ref<UserRoleValue | ''>('')

const filters = reactive({
  keyword: '',
  roleName: '' as UserRoleValue | '',
})

const roleOptions = Object.entries(UserRoleLabel).map(([value, label]) => ({ value, label }))

async function loadMembers(): Promise<void> {
  await memberStore.fetchList({
    keyword: filters.keyword || undefined,
    roleName: filters.roleName || undefined,
  })
}

function handleSearch(): void {
  loadMembers()
}

function resetFilters(): void {
  filters.keyword = ''
  filters.roleName = ''
  loadMembers()
}

function openRoleDialog(member: { userId: number; username: string; roleName: UserRoleValue }): void {
  selectedMember.value = member
  newRole.value = member.roleName
  roleDialogVisible.value = true
}

async function handleChangeRole(): Promise<void> {
  if (!selectedMember.value || !newRole.value) return
  try {
    await memberStore.changeRole(selectedMember.value.userId, newRole.value)
    ElMessage.success('角色变更成功')
    roleDialogVisible.value = false
    loadMembers()
  } catch {
    ElMessage.error('角色变更失败')
  }
}

onMounted(() => {
  loadMembers()
})
</script>

<template>
  <div class="member-page page-container">
    <div class="page-header">
      <h2 class="page-header__title">成员管理</h2>
    </div>

    <el-card shadow="hover">
      <el-form inline class="member-page__filter">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="用户名/姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.roleName" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="memberStore.memberList" v-loading="memberStore.loading" stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120">
          <template #default="{ row }">{{ row.realName ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="140">
          <template #default="{ row }">
            {{ UserRoleLabel[row.roleName as UserRoleValue] ?? row.roleName }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registeredAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ row.registeredAt?.replace('T', ' ') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoleDialog(row)">角色变动</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="roleDialogVisible" title="角色变更" width="400px">
      <el-form label-width="80px" v-if="selectedMember">
        <el-form-item label="用户名">
          <span>{{ selectedMember.username }}</span>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="newRole" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeRole">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.member-page__filter {
  margin-bottom: var(--spacing-sm);
}
</style>

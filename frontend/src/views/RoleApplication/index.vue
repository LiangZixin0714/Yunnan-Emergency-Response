<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoleApplicationStore } from '@/stores/role-application'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import { ApplicationStatusLabel, ApplicationStatusTagType } from '@/types/enums'
import type { ApplicationStatusValue, UserRoleValue } from '@/types/enums'
import { UserRoleLabel } from '@/types/enums'
import { ElMessage } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const roleApplicationStore = useRoleApplicationStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  targetRole: '' as Exclude<UserRoleValue, 'VIEWER'> | '',
  reason: '',
})

const targetRoleOptions: { value: Exclude<UserRoleValue, 'VIEWER'>; label: string }[] = [
  { value: 'ADMIN', label: UserRoleLabel['ADMIN'] },
  { value: 'OPERATOR', label: UserRoleLabel['OPERATOR'] },
  { value: 'RESOURCE_MANAGER', label: UserRoleLabel['RESOURCE_MANAGER'] },
]

const rules: FormRules = {
  targetRole: [{ required: true, message: '请选择目标角色', trigger: 'change' }],
  reason: [
    { required: true, message: '请输入申请理由', trigger: 'blur' },
    { min: 10, max: 500, message: '申请理由长度10-500字符', trigger: 'blur' },
  ],
}

const applicationStatusMap = Object.fromEntries(
  Object.entries(ApplicationStatusLabel).map(([key, label]) => [
    key,
    { label, type: ApplicationStatusTagType[key as ApplicationStatusValue] },
  ])
)

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await roleApplicationStore.submitApplication({
      targetRole: form.targetRole as Exclude<UserRoleValue, 'VIEWER'>,
      reason: form.reason,
    }, authStore.userId!)
    ElMessage.success('申请已提交，等待管理员审核')
    form.targetRole = ''
    form.reason = ''
    await roleApplicationStore.fetchMyApplications(authStore.userId!)
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (authStore.userId) {
    roleApplicationStore.fetchMyApplications(authStore.userId)
  }
})

function scrollToRecords(): void {
  const el = document.querySelector('.role-application__list')
  el?.scrollIntoView({ behavior: 'smooth' })
}

async function handleReceiveApplication(id: number): Promise<void> {
  try {
    await roleApplicationStore.receiveApplication(id)
    ElMessage.success('角色变更成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch {
    ElMessage.error('角色变更失败，请重试')
  }
}
</script>

<template>
  <div class="role-application page-container">
    <div class="page-header">
      <h2 class="page-header__title">角色申请</h2>
      <el-button link type="primary" @click="scrollToRecords">申请记录</el-button>
    </div>

    <el-card shadow="hover" class="role-application__form">
      <template #header>
        <span>申请更高权限角色</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
        <el-form-item label="目标角色" prop="targetRole">
          <el-select v-model="form.targetRole" placeholder="请选择目标角色" style="width: 100%">
            <el-option
              v-for="opt in targetRoleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入申请理由（10-500字符）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="role-application__list">
      <template #header>
        <span>我的申请</span>
      </template>
      <el-table :data="roleApplicationStore.myApplications" v-loading="roleApplicationStore.loading" stripe>
        <el-table-column prop="targetRole" label="申请角色" width="160">
          <template #default="{ row }">
            {{ UserRoleLabel[row.targetRole as UserRoleValue] ?? row.targetRole }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column prop="status" label="审核状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" :status-map="applicationStatusMap" />
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="200">
          <template #default="{ row }">
            {{ row.rejectReason ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 'rejected'"
              type="primary"
              size="small"
              :disabled="row.status !== 'approved'"
              @click="handleReceiveApplication(row.id)"
            >
              接收审批
            </el-button>
            <span v-else class="role-application__failed">
              <el-icon color="#F56C6C"><CircleCloseFilled /></el-icon>
              申请失败
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.role-application__form {
  margin-bottom: var(--spacing-md);
}

.role-application__failed {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #F56C6C;
  font-size: 13px;
}
</style>
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useIncidentStore } from '@/stores/incident'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DisasterTypeLabel, IncidentLevelLabel, IMAGE_ACCEPT_STRING } from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue } from '@/types/enums'
import FileUpload from '@/components/FileUpload.vue'

const router = useRouter()
const incidentStore = useIncidentStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const files = ref<File[]>([])

const form = reactive({
  incidentName: '',
  disasterType: '' as DisasterTypeValue | '',
  incidentLevel: '' as IncidentLevelValue | '',
  occurTime: '',
  location: '',
  description: '',
  deathCount: null as number | null,
  propertyLoss: null as number | null,
})

const rules: FormRules = {
  incidentName: [{ required: true, message: '请输入事件名称', trigger: 'blur' }],
  disasterType: [{ required: true, message: '请选择灾害类型', trigger: 'change' }],
  incidentLevel: [{ required: true, message: '请选择事件等级', trigger: 'change' }],
  occurTime: [{ required: true, message: '请选择发生时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入发生地点', trigger: 'blur' }],
  description: [{ required: true, message: '请输入事件描述', trigger: 'blur' }],
}

const disasterOptions = Object.entries(DisasterTypeLabel).map(([value, label]) => ({
  value,
  label,
}))

const levelOptions = Object.entries(IncidentLevelLabel).map(([value, label]) => ({
  value,
  label,
}))

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('确认提交灾情上报信息？', '确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    await incidentStore.report({
      incidentName: form.incidentName,
      disasterType: form.disasterType as DisasterTypeValue,
      incidentLevel: form.incidentLevel as IncidentLevelValue,
      occurTime: form.occurTime,
      location: form.location,
      description: form.description,
      deathCount: form.deathCount ?? undefined,
      propertyLoss: form.propertyLoss ?? undefined,
      images: files.value,
    })
    ElMessage.success('灾情上报成功')
    router.push('/incident/list')
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="incident-report page-container">
    <div class="page-header">
      <h2 class="page-header__title">灾情上报</h2>
    </div>

    <el-card shadow="hover">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 700px"
      >
        <el-form-item label="事件名称" prop="incidentName">
          <el-input v-model="form.incidentName" placeholder="请输入事件名称" />
        </el-form-item>

        <el-form-item label="灾害类型" prop="disasterType">
          <el-select v-model="form.disasterType" placeholder="请选择灾害类型" style="width: 100%">
            <el-option
              v-for="opt in disasterOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="事件等级" prop="incidentLevel">
          <el-select v-model="form.incidentLevel" placeholder="请选择事件等级" style="width: 100%">
            <el-option
              v-for="opt in levelOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="发生时间" prop="occurTime">
          <el-date-picker
            v-model="form.occurTime"
            type="datetime"
            placeholder="请选择发生时间"
            style="width: 100%"
            :disabled-date="(d: Date) => d > new Date()"
          />
        </el-form-item>

        <el-form-item label="发生地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入发生地点" />
        </el-form-item>

        <el-form-item label="事件描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入事件描述"
          />
        </el-form-item>

        <el-form-item label="死亡人数">
          <el-input-number v-model="form.deathCount" :min="0" :step="1" :precision="0" placeholder="选填" />
        </el-form-item>

        <el-form-item label="财产损失/万元">
          <el-input-number v-model="form.propertyLoss" :min="0" :step="0.01" :precision="2" placeholder="选填" />
        </el-form-item>

        <el-form-item label="现场图片">
          <FileUpload v-model="files" :accept="IMAGE_ACCEPT_STRING" :image-mode="true" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交上报
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

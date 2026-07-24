<script setup lang="ts">
import { computed } from 'vue'
import type { Resource } from '@/types/resource'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  resourceList: Resource[]
  modelValue: string
  increaseQuantity: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:increaseQuantity': [value: number]
  confirm: [resourceId: string, quantity: number]
}>()

const resourceOptions = computed(() => {
  return props.resourceList.map((res) => ({
    value: res.resourceId,
    label: `${res.name}（总数${res.quantity}，已调度${res.dispatchedCount ?? 0}，可分配${res.quantity - (res.dispatchedCount ?? 0)}）`,
  }))
})

function handleConfirm(): void {
  if (!props.modelValue) {
    ElMessage.warning('请先选择资源')
    return
  }
  if (!props.increaseQuantity || props.increaseQuantity <= 0) {
    ElMessage.warning('请输入有效的增加数量')
    return
  }
  emit('confirm', props.modelValue, props.increaseQuantity)
}
</script>

<template>
  <div class="resource-search-item">
    <el-select
      :model-value="modelValue"
      filterable
      placeholder="搜索资源名称"
      style="flex: 1; min-width: 200px"
      @update:model-value="emit('update:modelValue', $event)"
    >
      <el-option
        v-for="opt in resourceOptions"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
      />
    </el-select>
    <el-input-number
      :model-value="increaseQuantity"
      :min="1"
      :step="1"
      :precision="0"
      placeholder="增加数量"
      style="width: 140px; margin-left: 8px"
      @update:model-value="emit('update:increaseQuantity', $event ?? 0)"
    />
    <el-button type="primary" style="margin-left: 8px" @click="handleConfirm">确认增加</el-button>
  </div>
</template>

<style scoped>
.resource-search-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
</style>

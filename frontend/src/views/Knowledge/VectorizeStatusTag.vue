<script setup lang="ts">
import { ElTooltip, ElTag } from 'element-plus'
import { VECTORIZE_STATUS, type VectorizeStatus } from '@/api/knowledge'

const props = defineProps<{
  status: VectorizeStatus
  failReason?: string
}>()

const statusConfig: Record<VectorizeStatus, { type: 'info' | 'warning' | 'success' | 'danger'; label: string }> = {
  [VECTORIZE_STATUS.PENDING]: { type: 'info', label: '待处理' },
  [VECTORIZE_STATUS.PROCESSING]: { type: 'warning', label: '处理中' },
  [VECTORIZE_STATUS.COMPLETED]: { type: 'success', label: '已完成' },
  [VECTORIZE_STATUS.FAILED]: { type: 'danger', label: '失败' },
}

const config = statusConfig[props.status] ?? statusConfig[VECTORIZE_STATUS.PENDING]
</script>

<template>
  <ElTooltip v-if="status === VECTORIZE_STATUS.FAILED && failReason" :content="failReason" placement="top">
    <ElTag :type="config.type" size="small">{{ config.label }}</ElTag>
  </ElTooltip>
  <ElTag v-else :type="config.type" size="small">{{ config.label }}</ElTag>
</template>
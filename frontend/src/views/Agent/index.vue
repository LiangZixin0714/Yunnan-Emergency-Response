<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAgentRunList, getAgentRunCitations, getAuditLogList } from '@/api/agent'
import type { AgentRun, Citation, AuditLog, PageResult } from '@/types/agent'

const activeTab = ref('agent')

const agentLoading = ref(false)
const agentData = ref<PageResult<AgentRun>>({ list: [], total: 0, page: 1, size: 10 })
const agentPage = ref(1)
const agentSize = ref(10)
const agentFilters = ref({ incidentId: '', agentName: '', status: '' })

const auditLoading = ref(false)
const auditData = ref<PageResult<AuditLog>>({ list: [], total: 0, page: 1, size: 10 })
const auditPage = ref(1)
const auditSize = ref(10)
const auditFilters = ref({ userId: undefined as number | undefined, module: '', action: '' })

const detailVisible = ref(false)
const detailItem = ref<AgentRun | null>(null)
const citationsVisible = ref(false)
const citations = ref<Citation[]>([])
const citationsLoading = ref(false)

const auditDetailVisible = ref(false)
const auditDetailItem = ref<AuditLog | null>(null)

const statusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '超时', value: 'timeout' },
]

const moduleOptions = [
  { label: '全部', value: '' },
  { label: '认证', value: 'auth' },
  { label: '灾情', value: 'incident' },
  { label: '方案', value: 'plan' },
  { label: '资源', value: 'resource' },
  { label: '处置方案', value: 'disposal-plan' },
  { label: '资源请求', value: 'resource-request' },
]

async function fetchAgentRuns() {
  agentLoading.value = true
  try {
    const res = await getAgentRunList({
      page: agentPage.value,
      size: agentSize.value,
      incidentId: agentFilters.value.incidentId || undefined,
      agentName: agentFilters.value.agentName || undefined,
      status: agentFilters.value.status || undefined,
    })
    agentData.value = res
  } catch {
    ElMessage.error('获取Agent执行记录失败')
  } finally {
    agentLoading.value = false
  }
}

async function fetchAuditLogs() {
  auditLoading.value = true
  try {
    const res = await getAuditLogList({
      page: auditPage.value,
      size: auditSize.value,
      userId: auditFilters.value.userId || undefined,
      module: auditFilters.value.module || undefined,
      action: auditFilters.value.action || undefined,
    })
    auditData.value = res
  } catch {
    ElMessage.error('获取审计日志失败')
  } finally {
    auditLoading.value = false
  }
}

function showAgentDetail(row: AgentRun) {
  detailItem.value = row
  detailVisible.value = true
}

async function showCitations(row: AgentRun) {
  citationsVisible.value = true
  citationsLoading.value = true
  citations.value = []
  try {
    citations.value = await getAgentRunCitations(row.id)
  } catch {
    ElMessage.error('获取引用来源失败')
  } finally {
    citationsLoading.value = false
  }
}

function showAuditDetail(row: AuditLog) {
  auditDetailItem.value = row
  auditDetailVisible.value = true
}

function handleAgentPageChange(page: number) {
  agentPage.value = page
  fetchAgentRuns()
}

function handleAuditPageChange(page: number) {
  auditPage.value = page
  fetchAuditLogs()
}

function searchAgent() {
  agentPage.value = 1
  fetchAgentRuns()
}

function searchAudit() {
  auditPage.value = 1
  fetchAuditLogs()
}

function formatTime(val: string | null) {
  if (!val) return '-'
  return val.replace('T', ' ').substring(0, 19)
}

function getStatusType(status: string) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'timeout') return 'warning'
  return 'info'
}

onMounted(() => {
  fetchAgentRuns()
})
</script>

<template>
  <div class="agent-page page-container">
    <div class="page-header">
      <h2 class="page-header__title">Agent与审计日志</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="(tab: string | number) => { if (tab === 'audit') fetchAuditLogs() }">
      <el-tab-pane label="Agent执行记录" name="agent">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <el-form :inline="true" @submit.prevent="searchAgent">
            <el-form-item label="灾情ID">
              <el-input v-model="agentFilters.incidentId" placeholder="输入incidentId" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="Agent名称">
              <el-input v-model="agentFilters.agentName" placeholder="输入agentName" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="agentFilters.status" style="width: 120px">
                <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchAgent">查询</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <el-table :data="agentData.list" v-loading="agentLoading" stripe @row-click="showAgentDetail" style="cursor: pointer">
            <el-table-column prop="runId" label="Run ID" width="180" show-overflow-tooltip />
            <el-table-column prop="agentName" label="Agent名称" width="140" />
            <el-table-column prop="incidentId" label="灾情ID" width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="170">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column prop="endTime" label="结束时间" width="170">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click.stop="showCitations(row)">引用来源</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: flex-end; margin-top: 16px">
            <el-pagination
              v-model:current-page="agentPage"
              :page-size="agentSize"
              :total="agentData.total"
              layout="total, prev, pager, next"
              @current-change="handleAgentPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="audit">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <el-form :inline="true" @submit.prevent="searchAudit">
            <el-form-item label="模块">
              <el-select v-model="auditFilters.module" style="width: 140px">
                <el-option v-for="opt in moduleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作">
              <el-input v-model="auditFilters.action" placeholder="输入action" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="searchAudit">查询</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <el-table :data="auditData.list" v-loading="auditLoading" stripe @row-click="showAuditDetail" style="cursor: pointer">
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="action" label="操作" width="120" />
            <el-table-column prop="actionType" label="类型" width="100" />
            <el-table-column prop="ipAddress" label="IP地址" width="140" />
            <el-table-column prop="duration" label="耗时(ms)" width="100" />
            <el-table-column prop="createdAt" label="操作时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: flex-end; margin-top: 16px">
            <el-pagination
              v-model:current-page="auditPage"
              :page-size="auditSize"
              :total="auditData.total"
              layout="total, prev, pager, next"
              @current-change="handleAuditPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="执行记录详情" width="700px">
      <template v-if="detailItem">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Run ID">{{ detailItem.runId }}</el-descriptions-item>
          <el-descriptions-item label="Agent名称">{{ detailItem.agentName }}</el-descriptions-item>
          <el-descriptions-item label="灾情ID">{{ detailItem.incidentId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailItem.status)" size="small">{{ detailItem.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatTime(detailItem.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatTime(detailItem.endTime) }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <h4>输入参数</h4>
          <el-input type="textarea" :model-value="detailItem.inputParams" :rows="4" readonly />
        </div>
        <div style="margin-top: 12px">
          <h4>输出结果</h4>
          <el-input type="textarea" :model-value="detailItem.outputResult" :rows="6" readonly />
        </div>
        <div v-if="detailItem.errorMessage" style="margin-top: 12px">
          <h4>错误信息</h4>
          <el-input type="textarea" :model-value="detailItem.errorMessage" :rows="3" readonly />
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="citationsVisible" title="引用来源" width="700px">
      <el-table :data="citations" v-loading="citationsLoading" stripe>
        <el-table-column prop="sourceText" label="来源文本" show-overflow-tooltip />
        <el-table-column prop="sourceUrl" label="来源URL" width="200" show-overflow-tooltip />
        <el-table-column prop="relevanceScore" label="相关度" width="100" />
      </el-table>
      <el-empty v-if="!citationsLoading && citations.length === 0" description="暂无引用来源" />
    </el-dialog>

    <el-dialog v-model="auditDetailVisible" title="审计日志详情" width="700px">
      <template v-if="auditDetailItem">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户">{{ auditDetailItem.username }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ auditDetailItem.module }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ auditDetailItem.action }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ auditDetailItem.actionType }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ auditDetailItem.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="耗时(ms)">{{ auditDetailItem.duration }}</el-descriptions-item>
          <el-descriptions-item label="目标类型">{{ auditDetailItem.targetType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标ID">{{ auditDetailItem.targetId || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <h4>请求参数</h4>
          <el-input type="textarea" :model-value="auditDetailItem.requestParams" :rows="4" readonly />
        </div>
        <div style="margin-top: 12px">
          <h4>执行结果</h4>
          <el-input type="textarea" :model-value="auditDetailItem.result" :rows="4" readonly />
        </div>
        <div v-if="auditDetailItem.detail" style="margin-top: 12px">
          <h4>详情</h4>
          <el-input type="textarea" :model-value="auditDetailItem.detail" :rows="3" readonly />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.agent-page {
  padding: 20px;
}
</style>

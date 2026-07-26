<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { UploadFilled, Download, Delete, Document, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import {
  getKnowledgeList,
  uploadKnowledgeFile,
  deleteKnowledgeFile,
  getKnowledgeDownloadUrl,
  retryVectorize,
  VECTORIZE_STATUS,
  type KnowledgeFile,
} from '@/api/knowledge'
import VectorizeStatusTag from './VectorizeStatusTag.vue'

const fileList = ref<KnowledgeFile[]>([])
const loading = ref(false)
const uploading = ref(false)
const uploadDialogVisible = ref(false)
const uploadFile = ref<File | null>(null)
const uploadRef = ref<InstanceType<typeof import('element-plus')['ElUpload']>>()

function openUploadDialog() {
  uploadFile.value = null
  uploadDialogVisible.value = true
  setTimeout(() => {
    uploadRef.value?.clearFiles()
  }, 0)
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').slice(0, 19)
}

async function loadFiles() {
  loading.value = true
  try {
    fileList.value = await getKnowledgeList() as unknown as KnowledgeFile[]
  } catch {
    fileList.value = []
  } finally {
    loading.value = false
  }
}

function handleFileChange(uploadFileRaw: UploadFile) {
  if (uploadFileRaw.raw) {
    if (uploadFileRaw.raw.type !== 'application/pdf') {
      ElMessage.warning('仅支持上传PDF文件')
      uploadRef.value?.clearFiles()
      uploadFile.value = null
      return
    }
    uploadFile.value = uploadFileRaw.raw
  }
}

function handleFileRemove() {
  uploadFile.value = null
}

async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    await uploadKnowledgeFile(uploadFile.value)
    ElMessage.success('文件上传成功，正在自动向量化处理')
    uploadDialogVisible.value = false
    uploadFile.value = null
    await loadFiles()
    setTimeout(() => { loadFiles() }, 3000)
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function handleDownload(row: KnowledgeFile) {
  const url = getKnowledgeDownloadUrl(row.fileId)
  const token = sessionStorage.getItem('token')
  const link = document.createElement('a')
  link.href = url + (token ? `?token=${encodeURIComponent(token)}` : '')
  link.target = '_blank'
  link.download = row.fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

async function handleDelete(row: KnowledgeFile) {
  try {
    await ElMessageBox.confirm(`确认删除文件"${row.fileName}"？删除后不可恢复`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteKnowledgeFile(row.fileId)
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch {
    return
  }
}

async function handleRetry(row: KnowledgeFile) {
  try {
    await retryVectorize(row.fileId)
    ElMessage.success('重新处理已触发')
    await loadFiles()
    setTimeout(() => { loadFiles() }, 3000)
  } catch (e: any) {
    ElMessage.error(e.message || '重新处理失败')
  }
}

onMounted(() => {
  loadFiles()
})
</script>

<template>
  <div class="knowledge-page page-container">
    <div class="page-header">
      <h2 class="page-header__title">知识库管理</h2>
      <el-button type="primary" :icon="UploadFilled" @click="openUploadDialog">
        上传文件
      </el-button>
    </div>

    <el-card shadow="hover">
      <el-table :data="fileList" v-loading="loading" stripe>
        <el-table-column label="文件" min-width="280">
          <template #default="{ row }">
            <div class="file-info">
              <el-icon :size="28" color="#e74c3c"><Document /></el-icon>
              <div class="file-info__text">
                <div class="file-info__name">{{ row.fileName }}</div>
                <div class="file-info__meta">
                  PDF · {{ formatFileSize(row.fileSize) }}
                  <template v-if="row.chunkCount > 0"> · {{ row.chunkCount }}个知识块</template>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="uploaderName" label="上传者" width="100" />
        <el-table-column label="向量化状态" width="110">
          <template #default="{ row }">
            <VectorizeStatusTag :status="row.vectorizeStatus" :fail-reason="row.vectorizeFailReason" />
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Download" @click="handleDownload(row)">
              下载
            </el-button>
            <el-button v-if="row.vectorizeStatus === VECTORIZE_STATUS.FAILED" link type="warning" :icon="RefreshRight" @click="handleRetry(row)">
              重新处理
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && fileList.length === 0" description="暂无知识库文件，点击上方按钮上传" />
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".pdf"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            drag
          >
            <el-icon :size="40" class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">将PDF文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持PDF文件，单文件不超过50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          {{ uploading ? '上传中...' : '确认上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-info__text {
  flex: 1;
  min-width: 0;
}

.file-info__name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.upload-icon {
  color: var(--el-text-color-placeholder);
}
</style>

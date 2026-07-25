<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ALLOWED_IMAGE_TYPES } from '@/types/enums'

const props = withDefaults(defineProps<{
  modelValue: File[]
  maxCount?: number
  maxSize?: number
  accept?: string
  imageMode?: boolean
}>(), {
  maxCount: 5,
  maxSize: 10 * 1024 * 1024,
  imageMode: true,
})

const emit = defineEmits<{
  'update:modelValue': [files: File[]]
  'upload-error': [message: string]
}>()

const fileList = ref<Array<{ uid: string; name: string; status: string; url: string; raw: File }>>([])

const fileMap = new Map<string, File>()

function syncFileList(): void {
  fileList.value.forEach((item) => {
    if (item.url) URL.revokeObjectURL(item.url)
    fileMap.delete(item.uid)
  })
  
  fileList.value = props.modelValue.map((file, index) => {
    const uid = `${Date.now()}-${index}`
    fileMap.set(uid, file)
    return {
      uid,
      name: file.name,
      status: 'success',
      url: URL.createObjectURL(file),
      raw: file,
    }
  })
}

watch(() => props.modelValue, syncFileList, { immediate: true })

function validateFile(file: File): boolean {
  if (props.imageMode) {
    const isImage = ALLOWED_IMAGE_TYPES.includes(file.type as typeof ALLOWED_IMAGE_TYPES[number]) || 
                    /\.(jpg|jpeg|png|gif|webp)$/i.test(file.name)
    if (!isImage) {
      ElMessage.warning('仅支持jpg/jpeg/png/gif/webp格式图片')
      emit('upload-error', '仅支持jpg/jpeg/png/gif/webp格式图片')
      return false
    }
  }
  if (file.size > props.maxSize) {
    const msg = props.imageMode ? '图片大小不能超过10MB' : '文件大小不能超过10MB'
    ElMessage.warning(msg)
    emit('upload-error', msg)
    return false
  }
  if (props.modelValue.length >= props.maxCount) {
    const msg = props.imageMode ? '最多上传5张图片' : '最多上传5个文件'
    ElMessage.warning(msg)
    emit('upload-error', msg)
    return false
  }
  return true
}

function handleChange(file: { raw: File; uid: string }, _fileListChange: Array<{ uid: string; name: string; status: string; url: string; raw: File }>): void {
  console.log('=== handleChange ===')
  console.log('file:', file)
  console.log('file.raw:', file.raw)
  
  if (file.raw) {
    if (validateFile(file.raw)) {
      const newFiles = [...props.modelValue, file.raw]
      console.log('newFiles.length:', newFiles.length)
      emit('update:modelValue', newFiles)
    }
  }
}

function handleRemove(file: { uid: string; url: string }): void {
  console.log('=== handleRemove ===')
  console.log('file.uid:', file.uid)
  
  if (file.url) {
    URL.revokeObjectURL(file.url)
  }
  const rawFile = fileMap.get(file.uid)
  fileMap.delete(file.uid)
  
  if (rawFile) {
    const newFiles = props.modelValue.filter(f => f !== rawFile)
    emit('update:modelValue', newFiles)
  } else {
    const index = fileList.value.findIndex(item => item.uid === file.uid)
    if (index !== -1) {
      const newFiles = props.modelValue.filter((_, i) => i !== index)
      emit('update:modelValue', newFiles)
    }
  }
}

function handlePreview(_file: { url: string }): void {
}

onUnmounted(() => {
  fileList.value.forEach((item) => {
    if (item.url) URL.revokeObjectURL(item.url)
  })
  fileMap.clear()
})
</script>

<template>
  <div class="file-upload">
    <el-upload
      v-model:file-list="fileList"
      :auto-upload="false"
      :show-file-list="true"
      :list-type="imageMode ? 'picture-card' : 'text'"
      :accept="accept"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :limit="maxCount"
      multiple
      name="images"
    >
      <el-icon><Plus /></el-icon>
      <template #tip>
        <div class="file-upload__tip">
          {{ imageMode ? `单张图片不超过10MB，最多${maxCount}张图片` : `单文件不超过10MB，最多${maxCount}个文件` }}
        </div>
      </template>
    </el-upload>
  </div>
</template>

<style scoped>
.file-upload__tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}

:deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 120px;
  height: 120px;
}
</style>

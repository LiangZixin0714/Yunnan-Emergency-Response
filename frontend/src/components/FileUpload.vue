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

const previewUrls = ref<string[]>([])

function syncPreviewUrls(): void {
  previewUrls.value.forEach((url) => URL.revokeObjectURL(url))
  previewUrls.value = props.modelValue.map((file) => URL.createObjectURL(file))
}

watch(() => props.modelValue, syncPreviewUrls, { immediate: true })

function handleBeforeUpload(file: File): boolean {
  if (props.imageMode && !ALLOWED_IMAGE_TYPES.includes(file.type as typeof ALLOWED_IMAGE_TYPES[number])) {
    ElMessage.warning('仅支持jpg/jpeg/png/gif/webp格式图片')
    emit('upload-error', '仅支持jpg/jpeg/png/gif/webp格式图片')
    return false
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
  const newFiles = [...props.modelValue, file]
  emit('update:modelValue', newFiles)
  return false
}

function handleRemove(index: number): void {
  if (previewUrls.value[index]) {
    URL.revokeObjectURL(previewUrls.value[index])
  }
  const newFiles = props.modelValue.filter((_, i) => i !== index)
  emit('update:modelValue', newFiles)
}

onUnmounted(() => {
  previewUrls.value.forEach((url) => URL.revokeObjectURL(url))
})
</script>

<template>
  <div class="file-upload">
    <el-upload
      :auto-upload="false"
      :show-file-list="false"
      :accept="accept"
      :before-upload="handleBeforeUpload"
    >
      <el-button type="primary" plain>
        <el-icon><Upload /></el-icon>
        {{ imageMode ? '选择图片' : '选择文件' }}
      </el-button>
    </el-upload>

    <div v-if="imageMode && modelValue.length" class="file-upload__images">
      <div
        v-for="(_file, index) in modelValue"
        :key="index"
        class="file-upload__image-item"
      >
        <el-image
          :src="previewUrls[index]"
          fit="cover"
          class="file-upload__thumbnail"
          :preview-src-list="previewUrls"
          :initial-index="index"
        >
          <template #error>
            <div class="file-upload__image-error">
              <el-icon><PictureFilled /></el-icon>
              <span>图片加载失败</span>
            </div>
          </template>
        </el-image>
        <el-icon class="file-upload__image-remove" @click="handleRemove(index)"><Close /></el-icon>
      </div>
    </div>

    <div v-if="!imageMode && modelValue.length" class="file-upload__list">
      <div class="file-upload__item" v-for="(file, index) in modelValue" :key="index">
        <el-icon><Document /></el-icon>
        <span class="file-upload__name">{{ file.name }}</span>
        <el-icon class="file-upload__remove" @click="handleRemove(index)"><Close /></el-icon>
      </div>
    </div>

    <div class="file-upload__tip">
      {{ imageMode ? `单张图片不超过10MB，最多${maxCount}张图片` : `单文件不超过10MB，最多${maxCount}个文件` }}
    </div>
  </div>
</template>

<style scoped>
.file-upload__images {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.file-upload__image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: var(--border-radius-sm);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
}

.file-upload__thumbnail {
  width: 100%;
  height: 100%;
}

.file-upload__image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--color-bg-page);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  gap: var(--spacing-xs);
}

.file-upload__image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: background var(--transition-duration);
}

.file-upload__image-remove:hover {
  background: rgba(0, 0, 0, 0.8);
}

.file-upload__list {
  margin-top: var(--spacing-sm);
}

.file-upload__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) 0;
  color: var(--color-text-regular);
  font-size: var(--font-size-sm);
}

.file-upload__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-upload__remove {
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--transition-duration);
}

.file-upload__remove:hover {
  color: var(--color-danger);
}

.file-upload__tip {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xs);
}
</style>

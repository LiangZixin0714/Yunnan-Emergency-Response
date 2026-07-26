import request from '@/utils/request'

export const VECTORIZE_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  COMPLETED: 'completed',
  FAILED: 'failed',
} as const
export type VectorizeStatus = typeof VECTORIZE_STATUS[keyof typeof VECTORIZE_STATUS]

export interface KnowledgeFile {
  id: number
  fileId: string
  fileName: string
  fileSize: number
  fileType: string
  objectKey: string
  bucket: string
  description: string
  uploaderId: number
  uploaderName: string
  vectorizeStatus: VectorizeStatus
  vectorizeFailReason: string
  vectorizeStartedAt: string
  vectorizeCompletedAt: string
  vectorizeRetryCount: number
  chunkCount: number
  createdAt: string
  updatedAt: string
}

export function uploadKnowledgeFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/upload', formData)
}

export function getKnowledgeList() {
  return request.get('/knowledge/list')
}

export function deleteKnowledgeFile(fileId: string) {
  return request.delete(`/knowledge/delete/${fileId}`)
}

export function getKnowledgeDownloadUrl(fileId: string) {
  return `/api/knowledge/download/${fileId}`
}

export function retryVectorize(fileId: string) {
  return request.post(`/knowledge/vectorize/retry/${fileId}`)
}

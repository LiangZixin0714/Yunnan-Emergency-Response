import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'
import { getStoredToken, clearStoredToken } from '@/utils/token'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getStoredToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (config.data instanceof FormData) {
      config.headers['Content-Type'] = undefined
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown> | Blob>) => {
    if (response.config.responseType === 'blob' && response.data instanceof Blob) {
      const blobType = response.data.type || ''
      if (blobType && !blobType.includes('octet-stream') && !blobType.includes('pdf') && !blobType.includes('wordprocessingml') && !blobType.includes('msword') && !blobType.includes('zip')) {
        return response.data.text().then(text => {
          const res = JSON.parse(text) as ApiResponse<unknown>
          ElMessage.error(res.message || '请求失败')
          return Promise.reject(new Error(res.message || '请求失败'))
        })
      }
      return response.data
    }
    const res = response.data as ApiResponse<unknown>
    if (res.code === 0) {
      return res.data as any
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      let msg = ''
      if (data) {
        if (typeof data === 'string') {
          try { msg = JSON.parse(data).message || '' } catch { msg = '' }
        } else if (data.message) {
          msg = data.message
        } else if (data.msg) {
          msg = data.msg
        }
      }
      if (status === 401) {
        clearStoredToken()
        window.location.href = '/login'
        ElMessage.warning(msg || '登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error(msg || '权限不足，无法执行此操作')
      } else if (status >= 500) {
        ElMessage.error(msg || '服务暂时不可用，请稍后重试')
      } else if (msg) {
        ElMessage.error(msg)
      } else {
        ElMessage.error('请求失败')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接后重试')
    }
    return Promise.reject(error)
  }
)

export default request

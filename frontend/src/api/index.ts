// api/index.ts — Axios 实例与统一鉴权处理

import axios, { type AxiosError, type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ERROR_ACTION } from '@/constants/errors'
import { emitAuthExpired } from '@/utils/auth-events'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

instance.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      handleBusinessError(res.code, res.message)
      return Promise.reject(new BusinessError(res.code, res.message))
    }
    return res
  },
  (error: AxiosError<BusinessResponse>) => {
    const status = error.response?.status
    const res = error.response?.data

    switch (status) {
      case 401: {
        const authStore = useAuthStore()
        authStore.clearAuth()
        ElMessage.error('登录已过期，请重新登录')
        emitAuthExpired()
        break
      }
      case 403:
        ElMessage.error('无操作权限')
        break
      case 429:
        ElMessage.warning('请求频率过高，请稍后重试')
        break
      default:
        ElMessage.error(res?.message || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  }
)

function handleBusinessError(code: number, message: string) {
  const action = ERROR_ACTION[code]

  switch (action) {
    case 'toast':
      ElMessage.error(message)
      break
    case 'redirect-login': {
      const authStore = useAuthStore()
      authStore.clearAuth()
      emitAuthExpired()
      break
    }
    case 'retry-presign':
      console.warn(`[TOS presign retry] code=${code}, message=${message}`)
      break
    case 'disabled-btn':
      ElMessage.warning(message)
      break
    case 'global-error':
      ElMessage.error('服务器内部错误，请稍后重试')
      break
    default:
      ElMessage.error(message)
  }
}

interface BusinessResponse {
  code: number
  message: string
  data: any
  timestamp: number
}

class BusinessError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'BusinessError'
  }
}

export default instance
export { BusinessError }

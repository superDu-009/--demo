// api/index.ts — 系分第 5.1 节：Axios 实例与拦截器

import axios, { type AxiosInstance, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { ERROR_ACTION } from '@/constants/errors'

// ===== Axios 实例配置 =====
const instance: AxiosInstance = axios.create({
  // 从环境变量读取 API 基础路径，默认 /api
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,  // 请求超时 30s
  headers: { 'Content-Type': 'application/json' }
})

// ===== 请求拦截器：自动附加 Token =====
instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    // 如果已登录，在请求头中附加 Bearer Token
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ===== 响应拦截器：统一处理业务错误和 HTTP 错误 =====
instance.interceptors.response.use(
  (response) => {
    const res = response.data

    // HTTP 200 但业务码非 0 → 业务错误
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
      case 401:
        // v1.1：401 清除认证状态 + Toast 提示 + 跳转登录页
        const authStore = useAuthStore()
        authStore.clearAuth()
        ElMessage.error('登录已过期，请重新登录')
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
        break

      case 403:
        // v1.1：403 明确用 Toast 提示，不设计无权限跳转页面
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

// ===== 业务错误处理函数 =====
// 根据 ERROR_ACTION 映射表执行对应操作
function handleBusinessError(code: number, message: string) {
  const action = ERROR_ACTION[code]

  switch (action) {
    case 'toast':
      ElMessage.error(message)
      break
    case 'form':
      // 由调用方处理表单高亮
      break
    case 'redirect-login':
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push({ name: 'Login' })
      break
    case 'retry-presign':
      // v1.1：此处静默处理，不显示 Toast
      // Toast 由 useTosUpload 统一展示，避免重复提示
      console.warn(`[TOS presign retry] code=${code}, message=${message}`)
      break
    case 'disabled-btn':
      // 禁用对应按钮（由调用方处理）
      ElMessage.warning(message)
      break
    case 'global-error':
      ElMessage.error('服务器内部错误，请稍后重试')
      break
    default:
      ElMessage.error(message)
  }
}

// ===== 类型定义 =====
interface BusinessResponse {
  code: number
  message: string
  data: any
  timestamp: number
}

// 业务错误类
class BusinessError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'BusinessError'
  }
}

export default instance

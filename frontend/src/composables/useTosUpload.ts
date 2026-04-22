// composables/useTosUpload.ts — 系分第 8.3 节：TOS 预签名直传
import { ref } from 'vue'
import { tosApi } from '@/api/tos'
import type { PresignResult, TosCompleteRequest } from '@/types'
import { ElMessage } from 'element-plus'

// 上传选项
export interface UploadOptions {
  projectId: number
  fileType: 'novel' | 'asset' | 'other'
  projectDir?: string
  maxFileSize?: number       // 默认 50MB
  allowedTypes?: string[]    // 默认 ['image/png', 'image/jpeg', 'video/mp4', 'text/plain']
  onProgress?: (percent: number) => void // 进度回调
  onSuccess?: (accessUrl: string, key: string) => void // 成功回调
  onError?: (error: Error) => void // 失败回调
}

export function useTosUpload() {
  const uploading = ref(false)
  const progress = ref(0)
  let xhr: XMLHttpRequest | null = null // 保存XHR实例用于取消请求

  // 火山 TOS 预签名 URL 上传：直传文件到对象存储，不经过后端
  // 流程：请求预签名 URL → PUT 上传文件 → 返回文件 key 给后端保存
  async function upload(file: File, options: UploadOptions, retryCount = 0): Promise<string> {
    const MAX_RETRY = 3 // 最多重试3次，避免无限递归
    // 1. 校验文件大小
    const maxFileSize = options.maxFileSize || 50 * 1024 * 1024
    if (file.size > maxFileSize) {
      ElMessage.error(`文件大小不能超过 ${maxFileSize / 1024 / 1024}MB`)
      throw new Error('File too large')
    }

    // 2. 校验文件类型：双重校验（file.type + 文件头魔数 + 后缀名兼容）
    const allowedTypes = options.allowedTypes || ['image/png', 'image/jpeg', 'video/mp4', 'text/plain', 'text/markdown']
    const contentType = getContentType(file)
    if (!contentType || !allowedTypes.includes(contentType)) {
      ElMessage.error(`不支持的文件类型: ${file.type || file.name.split('.').pop()}`)
      throw new Error('Unsupported file type')
    }
    // 文件头魔数校验，防止篡改后缀绕过
    const fileTypeValid = await validateFileType(file, allowedTypes)
    if (!fileTypeValid) {
      ElMessage.error('文件真实类型与后缀不匹配，请选择正确的文件')
      throw new Error('Invalid file type')
    }

    uploading.value = true
    progress.value = 0

    try {
      // 3. 获取预签名 URL
      const presignRes = await tosApi.presign({
        fileName: file.name,
        contentType,
        source: 'frontend',
        businessId: options.projectId
      })
      const presignResult = presignRes.data

      // 4. 直传 TOS
      await uploadToTos(presignResult.uploadUrl, file, contentType, options.onProgress)

      // 5. 通知后端上传完成（v1.1：complete 失败仅重试 1 次）
      try {
        await tosApi.complete({
          fileKey: presignResult.fileKey,
          businessId: options.projectId,
          fileSize: file.size,
          originalName: file.name
        } as TosCompleteRequest)
      } catch (completeError) {
        // v1.1：仅重试 1 次，重试前增加1秒延迟，降低后端压力
        try {
          await new Promise(resolve => setTimeout(resolve, 1000))
          await tosApi.complete({
            fileKey: presignResult.fileKey,
            businessId: options.projectId,
            fileSize: file.size,
            originalName: file.name
          } as TosCompleteRequest)
        } catch (e2) {
          ElMessage.error('文件已上传，但记录保存失败，请刷新后重试')
          throw e2
        }
      }

      options.onSuccess?.(presignResult.accessUrl, presignResult.fileKey)
      return presignResult.accessUrl
    } catch (error: any) {
      // 预签名过期 → 重试最多3次，避免无限递归
      if ((error.code === 40005 || error.response?.data?.code === 40005) && retryCount < MAX_RETRY) {
        console.warn(`[TOS上传] 预签名过期，第${retryCount + 1}次重试`, error)
        return upload(file, options, retryCount + 1)
      }
      options.onError?.(error)
      throw error
    } finally {
      uploading.value = false
      progress.value = 0
    }
  }

  // XHR 直传 TOS（支持进度回调）
  async function uploadToTos(url: string, file: File, contentType: string, onProgress?: (percent: number) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      xhr = new XMLHttpRequest()
      xhr.open('PUT', url)
      xhr.setRequestHeader('Content-Type', contentType)

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          progress.value = Math.round((e.loaded / e.total) * 100)
          onProgress?.(progress.value)
        }
      }

      xhr.onload = () => {
        if (xhr && xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`TOS upload failed: ${xhr?.status}`))
        }
        xhr = null
      }

      xhr.onerror = () => {
        reject(new Error('TOS upload network error. 请检查 TOS Bucket CORS 是否允许当前前端域名、PUT 方法和 Content-Type 请求头'))
        xhr = null
      }
      xhr.send(file)
    })
  }

  // 文件头魔数映射表（仅校验常用类型）
  const FILE_MAGIC_NUMBERS: Record<string, string[]> = {
    'image/png': ['89504E47'],
    'image/jpeg': ['FFD8FF', 'FFD8FFDB', 'FFD8FFE0', 'FFD8FFE1'],
    'video/mp4': ['0000001866747970', '0000002066747970'],
    'text/plain': ['EFBBBF', 'FFFE', 'FEFF'], // UTF-8/UTF-16 BOM，无BOM纯文本后续补充逻辑
    'text/markdown': ['EFBBBF', 'FFFE', 'FEFF']
  }

  function getContentType(file: File): string {
    if (file.type) return file.type

    const ext = file.name.split('.').pop()?.toLowerCase()
    const fallbackTypes: Record<string, string> = {
      md: 'text/markdown',
      markdown: 'text/markdown',
      txt: 'text/plain',
      docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    }

    return ext ? fallbackTypes[ext] || '' : ''
  }

  // 校验文件真实类型（读取文件头魔数）
  async function validateFileType(file: File, allowedTypes: string[]): Promise<boolean> {
    return new Promise((resolve) => {
      // 文本类型跳过魔数校验（纯文本无固定魔数）
      if (allowedTypes.includes('text/plain') || allowedTypes.includes('text/markdown')) {
        resolve(true)
        return
      }
      
      const reader = new FileReader()
      reader.onload = (e) => {
        const arr = new Uint8Array(e.target?.result as ArrayBuffer).subarray(0, 8)
        const header = Array.from(arr, b => b.toString(16).padStart(2, '0')).join('').toUpperCase()
        
        // 检查匹配允许的类型
        for (const type of allowedTypes) {
          const magicNumbers = FILE_MAGIC_NUMBERS[type] || []
          if (magicNumbers.some(magic => header.startsWith(magic))) {
            resolve(true)
            return
          }
        }
        resolve(false)
      }
      reader.readAsArrayBuffer(file.slice(0, 8))
    })
  }

  // 取消上传请求
  function abort() {
    if (xhr && xhr.readyState !== 4) {
      xhr.abort()
      xhr = null
      uploading.value = false
      progress.value = 0
      console.log('[TOS上传] 已取消未完成的上传请求')
    }
  }

  // v1.1：增加 decodeURIComponent 处理
  function extractKeyFromUrl(url: string): string {
    const urlObj = new URL(url)
    return decodeURIComponent(urlObj.pathname.substring(1))
  }

  return { uploading, progress, upload, abort }
}

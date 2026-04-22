// composables/useTosUpload.ts — 系分第 8.3 节：TOS 预签名直传

import { ref } from 'vue'
import { tosApi } from '@/api/tos'
import type { PresignResult } from '@/types'
import { ElMessage } from 'element-plus'

export interface UploadOptions {
  projectId: number
  fileType: 'novel' | 'asset' | 'other'
  projectDir?: string
  maxFileSize?: number       // 默认 50MB
  allowedTypes?: string[]    // 默认 ['image/png', 'image/jpeg', 'video/mp4', 'text/plain']
  onSuccess?: (accessUrl: string) => void
  onError?: (error: Error) => void
}

export function useTosUpload() {
  const uploading = ref(false)
  const progress = ref(0)

  // 火山 TOS 预签名 URL 上传：直传文件到对象存储，不经过后端
  // 流程：请求预签名 URL → PUT 上传文件 → 返回文件 key 给后端保存
  async function upload(file: File, options: UploadOptions): Promise<string> {
    // 1. 校验文件大小
    const maxFileSize = options.maxFileSize || 50 * 1024 * 1024
    if (file.size > maxFileSize) {
      ElMessage.error(`文件大小不能超过 ${maxFileSize / 1024 / 1024}MB`)
      throw new Error('File too large')
    }

    // 2. 校验文件类型
    const allowedTypes = options.allowedTypes || ['image/png', 'image/jpeg', 'video/mp4', 'text/plain']
    if (!allowedTypes.includes(file.type)) {
      ElMessage.error(`不支持的文件类型: ${file.type}`)
      throw new Error('Unsupported file type')
    }

    uploading.value = true
    progress.value = 0

    try {
      // 3. 获取预签名 URL
      const presignRes = await tosApi.presign({
        fileName: file.name,
        contentType: file.type,
        source: 'frontend',
        businessId: options.projectId
      })
      const presignResult = presignRes.data

      // 4. 直传 TOS
      await uploadToTos(presignResult.uploadUrl, file)

      // 5. 通知后端上传完成（v1.1：complete 失败仅重试 1 次）
      try {
        await tosApi.complete({
          fileKey: presignResult.fileKey,
          businessId: options.projectId,
          fileSize: file.size,
          originalName: file.name
        })
      } catch (completeError) {
        // v1.1：仅重试 1 次，不搞递增延迟、不上报监控
        try {
          await tosApi.complete({
            fileKey: presignResult.fileKey,
            businessId: options.projectId,
            fileSize: file.size,
            originalName: file.name
          })
        } catch (e2) {
          ElMessage.error('文件已上传，但记录保存失败，请刷新后重试')
          throw e2
        }
      }

      options.onSuccess?.(presignResult.accessUrl)
      return presignResult.accessUrl
    } catch (error: any) {
      // 预签名过期 → 触发 retry-presign action，递归重试一次
      if (error.code === 40005 || error.response?.data?.code === 40005) {
        return upload(file, options)
      }
      options.onError?.(error)
      throw error
    } finally {
      uploading.value = false
      progress.value = 0
    }
  }

  // XHR 直传 TOS（支持进度回调）
  async function uploadToTos(url: string, file: File): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', url)
      xhr.setRequestHeader('Content-Type', file.type)

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          progress.value = Math.round((e.loaded / e.total) * 100)
        }
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`TOS upload failed: ${xhr.status}`))
        }
      }

      xhr.onerror = () => reject(new Error('TOS upload network error'))
      xhr.send(file)
    })
  }

  // v1.1：增加 decodeURIComponent 处理
  function extractKeyFromUrl(url: string): string {
    const urlObj = new URL(url)
    return decodeURIComponent(urlObj.pathname.substring(1))
  }

  return { uploading, progress, upload }
}

// types/tos.ts — 系分第 6.2 节：TOS 存储类型定义

// 预签名请求
export interface PresignRequest {
  fileName: string
  contentType: string
  projectDir?: string
}

// 预签名结果
export interface PresignResult {
  uploadUrl: string
  accessUrl: string
  expiresIn: number
  maxFileSize: number
  allowedContentTypes: string[]
}

// TOS 上传完成请求
export interface TosCompleteRequest {
  key: string
  projectId: number
  fileType: 'novel' | 'asset' | 'other'
  metadata?: {
    originalName: string
    size: number
  }
}

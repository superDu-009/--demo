// types/tos.ts — 系分第 6.2 节：TOS 存储类型定义

// 预签名请求（符合后端swagger定义）
export interface PresignRequest {
  fileName: string
  contentType: string
  source: string
  businessId?: string
}

// 预签名结果
export interface PresignResult {
  uploadUrl: string
  fileKey: string
  expireSeconds: number
}

// TOS 上传完成请求（符合后端swagger定义）
export interface TosCompleteRequest {
  fileKey: string
  source: string
}

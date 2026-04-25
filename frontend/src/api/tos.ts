// api/tos.ts — 系分第 5.2 节：TOS 存储接口

import request from '@/api'
import type { PresignRequest, PresignResult, TosCompleteRequest, ApiResponse } from '@/types'

export const tosApi = {
  // 获取预签名 URL：POST /api/storage/tos/presign
  presign: (data: PresignRequest) =>
    request.post<never, ApiResponse<PresignResult>>('/storage/tos/presign', data),

  // 通知后端上传完成：POST /api/storage/tos/complete
  complete: (data: TosCompleteRequest) =>
    request.post('/storage/tos/complete', data)
}

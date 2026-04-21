// api/asset.ts — 系分第 5.2 节：资产模块接口

import request from '@/api'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO, ShotReferenceVO, ApiResponse, PageResult } from '@/types'

export const assetApi = {
  // 获取项目资产列表：GET /api/project/{projectId}/assets
  list: (projectId: number, assetType?: string) =>
    request.get<never, ApiResponse<AssetVO[]>>(`/project/${projectId}/assets`, {
      params: assetType ? { assetType } : undefined
    }),

  // 创建资产：POST /api/project/{projectId}/assets
  create: (projectId: number, data: AssetCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/assets`, data),

  // 更新资产：PUT /api/asset/{id}
  update: (id: number, data: AssetUpdateRequest) =>
    request.put(`/asset/${id}`, data),

  // 删除资产：DELETE /api/asset/{id}
  delete: (id: number) =>
    request.delete(`/asset/${id}`),

  // 确认资产：PUT /api/asset/{id}/confirm
  confirm: (id: number) =>
    request.put(`/asset/${id}/confirm`),

  // 获取资产引用（删除前检查）：GET /api/asset/{assetId}/references
  getReferences: (assetId: number, params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotReferenceVO>>>(`/asset/${assetId}/references`, { params })
}

// api/asset.ts — 系分第 5.2 节：资产模块接口

import request from '@/api'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO, ShotReferenceVO, ApiResponse, PageResult } from '@/types'

type BackendAssetVO = Omit<AssetVO, 'referenceImages' | 'stylePreset'> & {
  referenceImages: string | string[] | null
  stylePreset: string | Record<string, any> | null
}

const parseJsonField = <T>(value: unknown, fallback: T): T => {
  if (value === null || value === undefined || value === '') return fallback
  if (typeof value !== 'string') return value as T
  try {
    return JSON.parse(value) as T
  } catch {
    return fallback
  }
}

const normalizeAsset = (asset: BackendAssetVO): AssetVO => ({
  ...asset,
  referenceImages: parseJsonField<string[]>(asset.referenceImages, []),
  stylePreset: parseJsonField<Record<string, any> | null>(asset.stylePreset, null)
})

const serializeAssetPayload = <T extends AssetCreateRequest | AssetUpdateRequest>(data: T) => ({
  ...data,
  referenceImages: Array.isArray(data.referenceImages)
    ? JSON.stringify(data.referenceImages)
    : data.referenceImages,
  stylePreset: data.stylePreset && typeof data.stylePreset !== 'string'
    ? JSON.stringify(data.stylePreset)
    : data.stylePreset
})

export const assetApi = {
  // 获取项目资产列表：GET /api/project/{projectId}/assets
  list: async (projectId: number, params?: { assetType?: string; keyword?: string; page?: number; size?: number }) => {
    const res = await request.get<never, ApiResponse<BackendAssetVO[]>>(`/project/${projectId}/assets`, {
      params: params?.assetType ? { assetType: params.assetType } : undefined
    })
    const keyword = params?.keyword?.trim()
    const normalized = res.data.map(normalizeAsset)
    const filtered = keyword
      ? normalized.filter(asset => asset.name.includes(keyword) || (asset.description || '').includes(keyword))
      : normalized
    const page = params?.page || 1
    const size = params?.size || filtered.length || 12
    const start = (page - 1) * size
    return {
      ...res,
      data: {
        total: filtered.length,
        page,
        size,
        hasNext: start + size < filtered.length,
        list: filtered.slice(start, start + size)
      } as PageResult<AssetVO>
    }
  },

  // 创建资产：POST /api/project/{projectId}/assets
  create: (projectId: number, data: AssetCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/assets`, serializeAssetPayload(data)),

  // 更新资产：PUT /api/asset/{id}
  update: (id: number, data: AssetUpdateRequest) =>
    request.put<number, ApiResponse<number>>(`/asset/${id}`, serializeAssetPayload(data)),

  // 删除资产：DELETE /api/asset/{id}
  delete: (id: number) =>
    request.delete(`/asset/${id}`),

  // 确认资产：PUT /api/asset/{id}/confirm
  confirm: (id: number) =>
    request.put(`/asset/${id}/confirm`),

  // 获取资产引用（详细列表）：GET /api/asset/{assetId}/references
  getReferences: (assetId: number, params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotReferenceVO>>>(`/asset/${assetId}/references`, { params })
}

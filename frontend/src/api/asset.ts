import request from '@/api'
import type {
  ApiResponse,
  AssetCreateRequest,
  AssetUpdateRequest,
  AssetVO,
  GenerateTaskResponse,
  PageResult,
  ShotReferenceVO
} from '@/types'

type BackendAssetVO = Omit<AssetVO, 'referenceImages' | 'stylePreset'> & {
  referenceImages: string | string[] | null
  parentIds?: string | number[] | null
  draftContent?: string | null
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
  parentIds: parseJsonField<number[]>(asset.parentIds, []),
  draftContent: asset.draftContent || '',
  stylePreset: parseJsonField<Record<string, any> | null>(asset.stylePreset, null)
})

const serializeAssetPayload = <T extends AssetCreateRequest | AssetUpdateRequest>(data: T) => ({
  ...data,
  referenceImages: Array.isArray(data.referenceImages)
    ? JSON.stringify(data.referenceImages)
    : data.referenceImages,
  parentIds: Array.isArray(data.parentIds)
    ? JSON.stringify(data.parentIds)
    : data.parentIds,
  stylePreset: data.stylePreset && typeof data.stylePreset !== 'string'
    ? JSON.stringify(data.stylePreset)
    : data.stylePreset
})

export const assetApi = {
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

  update: (id: number, data: AssetUpdateRequest) =>
    request.put<number, ApiResponse<number>>(`/asset/${id}`, serializeAssetPayload(data)),

  delete: (id: number) =>
    request.delete(`/asset/${id}`),

  confirm: (id: number) =>
    request.put(`/asset/${id}/confirm`),

  generateImage: (id: number) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/asset/${id}/image/generate`),

  extract: (projectId: number, data: { episodeIds: number[] }) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/project/${projectId}/assets/extract`, data),

  getExtractStatus: (projectId: number) =>
    request.get<never, ApiResponse<{ taskId?: number | null; status?: string; errorMsg?: string | null }>>(`/project/${projectId}/assets/extract/status`),

  getDuplicates: (projectId: number) =>
    request.get<never, ApiResponse<Array<{ assetIds: number[]; score: number }>>>(`/project/${projectId}/assets/duplicates`),

  getTree: (projectId: number) =>
    request.get<never, ApiResponse<Array<{ id: number; parentIds: number[]; childIds: number[] }>>>(`/project/${projectId}/assets/tree`),

  updateRelations: (id: number, data: { parentIds: number[] }) =>
    request.put(`/asset/${id}/relations`, data),

  getReferences: (assetId: number, params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotReferenceVO>>>(`/asset/${assetId}/references`, { params })
}

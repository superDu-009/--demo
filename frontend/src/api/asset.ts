import request from '@/api'
import type {
  ApiResponse,
  AssetDuplicateVO,
  AssetCreateRequest,
  AssetTreeNode,
  AssetUpdateRequest,
  AssetVO,
  PageResult,
  ShotReferenceVO
} from '@/types'

type BackendAssetVO = Omit<AssetVO, 'referenceImages'> & {
  referenceImages: string | string[] | null
  parentIds?: string | number[] | null
  draftContent?: string | null
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
  draftContent: asset.draftContent || ''
})

const serializeAssetPayload = <T extends AssetCreateRequest | AssetUpdateRequest>(data: T) => ({
  ...data,
  referenceImages: Array.isArray(data.referenceImages)
    ? JSON.stringify(data.referenceImages)
    : data.referenceImages,
  parentIds: Array.isArray(data.parentIds)
    ? JSON.stringify(data.parentIds)
    : data.parentIds
})

const flattenTree = (
  nodes: AssetTreeNode[],
  parentId?: number,
  result: Array<{ id: number; parentIds: number[]; childIds: number[] }> = []
) => {
  nodes.forEach((node) => {
    const childIds = (node.children || []).map(child => child.id)
    result.push({
      id: node.id,
      parentIds: parentId ? [parentId] : [],
      childIds
    })
    flattenTree(node.children || [], node.id, result)
  })
  return result
}

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
    request.post<never, ApiResponse<number>>(`/asset/${id}/image/generate`),

  extract: (projectId: number, data: { episodeIds: number[] }) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/assets/extract`, undefined, {
      params: { episodeId: data.episodeIds[0] }
    }),

  getExtractStatus: (projectId: number) =>
    request.get<never, ApiResponse<{ id?: number | null; status?: number | null; errorMsg?: string | null }>>(`/project/${projectId}/assets/extract/status`),

  getDuplicates: (projectId: number) =>
    request.get<never, ApiResponse<AssetDuplicateVO[]>>(`/project/${projectId}/assets/duplicates`),

  getTree: async (projectId: number) => {
    const res = await request.get<never, ApiResponse<AssetTreeNode[]>>(`/project/${projectId}/assets/tree`)
    return {
      ...res,
      data: flattenTree(res.data || [])
    }
  },

  updateRelations: (id: number, data: { parentIds: number[] }) =>
    request.put(`/asset/${id}/relations`, undefined, {
      params: { parentIds: JSON.stringify(data.parentIds || []) }
    }),

  getReferences: (assetId: number, params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotReferenceVO>>>(`/asset/${assetId}/references`, { params })
}

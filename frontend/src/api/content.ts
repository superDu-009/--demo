// api/content.ts — 系分第 5.2 节：内容模块接口（分集/分场/分镜）

import request from '@/api'
import type {
  EpisodeCreateRequest, SceneCreateRequest, ShotCreateRequest,
  ShotUpdateRequest, BatchReviewRequest, BatchReviewResult,
  EpisodeVO, SceneVO, ShotVO, ApiResponse, PageResult
} from '@/types'

export const contentApi = {
  // === 分集 ===
  // 获取分集列表：GET /api/project/{projectId}/episodes
  listEpisodes: (projectId: number) =>
    request.get<never, ApiResponse<EpisodeVO[]>>(`/project/${projectId}/episodes`),

  // 创建分集：POST /api/project/{projectId}/episodes
  createEpisode: (projectId: number, data: EpisodeCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/episodes`, data),

  // 更新分集：PUT /api/episode/{id}
  updateEpisode: (id: number, data: Partial<EpisodeCreateRequest>) =>
    request.put(`/episode/${id}`, data),

  // 删除分集：DELETE /api/episode/{id}
  deleteEpisode: (id: number) =>
    request.delete(`/episode/${id}`),

  // === 分场 ===
  // 获取分场列表：GET /api/episode/{episodeId}/scenes
  listScenes: (episodeId: number) =>
    request.get<never, ApiResponse<SceneVO[]>>(`/episode/${episodeId}/scenes`),

  // 创建分场：POST /api/episode/{episodeId}/scenes
  createScene: (episodeId: number, data: SceneCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/episode/${episodeId}/scenes`, data),

  // 更新分场：PUT /api/scene/{id}
  updateScene: (id: number, data: Partial<SceneCreateRequest>) =>
    request.put(`/scene/${id}`, data),

  // 删除分场：DELETE /api/scene/{id}
  deleteScene: (id: number) =>
    request.delete(`/scene/${id}`),

  // === 分镜 ===
  // 获取分镜列表（分页）：GET /api/scene/{sceneId}/shots
  listShots: (sceneId: number, params?: { page?: number; size?: number; status?: number }) =>
    request.get<never, ApiResponse<PageResult<ShotVO>>>(`/scene/${sceneId}/shots`, { params }),

  // 创建分镜：POST /api/scene/{sceneId}/shots
  createShot: (sceneId: number, data: ShotCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/scene/${sceneId}/shots`, data),

  // 更新分镜：PUT /api/shot/{id}
  updateShot: (id: number, data: ShotUpdateRequest) =>
    request.put(`/shot/${id}`, data),

  // 删除分镜：DELETE /api/shot/{id}
  deleteShot: (id: number) =>
    request.delete(`/shot/${id}`),

  // 批量审核：POST /api/shot/batch-review
  batchReview: (data: BatchReviewRequest) =>
    request.post<never, ApiResponse<BatchReviewResult>>('/shot/batch-review', data),

  // 绑定资产：POST /api/shot/{shotId}/assets
  bindAsset: (shotId: number, data: { assetId: number; assetType: string }) =>
    request.post(`/shot/${shotId}/assets`, null, { params: data }),

  // 解绑资产：DELETE /api/shot/{shotId}/assets/{assetId}
  unbindAsset: (shotId: number, assetId: number) =>
    request.delete(`/shot/${shotId}/assets/${assetId}`)
}

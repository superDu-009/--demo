import request from '@/api'
import type {
  ApiResponse,
  BatchGenerateRequest,
  BatchGenerateResponse,
  EpisodeAnalyzeRequest,
  EpisodeAnalyzeStatusVO,
  EpisodeCreateRequest,
  EpisodeVO,
  GeneratePayload,
  GenerateTaskResponse,
  ShotCreateRequest,
  ShotDraftRequest,
  ShotSplitRequest,
  ShotUpdateRequest,
  ShotVO
} from '@/types'

const normalizeShotPayload = (data: ShotCreateRequest | ShotUpdateRequest) => ({
  ...data,
  followLast: typeof data.followLast === 'boolean' ? Number(data.followLast) : data.followLast
})

export const contentApi = {
  listEpisodes: (projectId: number) =>
    request.get<never, ApiResponse<EpisodeVO[]>>(`/project/${projectId}/episodes`),

  analyzeEpisodes: (projectId: number, data: EpisodeAnalyzeRequest) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/project/${projectId}/episodes/analyze`, data),

  getAnalyzeStatus: (projectId: number) =>
    request.get<never, ApiResponse<EpisodeAnalyzeStatusVO>>(`/project/${projectId}/episodes/analyze/status`),

  createEpisode: (projectId: number, data: EpisodeCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/episodes`, data),

  updateEpisode: (id: number, data: EpisodeCreateRequest) =>
    request.put<number, ApiResponse<number>>(`/episode/${id}`, data),

  deleteEpisode: (id: number) =>
    request.delete(`/episode/${id}`),

  listShots: (episodeId: number, params?: { promptStatus?: string; imageStatus?: string; videoStatus?: string }) =>
    request.get<never, ApiResponse<ShotVO[]>>(`/episode/${episodeId}/shots`, { params }),

  splitShots: (episodeId: number, data: ShotSplitRequest) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/episode/${episodeId}/shots/split`, data),

  createShot: (episodeId: number, data: ShotCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/episode/${episodeId}/shots`, normalizeShotPayload(data)),

  updateShot: (id: number, data: ShotUpdateRequest) =>
    request.put<number, ApiResponse<number>>(`/shot/${id}`, normalizeShotPayload(data)),

  deleteShot: (id: number) =>
    request.delete(`/shot/${id}`),

  sortShot: (id: number, sortOrder: number) =>
    request.put(`/shot/${id}/sort`, { sortOrder }),

  saveDraft: (id: number, data: ShotDraftRequest) =>
    request.post<number, ApiResponse<number>>(`/shot/${id}/draft`, data),

  generatePrompt: (id: number) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/shot/${id}/prompt/generate`),

  generateImage: (id: number, data: GeneratePayload) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/shot/${id}/image/generate`, data),

  generateVideo: (id: number, data: GeneratePayload) =>
    request.post<never, ApiResponse<GenerateTaskResponse>>(`/shot/${id}/video/generate`, data),

  batchGeneratePrompt: (episodeId: number, data: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/prompt`, data),

  batchGenerateImage: (episodeId: number, data: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/image`, data),

  batchGenerateVideo: (episodeId: number, data: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/video`, data)
}

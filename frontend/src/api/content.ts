import request from '@/api'
import type {
  ApiResponse,
  BatchGenerateRequest,
  BatchGenerateResponse,
  EpisodeAnalyzeStatusVO,
  EpisodeCreateRequest,
  EpisodeVO,
  ShotCreateRequest,
  ShotSplitRequest,
  ShotUpdateRequest,
  ShotVO
} from '@/types'

const normalizeLines = (lines: ShotCreateRequest['lines'] | string | null | undefined) => {
  if (Array.isArray(lines)) return lines
  if (typeof lines !== 'string') return lines
  return lines
    .split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean)
}

const normalizeShotPayload = (data: ShotCreateRequest | ShotUpdateRequest) => ({
  ...data,
  lines: normalizeLines(data.lines as ShotCreateRequest['lines'] | string | null | undefined),
  followLast: typeof data.followLast === 'boolean' ? Number(data.followLast) : data.followLast
})

export const contentApi = {
  listEpisodes: (projectId: number) =>
    request.get<never, ApiResponse<EpisodeVO[]>>(`/project/${projectId}/episodes`),

  analyzeEpisodes: (projectId: number) =>
    request.post<never, ApiResponse<number>>(`/project/${projectId}/episodes/analyze`),

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
    request.post<never, ApiResponse<number>>(`/episode/${episodeId}/shots/split`, undefined, {
      params: { duration: data.durationSeconds }
    }),

  createShot: (episodeId: number, data: ShotCreateRequest) =>
    request.post<never, ApiResponse<number>>(`/episode/${episodeId}/shots`, normalizeShotPayload(data)),

  updateShot: (id: number, data: ShotUpdateRequest) =>
    request.put<number, ApiResponse<number>>(`/shot/${id}`, normalizeShotPayload(data)),

  deleteShot: (id: number) =>
    request.delete(`/shot/${id}`),

  sortShot: (id: number, sortOrder: number) =>
    request.put(`/shot/${id}/sort`, undefined, { params: { sortOrder } }),

  saveDraft: (id: number, draftContent: string) =>
    request.post<string, ApiResponse<void>>(`/shot/${id}/draft`, draftContent, {
      headers: { 'Content-Type': 'text/plain' }
    }),

  generatePrompt: (id: number) =>
    request.post<never, ApiResponse<number>>(`/shot/${id}/prompt/generate`),

  generateImage: (id: number) =>
    request.post<never, ApiResponse<number>>(`/shot/${id}/image/generate`),

  generateVideo: (id: number) =>
    request.post<never, ApiResponse<number>>(`/shot/${id}/video/generate`),

  batchGeneratePrompt: (episodeId: number, _data?: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/prompt`),

  batchGenerateImage: (episodeId: number, _data?: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/image`),

  batchGenerateVideo: (episodeId: number, _data?: BatchGenerateRequest) =>
    request.post<never, ApiResponse<BatchGenerateResponse>>(`/episode/${episodeId}/shots/batch/video`)
}

// types/content.ts — 内容类型定义（项目 → 分集 → 分镜）

import type { GenerationStatus, ParseStatus } from './common'
import type { ProjectDefinition, ProjectRatio } from './project'

export interface EpisodeCreateRequest {
  title: string
  content?: string
  sortOrder?: number
}

export interface EpisodeVO {
  id: number
  projectId: number
  title: string
  sortOrder: number
  content: string | null
  parseStatus?: ParseStatus | number
  parseError?: string | null
  assetIds?: number[]
  createTime: string
  updateTime: string
}

export interface EpisodeAnalyzeStatusVO {
  taskId?: number | null
  parseStatus: ParseStatus
  parseError?: string | null
}

export interface ShotSplitRequest {
  durationSeconds: 10 | 12 | 15
}

export interface ShotAssetRef {
  assetId: number
  assetType: string
  assetName: string
  primaryImage: string | null
}

export interface ShotVO {
  id: number
  episodeId: number
  sortOrder: number
  prompt: string | null
  promptEn: string | null
  generatedImageUrl: string | null
  generatedVideoUrl: string | null
  lastFrameUrl?: string | null
  sceneType?: string | null
  cameraMove?: string | null
  lines?: string[] | string | null
  duration?: number | null
  followLast?: number | boolean | null
  draftContent?: string | null
  promptStatus?: GenerationStatus | number
  imageStatus?: GenerationStatus | number
  videoStatus?: GenerationStatus | number
  errorMsg?: string | null
  assetRefs: ShotAssetRef[]
  assetIds?: number[]
  createTime?: string
  updateTime?: string
}

export interface ShotCreateRequest {
  episodeId?: number
  sortOrder?: number
  prompt?: string
  promptEn?: string
  sceneType?: string | null
  cameraMove?: string | null
  lines?: string[] | string | null
  duration?: number | null
  followLast?: boolean
  draftContent?: string | null
  assetIds?: number[]
}

export interface ShotUpdateRequest extends ShotCreateRequest {
  generatedImageUrl?: string | null
  generatedVideoUrl?: string | null
  lastFrameUrl?: string | null
}

export interface BatchGenerateRequest {
  shotIds: number[]
}

export interface BatchGenerateResponse {
  batchId: string
  taskIds: number[]
}

export interface GeneratePayload {
  ratio: ProjectRatio
  definition: ProjectDefinition
}

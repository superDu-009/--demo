// types/content.ts — 系分第 6.2 节：内容类型定义（分集/分场/分镜）

import type { ShotStatus, AiTaskStatus } from './common'

// 创建分集请求
export interface EpisodeCreateRequest {
  title: string
  content?: string
  sortOrder?: number
}

// 分集 VO
export interface EpisodeVO {
  id: number
  projectId: number
  title: string
  sortOrder: number
  content: string | null
  status: number
  createTime: string
  updateTime: string
  // 前端扩展字段
  sceneCount?: number
  shotStats?: { total: number; approved: number; rejected: number }
}

// 创建分场请求
export interface SceneCreateRequest {
  title: string
  content?: string
  sortOrder?: number
}

// 分场 VO
export interface SceneVO {
  id: number
  episodeId: number
  title: string
  sortOrder: number
  content: string | null
  status: number
  createTime: string
  updateTime: string
  // 前端扩展字段
  shotCount?: number
  shotStats?: { total: number; approved: number; rejected: number }
}

// 分镜资产引用
export interface ShotAssetRef {
  assetId: number
  assetType: string
  assetName: string
  primaryImage: string
}

// 分镜 VO
export interface ShotVO {
  id: number
  sceneId: number
  sortOrder: number
  prompt: string | null
  promptEn: string | null
  generatedImageUrl: string | null
  generatedVideoUrl: string | null
  status: ShotStatus
  reviewComment: string | null
  version: number
  generationAttempts: number
  assetRefs: ShotAssetRef[]
  currentAiTask: CurrentAiTask | null
  createTime: string
  updateTime: string
}

// 当前 AI 任务
export interface CurrentAiTask {
  taskId: number
  taskType: 'image_gen' | 'video_gen'
  status: AiTaskStatus
}

// 创建分镜请求
export interface ShotCreateRequest {
  sortOrder?: number
  prompt?: string
  promptEn?: string
}

// 更新分镜请求
export interface ShotUpdateRequest {
  prompt?: string
  promptEn?: string
  status?: ShotStatus
  reviewComment?: string
}

// 批量审核请求
export interface BatchReviewRequest {
  shotIds: number[]
  action: 'approve' | 'reject'
  comment?: string
}

// 批量审核结果
export interface BatchReviewResult {
  totalCount: number
  successCount: number
  failedCount: number
  failedDetails: { shotId: number; reason: string }[]
}

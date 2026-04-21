// types/asset.ts — 系分第 6.2 节：资产类型定义

import type { AssetStatus, AssetType } from './common'

// 创建资产请求
export interface AssetCreateRequest {
  assetType: AssetType
  name: string
  description?: string
  referenceImages?: string[]
  stylePreset?: Record<string, any>
}

// 更新资产请求
export interface AssetUpdateRequest {
  name?: string
  description?: string
  referenceImages?: string[]
  stylePreset?: Record<string, any>
}

// 资产 VO
export interface AssetVO {
  id: number
  projectId: number
  assetType: AssetType
  name: string
  description: string | null
  referenceImages: string[] | null  // 第一个为主图
  stylePreset: Record<string, any> | null
  status: AssetStatus
  createTime: string
  updateTime: string
}

// 分镜引用 VO
export interface ShotReferenceVO {
  shotId: number
  sceneId: number
  episodeId: number
  shotStatus: number
}

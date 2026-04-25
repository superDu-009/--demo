// types/asset.ts — 资产类型定义

import type { AssetStatus, AssetType } from './common'

export interface AssetCreateRequest {
  assetType: AssetType
  name: string
  description?: string
  referenceImages?: string[]
  parentIds?: number[]
  draftContent?: string
  stylePreset?: Record<string, any> | null
}

export interface AssetUpdateRequest {
  name?: string
  description?: string
  referenceImages?: string[]
  parentIds?: number[]
  draftContent?: string
  stylePreset?: Record<string, any> | null
}

export interface AssetVO {
  id: number
  projectId: number
  assetType: AssetType
  name: string
  description: string | null
  referenceImages: string[] | null
  parentIds?: number[]
  draftContent?: string | null
  stylePreset: Record<string, any> | null
  status: AssetStatus
  createTime: string
  updateTime: string
}

export interface ShotReferenceVO {
  shotId: number
  episodeId: number
  shotStatus: number | string
}

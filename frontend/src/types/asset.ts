// types/asset.ts — 资产类型定义

import type { AssetStatus, AssetType } from './common'

export interface AssetCreateRequest {
  assetType: AssetType
  name: string
  description?: string
  referenceImages?: string[]
  parentIds?: number[]
  draftContent?: string
}

export interface AssetUpdateRequest {
  name?: string
  description?: string
  referenceImages?: string[]
  parentIds?: number[]
  draftContent?: string
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
  status: AssetStatus
  createTime: string
  updateTime: string
}

export interface ShotReferenceVO {
  shotId: number
  episodeId: number
  shotStatus: number | string
}

export interface AssetDuplicateVO {
  assetIds: number[]
  assetNames: string[]
  similarity: number
}

export interface AssetTreeNode {
  id: number
  name: string
  assetType: AssetType | string
  isSubAsset: boolean
  children: AssetTreeNode[]
}

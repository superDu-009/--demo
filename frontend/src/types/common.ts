// types/common.ts — 通用类型与统一枚举

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T = any> {
  total: number
  page: number
  size: number
  hasNext: boolean
  list: T[]
  records?: T[]
}

export enum AssetStatus {
  Draft = 'draft',
  Confirmed = 'confirmed',
  Deprecated = 'deprecated'
}

export enum AssetType {
  Character = 'character',
  Scene = 'scene',
  Prop = 'prop',
  Voice = 'voice'
}

export enum ParseStatus {
  Pending = 'pending',
  Processing = 'analyzing',
  Success = 'success',
  Failed = 'failed'
}

export enum GenerationStatus {
  Pending = 'pending',
  Processing = 'generating',
  Success = 'success',
  Failed = 'failed'
}

export enum AiTaskStatus {
  Pending = 0,
  Processing = 1,
  Success = 2,
  Failed = 3
}

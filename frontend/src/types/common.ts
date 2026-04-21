// types/common.ts — 系分第 6.1 节：通用类型定义

// 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应
export interface PageResult<T = any> {
  total: number
  page: number
  size: number
  hasNext: boolean
  list: T[]
}

// 项目状态枚举
export enum ProjectStatus {
  Draft = 0,       // 草稿
  InProgress = 1,  // 进行中
  Completed = 2    // 已完成
}

// 资产状态枚举
export enum AssetStatus {
  Draft = 0,       // 草稿
  Confirmed = 1,   // 已确认
  Deprecated = 2   // 已废弃
}

// 资产类型枚举
export enum AssetType {
  Character = 'character',
  Scene = 'scene',
  Prop = 'prop',
  Voice = 'voice'
}

// 分集状态枚举
export enum EpisodeStatus {
  Pending = 0,
  InProgress = 1,
  Completed = 2
}

// 分镜状态枚举
export enum ShotStatus {
  Pending = 0,       // 待处理
  Generating = 1,    // 生成中
  WaitingReview = 2, // 待审核
  Approved = 3,      // 已通过
  Rejected = 4,      // 已打回
  Completed = 5      // 已完成
}

// 工作流任务状态枚举
export enum WorkflowTaskStatus {
  NotStarted = 0,    // 未执行
  Running = 1,       // 执行中
  Success = 2,       // 成功
  Failed = 3,        // 失败
  WaitingReview = 4  // 待审核
}

// AI 任务状态枚举
export enum AiTaskStatus {
  Submitting = 0,    // 排队中
  Processing = 1,    // 处理中
  Success = 2,       // 成功
  Failed = 3         // 失败
}

// 流程步骤类型
export type StepType =
  | 'import'
  | 'asset_extract'
  | 'shot_gen'
  | 'image_gen'
  | 'video_gen'
  | 'export'

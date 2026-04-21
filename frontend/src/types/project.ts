// types/project.ts — 系分第 6.2 节：项目类型定义

import type { ProjectStatus, StepType } from './common'

// 创建项目请求
export interface ProjectCreateRequest {
  name: string
  description?: string
  novelTosPath?: string
}

// 更新项目请求
export interface ProjectUpdateRequest {
  name?: string
  description?: string
}

// 项目 VO
export interface ProjectVO {
  id: number
  userId: number
  name: string
  description: string | null
  novelTosPath: string | null
  workflowConfig: WorkflowConfig | null
  stylePreset: StylePreset | null
  status: ProjectStatus
  executionLock: number
  version: number
  createTime: string
  updateTime: string
}

// 工作流配置
export interface WorkflowConfig {
  steps: WorkflowStep[]
}

// 工作流步骤
export interface WorkflowStep {
  stepType: StepType
  enabled: boolean
  review: boolean
  config?: Record<string, any>
}

// 风格预设
export interface StylePreset {
  artStyle?: string
  colorTone?: string
}

// 保存工作流配置请求
export interface WorkflowConfigRequest {
  version: number
  workflowConfig: WorkflowConfig
  stylePreset?: StylePreset
}

// 工作流状态 VO
export interface WorkflowStatusVO {
  executionLock: number
  currentStep: string | null
  currentEpisodeId: number | null
  currentEpisodeTitle: string | null
  totalEpisodes: number
  overallProgress: number
  totalShots: number
  processedShots: number
  estimatedRemainingSeconds: number
  steps: WorkflowStepStatus[]
}

// 工作流步骤状态
export interface WorkflowStepStatus {
  stepType: string
  stepOrder: number
  status: number
  progress: number
  currentDetail: string
  errorMsg: string | null
  reviewComment: string | null
}

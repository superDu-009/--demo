// types/project.ts — 项目类型定义

export type ProjectRatio = '16:8' | '16:9' | '4:3' | '9:16'
export type ProjectDefinition = '720P' | '1080P' | '2K' | '4K'
export type ProjectStyle = '2D次元风' | '日漫风' | '国漫风' | '古风' | '现代写实' | '自定义'

export interface ProjectCreateRequest {
  name: string
  description?: string
  novelFile?: {
    fileName: string
    fileKey: string
    fileSize: number
  }
  novelTosPath?: string
  ratio?: ProjectRatio
  definition?: ProjectDefinition
  style?: ProjectStyle
  styleDesc?: string
}

export interface ProjectUpdateRequest {
  name?: string
  description?: string
}

export interface ProjectVO {
  id: number
  name: string
  description: string | null
  novelOriginalTosPath?: string | null
  novelTosPath: string | null
  ratio?: ProjectRatio | null
  definition?: ProjectDefinition | null
  style?: ProjectStyle | null
  styleDesc?: string | null
  createTime: string
  updateTime: string
}

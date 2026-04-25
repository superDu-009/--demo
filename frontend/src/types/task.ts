import type { AiTaskStatus } from './common'

export interface TaskVO {
  id: number
  type: 'script_analyze' | 'shot_split' | 'asset_extract' | 'prompt_gen' | 'image_gen' | 'video_gen'
  status: AiTaskStatus
  progress?: number | null
  errorMsg?: string | null
  resultData?: Record<string, any> | null
  resultUrl?: string | null
  batchId?: string | null
  createTime?: string
  updateTime?: string
}

export interface BatchTaskStatusRequest {
  taskIds?: number[]
  batchId?: string
}

export interface BatchTaskStatusResponse {
  batchId?: string | null
  tasks: TaskVO[]
}

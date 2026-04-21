// constants/status.ts — 系分第 9 节：状态颜色映射

// 枚举需要作为值使用，不能用 import type
import { ProjectStatus, AssetStatus, ShotStatus, WorkflowTaskStatus, AiTaskStatus } from '@/types'

// 项目状态映射
export const PROJECT_STATUS_MAP: Record<ProjectStatus, { label: string; type: 'info' | 'primary' | 'success' }> = {
  [ProjectStatus.Draft]: { label: '草稿', type: 'info' },
  [ProjectStatus.InProgress]: { label: '进行中', type: 'primary' },
  [ProjectStatus.Completed]: { label: '已完成', type: 'success' }
}

// 资产状态映射
export const ASSET_STATUS_MAP: Record<AssetStatus, { label: string; type: 'info' | 'success' | 'danger' }> = {
  [AssetStatus.Draft]: { label: '草稿', type: 'info' },
  [AssetStatus.Confirmed]: { label: '已确认', type: 'success' },
  [AssetStatus.Deprecated]: { label: '已废弃', type: 'danger' }
}

// 分镜状态映射（v1.1：移除 loading 图标）
export const SHOT_STATUS_MAP: Record<ShotStatus, { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger'; icon?: string }> = {
  [ShotStatus.Pending]: { label: '待处理', type: 'info' },
  [ShotStatus.Generating]: { label: '生成中', type: 'primary' },
  [ShotStatus.WaitingReview]: { label: '待审核', type: 'warning' },
  [ShotStatus.Approved]: { label: '已通过', type: 'success' },
  [ShotStatus.Rejected]: { label: '已打回', type: 'danger' },
  [ShotStatus.Completed]: { label: '已完成', type: 'success' }
}

// 工作流任务状态映射
export const WORKFLOW_TASK_STATUS_MAP: Record<WorkflowTaskStatus, { label: string; color: string }> = {
  [WorkflowTaskStatus.NotStarted]: { label: '未执行', color: '#909399' },
  [WorkflowTaskStatus.Running]: { label: '执行中', color: '#409EFF' },
  [WorkflowTaskStatus.Success]: { label: '成功', color: '#67C23A' },
  [WorkflowTaskStatus.Failed]: { label: '失败', color: '#F56C6C' },
  [WorkflowTaskStatus.WaitingReview]: { label: '待审核', color: '#E6A23C' }
}

// AI 任务状态映射（v1.1：Submitting → 排队中）
export const AI_TASK_STATUS_MAP: Record<AiTaskStatus, { label: string; type: 'info' | 'primary' | 'success' | 'danger' }> = {
  [AiTaskStatus.Submitting]: { label: '排队中', type: 'info' },
  [AiTaskStatus.Processing]: { label: '处理中', type: 'primary' },
  [AiTaskStatus.Success]: { label: '成功', type: 'success' },
  [AiTaskStatus.Failed]: { label: '失败', type: 'danger' }
}

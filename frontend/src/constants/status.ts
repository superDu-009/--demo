// constants/status.ts — 状态文案与按钮态映射

import { AiTaskStatus, AssetStatus, GenerationStatus, ParseStatus } from '@/types'

export const ASSET_STATUS_MAP: Record<AssetStatus, { label: string; type: 'info' | 'success' | 'danger' }> = {
  [AssetStatus.Draft]: { label: '未确认', type: 'info' },
  [AssetStatus.Confirmed]: { label: '已确认', type: 'success' },
  [AssetStatus.Deprecated]: { label: '已废弃', type: 'danger' }
}

export const PARSE_STATUS_MAP: Record<ParseStatus, { label: string; type: 'info' | 'primary' | 'success' | 'danger' }> = {
  [ParseStatus.Pending]: { label: '待解析', type: 'info' },
  [ParseStatus.Processing]: { label: '解析中', type: 'primary' },
  [ParseStatus.Success]: { label: '解析成功', type: 'success' },
  [ParseStatus.Failed]: { label: '解析失败', type: 'danger' }
}

export const GENERATION_STATUS_MAP: Record<GenerationStatus, { label: string; type: 'info' | 'primary' | 'success' | 'danger'; actionText: string }> = {
  [GenerationStatus.Pending]: { label: '未生成', type: 'info', actionText: '生成' },
  [GenerationStatus.Processing]: { label: '生成中', type: 'primary', actionText: '生成中' },
  [GenerationStatus.Success]: { label: '生成成功', type: 'success', actionText: '重新生成' },
  [GenerationStatus.Failed]: { label: '生成失败', type: 'danger', actionText: '重试' }
}

export const AI_TASK_STATUS_MAP: Record<AiTaskStatus, { label: string; type: 'info' | 'primary' | 'success' | 'danger' }> = {
  [AiTaskStatus.Pending]: { label: '排队中', type: 'info' },
  [AiTaskStatus.Processing]: { label: '处理中', type: 'primary' },
  [AiTaskStatus.Success]: { label: '成功', type: 'success' },
  [AiTaskStatus.Failed]: { label: '失败', type: 'danger' }
}

// constants/options.ts — 所有下拉/切换枚举统一收口

import type { AssetType, ProjectDefinition, ProjectRatio, ProjectStyle } from '@/types'

export const PROJECT_RATIO_OPTIONS: Array<{ label: ProjectRatio; value: ProjectRatio }> = [
  { label: '16:8', value: '16:8' },
  { label: '16:9', value: '16:9' },
  { label: '4:3', value: '4:3' },
  { label: '9:16', value: '9:16' }
]

export const PROJECT_DEFINITION_OPTIONS: Array<{ label: ProjectDefinition; value: ProjectDefinition }> = [
  { label: '720P', value: '720P' },
  { label: '1080P', value: '1080P' },
  { label: '2K', value: '2K' },
  { label: '4K', value: '4K' }
]

export const PROJECT_STYLE_OPTIONS: Array<{ label: ProjectStyle; value: ProjectStyle }> = [
  { label: '2D次元风', value: '2D次元风' },
  { label: '日漫风', value: '日漫风' },
  { label: '国漫风', value: '国漫风' },
  { label: '古风', value: '古风' },
  { label: '现代写实', value: '现代写实' },
  { label: '自定义', value: '自定义' }
]

export const ASSET_TYPE_OPTIONS: Array<{ label: string; value: AssetType }> = [
  { label: '角色', value: 'character' as AssetType },
  { label: '场景', value: 'scene' as AssetType },
  { label: '道具', value: 'prop' as AssetType },
  { label: '声音', value: 'voice' as AssetType }
]

export const SHOT_DURATION_OPTIONS = [
  { label: '10s', value: 10 },
  { label: '12s', value: 12 },
  { label: '15s', value: 15 }
]

export const SCRIPT_ANALYSIS_TEMPLATE_OPTIONS = [
  { label: '默认拆分规则', value: 'episode_split_default' },
  { label: '紧凑节奏规则', value: 'episode_split_compact' }
]

export const SHOT_SPLIT_TEMPLATE_OPTIONS = [
  { label: '10s 拆分', value: 'shot_split_10s' },
  { label: '12s 拆分', value: 'shot_split_12s' },
  { label: '15s 拆分', value: 'shot_split_15s' }
]

export const DUPLICATE_DECISION_OPTIONS = [
  { label: '合并', value: 'merge' },
  { label: '删除', value: 'delete' },
  { label: '保留', value: 'keep' }
]

// constants/workflow.ts — 系分第 9 节：流程步骤定义

// 流程步骤配置（标签、图标、描述、默认审核）
export const WORKFLOW_STEP_CONFIG: Record<string, {
  label: string
  icon: string
  description: string
  defaultReview: boolean
}> = {
  import: {
    label: '导入并拆分剧本',
    icon: 'Download',
    description: '读取小说文本，AI 自动拆分章节',
    defaultReview: false
  },
  asset_extract: {
    label: '资产提取',
    icon: 'Search',
    description: 'AI 分析剧本，提取角色/场景资产',
    defaultReview: true
  },
  shot_gen: {
    label: '分镜提示词生成',
    icon: 'EditPen',
    description: '根据分场+资产，生成中英文提示词',
    defaultReview: true
  },
  image_gen: {
    label: '首帧生图',
    icon: 'Picture',
    description: '根据提示词和资产参考图生成首帧',
    defaultReview: true
  },
  video_gen: {
    label: '视频生成',
    icon: 'VideoCamera',
    description: 'Seedance 模型根据首帧生成视频',
    defaultReview: true
  },
  export: {
    label: '合并导出',
    icon: 'Box',
    description: 'FFmpeg 合并所有分镜视频为最终作品',
    defaultReview: false
  }
}

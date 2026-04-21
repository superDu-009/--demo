// types/ai.ts — 系分第 6.2 节：AI 任务类型定义

import type { AiTaskStatus } from './common'

// AI 任务 VO
export interface AiTaskVO {
  id: number
  taskType: 'image_gen' | 'video_gen'
  status: AiTaskStatus
  resultUrl: string | null
  errorMsg: string | null
  createTime: string
  updateTime: string
}

// API 消耗报告
export interface ApiCostReport {
  totalCalls: number
  totalCost: number
  avgCostPerCall: number
  failRate: number
  dailyStats: { date: string; cost: number; calls: number }[]
  providerStats: { provider: string; cost: number; calls: number }[]
}

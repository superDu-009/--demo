// types/ai.ts — 兼容旧导出，统一复用 task 类型

export type { TaskVO as AiTaskVO } from './task'

// API 消耗报告
export interface ApiCostReport {
  totalCalls: number
  totalCost: number
  avgCostPerCall: number
  failRate: number
  dailyStats: { date: string; cost: number; calls: number }[]
  providerStats: { provider: string; cost: number; calls: number }[]
}

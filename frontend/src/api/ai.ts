// api/ai.ts — 系分第 5.2 节：AI 任务接口

import request from '@/api'
import type { AiTaskVO, ApiResponse } from '@/types'

export const aiApi = {
  // 获取任务状态：GET /api/ai/task/{taskId}
  getTaskStatus: (taskId: number) =>
    request.get<never, ApiResponse<AiTaskVO>>(`/ai/task/${taskId}`),

  // 获取最新任务：GET /api/ai/task/latest
  getLatestTask: (shotId: number) =>
    request.get<never, ApiResponse<AiTaskVO>>('/ai/task/latest', { params: { shotId } }),

  // 获取消耗报告：GET /api/ai/cost-report
  getCostReport: (params?: { startDate?: string; endDate?: string }) =>
    request.get('/ai/cost-report', { params })
}

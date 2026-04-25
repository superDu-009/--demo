// api/ai.ts — 兼容旧导出，统一转发到 task 接口

import { taskApi } from '@/api/task'
import type { BatchTaskStatusRequest } from '@/types'

export const aiApi = {
  getTaskStatus: (taskId: number) =>
    taskApi.getStatus(taskId),

  getLatestTask: (_shotId: number) =>
    Promise.reject(new Error('最新任务接口已废弃，请改用 taskApi.getStatus 或 taskApi.getBatchStatus')),

  getBatchTaskStatus: (data: BatchTaskStatusRequest) =>
    taskApi.getBatchStatus(data)
}

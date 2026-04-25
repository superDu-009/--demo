import request from '@/api'
import type { ApiResponse, BatchTaskStatusRequest, BatchTaskStatusResponse, TaskVO } from '@/types'

export const taskApi = {
  getStatus: (id: number) =>
    request.get<never, ApiResponse<TaskVO>>(`/task/${id}`),

  getBatchStatus: (data: BatchTaskStatusRequest) =>
    request.post<never, ApiResponse<BatchTaskStatusResponse>>('/task/batch/status', data)
}

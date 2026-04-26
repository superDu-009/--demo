import request from '@/api'
import type { ApiResponse, BatchTaskStatusRequest, BatchTaskStatusResponse, TaskVO } from '@/types'

export const taskApi = {
  getStatus: (id: number) =>
    request.get<never, ApiResponse<TaskVO>>(`/task/${id}`),

  getBatchStatus: async (data: BatchTaskStatusRequest) => {
    const res = await request.post<never, ApiResponse<TaskVO[]>>('/task/batch/status', data)
    return {
      ...res,
      data: {
        batchId: data.batchId || null,
        tasks: res.data || []
      } as BatchTaskStatusResponse
    }
  }
}

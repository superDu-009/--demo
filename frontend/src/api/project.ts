// api/project.ts — 项目模块接口

import request from '@/api'
import type { ApiResponse, PageResult, ProjectCreateRequest, ProjectUpdateRequest, ProjectVO } from '@/types'

export const projectApi = {
  list: (params?: { page?: number; size?: number; keyword?: string }) =>
    request.get<never, ApiResponse<PageResult<ProjectVO>>>('/project/list', { params }),

  create: (data: ProjectCreateRequest) =>
    request.post<never, ApiResponse<number>>('/project', data),

  getDetail: (id: number) =>
    request.get<never, ApiResponse<ProjectVO>>(`/project/${id}`),

  update: (id: number, data: ProjectUpdateRequest) =>
    request.put(`/project/${id}`, data),

  delete: (id: number) =>
    request.delete(`/project/${id}`)
}

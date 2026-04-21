// api/project.ts — 系分第 5.2 节：项目模块接口

import request from '@/api'
import type {
  ProjectCreateRequest,
  ProjectUpdateRequest,
  ProjectVO,
  WorkflowConfigRequest,
  WorkflowStatusVO,
  ApiResponse,
  PageResult
} from '@/types'

export const projectApi = {
  // 获取项目列表（分页）：GET /api/project/list
  list: (params?: { page?: number; size?: number }) =>
    request.get<never, ApiResponse<PageResult<ProjectVO>>>('/project/list', { params }),

  // 创建项目：POST /api/project
  create: (data: ProjectCreateRequest) =>
    request.post<never, ApiResponse<number>>('/project', data),

  // 获取项目详情：GET /api/project/{id}
  getDetail: (id: number) =>
    request.get<never, ApiResponse<ProjectVO>>(`/project/${id}`),

  // 更新项目：PUT /api/project/{id}
  update: (id: number, data: ProjectUpdateRequest) =>
    request.put(`/project/${id}`, data),

  // 删除项目：DELETE /api/project/{id}
  delete: (id: number) =>
    request.delete(`/project/${id}`),

  // 保存流程配置：PUT /api/project/{id}/workflow
  saveWorkflow: (id: number, data: WorkflowConfigRequest) =>
    request.put(`/project/${id}/workflow`, data),

  // 开始执行流程：POST /api/project/{id}/workflow/start
  startWorkflow: (id: number) =>
    request.post(`/project/${id}/workflow/start`),

  // 获取流程状态：GET /api/project/{id}/workflow/status
  getWorkflowStatus: (id: number) =>
    request.get<never, ApiResponse<WorkflowStatusVO>>(`/project/${id}/workflow/status`),

  // 审核流程步骤：POST /api/project/{id}/workflow/review
  reviewWorkflow: (id: number, data: { stepType: string; action: string; comment?: string }) =>
    request.post(`/project/${id}/workflow/review`, data),

  // 停止流程：POST /api/project/{id}/workflow/stop
  stopWorkflow: (id: number) =>
    request.post(`/project/${id}/workflow/stop`),

  // v1.1：跨分场查询，用于分镜工作台"全部镜头"视图
  getShots: (projectId: number, params?: { sceneId?: number; status?: number; page?: number; size?: number }) =>
    request.get(`/project/${projectId}/shots`, { params })
}

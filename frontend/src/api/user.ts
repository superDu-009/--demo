// api/user.ts — 系分第 5.2 节：用户模块接口

import request from '@/api'
import type { LoginRequest, LoginResult, UserInfoVO, ApiResponse } from '@/types'

export const userApi = {
  // 登录：POST /api/user/login
  login: (data: LoginRequest) =>
    request.post<never, ApiResponse<LoginResult>>('/user/login', data),

  // 登出：POST /api/user/logout
  logout: () =>
    request.post('/user/logout'),

  // 获取用户信息：GET /api/user/info
  getInfo: () =>
    request.get<never, ApiResponse<UserInfoVO>>('/user/info')
}

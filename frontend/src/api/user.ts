// api/user.ts — 用户模块接口

import request from '@/api'
import type {
  ApiResponse,
  LoginRequest,
  LoginResult,
  UpdateAvatarPayload,
  UpdatePasswordPayload,
  UpdateUsernamePayload,
  UserInfoVO
} from '@/types'

export const userApi = {
  login: (data: LoginRequest) =>
    request.post<never, ApiResponse<LoginResult>>('/user/login', data),

  logout: () =>
    request.post('/user/logout'),

  getInfo: () =>
    request.get<never, ApiResponse<UserInfoVO>>('/user/info'),

  updateUsername: (data: UpdateUsernamePayload) =>
    request.put('/user/username', undefined, { params: { newUsername: data.username } }),

  updateAvatar: (data: UpdateAvatarPayload) =>
    request.put('/user/avatar', undefined, { params: { avatarUrl: data.avatarUrl } }),

  updatePassword: (data: UpdatePasswordPayload) =>
    request.put('/user/password', undefined, {
      params: {
        oldPassword: data.oldPassword,
        newPassword: data.newPassword
      }
    })
}

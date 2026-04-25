// types/user.ts — 用户类型定义

// 登录请求参数
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应结果
export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
}

export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  status: number
  avatar?: string | null
  avatarUrl?: string | null
}

export interface UpdateProfilePayload {
  username: string
  avatar?: string
}

export interface UpdateUsernamePayload {
  username: string
}

export interface UpdateAvatarPayload {
  avatarUrl: string
}

export interface UpdatePasswordPayload {
  oldPassword: string
  newPassword: string
}

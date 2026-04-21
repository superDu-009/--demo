// types/user.ts — 系分第 6.2 节：用户类型定义

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

// 用户信息 VO
export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  status: number
}

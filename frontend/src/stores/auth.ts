// stores/auth.ts — 认证与个人中心状态

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { userApi } from '@/api/user'
import type { UpdatePasswordPayload, UpdateProfilePayload, UserInfoVO } from '@/types'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(storage.get('token') || '')
  const userInfo = ref<UserInfoVO | null>(storage.get('user_info') || null)

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')

  function setToken(nextToken: string) {
    token.value = nextToken
    storage.set('token', nextToken, 24 * 60 * 60 * 1000)
  }

  function setUserInfo(info: UserInfoVO | null) {
    userInfo.value = info
    storage.set('user_info', info)
  }

  async function fetchUserInfo() {
    const res = await userApi.getInfo()
    setUserInfo({
      id: res.data.id,
      username: res.data.username,
      nickname: res.data.nickname,
      status: res.data.status,
      avatar: res.data.avatar || res.data.avatarUrl || storage.get('user_avatar') || '',
      avatarUrl: res.data.avatarUrl || res.data.avatar || ''
    })
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    storage.remove('token')
    storage.remove('user_info')
  }

  async function logout() {
    try {
      await userApi.logout()
    } catch {}
    clearAuth()
  }

  async function updateProfile(payload: UpdateProfilePayload) {
    if (payload.username !== userInfo.value?.username) {
      await userApi.updateUsername({ username: payload.username })
    }
    if (payload.avatar && payload.avatar !== userInfo.value?.avatar) {
      await userApi.updateAvatar({ avatarUrl: payload.avatar })
    }
    const nextInfo: UserInfoVO = {
      ...(userInfo.value || { id: 0, username: payload.username, nickname: payload.username, status: 1 }),
      username: payload.username,
      nickname: payload.username,
      avatar: payload.avatar || userInfo.value?.avatar || '',
      avatarUrl: payload.avatar || userInfo.value?.avatarUrl || ''
    }
    storage.set('user_avatar', nextInfo.avatar || '')
    setUserInfo(nextInfo)
  }

  async function updatePassword(payload: UpdatePasswordPayload) {
    await userApi.updatePassword(payload)
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    displayName,
    setToken,
    setUserInfo,
    fetchUserInfo,
    clearAuth,
    logout,
    updateProfile,
    updatePassword
  }
})

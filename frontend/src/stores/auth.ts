// stores/auth.ts — 系分第 7.2 节：认证模块（核心 Store）

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  // 从 localStorage 恢复 token（storage 模块内置 TTL 检查）
  const token = ref<string>(storage.get('token') || '')
  const userInfo = ref<{ id: number; username: string; nickname: string } | null>(null)

  // 计算属性：是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 设置 Token（同时持久化到 localStorage，24h TTL）
  function setToken(t: string) {
    token.value = t
    storage.set('token', t, 24 * 60 * 60 * 1000) // 24h
  }

  // 设置用户信息
  function setUserInfo(info: typeof userInfo.value) {
    userInfo.value = info
  }

  // 获取用户信息（从后端拉取）
  async function fetchUserInfo() {
    const res = await userApi.getInfo()
    userInfo.value = {
      id: res.data.id,
      username: res.data.username,
      nickname: res.data.nickname
    }
  }

  // 清除认证信息（token + 用户信息）
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    storage.remove('token')
  }

  // 登出（调用后端接口 + 清除本地状态）
  async function logout() {
    try { await userApi.logout() } catch {}
    clearAuth()
  }

  return {
    token, userInfo, isLoggedIn,
    setToken, setUserInfo, fetchUserInfo, clearAuth, logout
  }
})

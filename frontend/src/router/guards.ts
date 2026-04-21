// router/guards.ts — 系分第 3 节：路由守卫逻辑

import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 设置路由守卫
export function setupGuards(router: Router) {
  router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    // 未登录且需要认证 → 跳转登录页（携带 redirect 参数）
    if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
      return next({ name: 'Login', query: { redirect: to.fullPath } })
    }

    // 已登录访问登录页 → 跳转项目列表
    if (to.name === 'Login' && authStore.isLoggedIn) {
      return next({ name: 'ProjectList' })
    }

    // 设置页面标题
    if (to.meta.title) {
      document.title = `${to.meta.title} - AI漫剧生产平台`
    }

    next()
  })
}

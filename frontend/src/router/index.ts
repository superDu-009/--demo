// router/index.ts — 系分第 3 节：路由设计

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // 登录页
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  // 主布局（包含 Header + Sidebar）
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout/AppLayout.vue'),
    redirect: '/projects',
    meta: { requiresAuth: true },
    children: [
      // 项目列表页
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('@/views/ProjectList.vue'),
        meta: { title: '项目列表' }
      },
      // 项目详情页（Tab 容器）
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/ProjectDetail.vue'),
        meta: { title: '项目详情' },
        redirect: { name: 'AssetLibrary' },
        children: [
          // 资产库 Tab
          {
            path: 'assets',
            name: 'AssetLibrary',
            component: () => import('@/views/tabs/AssetLibrary.vue'),
            meta: { title: '资产库' }
          },
          // 流程编辑器 Tab
          {
            path: 'workflow',
            name: 'WorkflowEditor',
            component: () => import('@/views/tabs/WorkflowEditor.vue'),
            meta: { title: '流程编辑器' }
          },
          // 分镜工作台 Tab
          {
            path: 'shots',
            name: 'ShotWorkbench',
            component: () => import('@/views/tabs/ShotWorkbench.vue'),
            meta: { title: '分镜工作台' }
          },
          // API 消耗看板 Tab
          {
            path: 'cost',
            name: 'ApiCost',
            component: () => import('@/views/tabs/ApiCost.vue'),
            meta: { title: 'API 消耗' }
          }
        ]
      }
    ]
  },
  // 404 页
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 引入路由守卫
import { setupGuards } from './guards'
setupGuards(router)

export default router

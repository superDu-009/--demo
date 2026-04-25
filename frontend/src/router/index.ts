import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout/AppLayout.vue'),
    redirect: '/projects',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('@/views/ProjectList.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/ProjectDetail.vue'),
        meta: { title: '项目详情' },
        redirect: { name: 'ScriptPreview' },
        children: [
          {
            path: 'script',
            name: 'ScriptPreview',
            component: () => import('@/views/tabs/ScriptPreview.vue'),
            meta: { title: '剧本预览' }
          },
          {
            path: 'shots',
            name: 'ShotWorkbench',
            component: () => import('@/views/tabs/ShotWorkbench.vue'),
            meta: { title: '分镜工作台' }
          },
          {
            path: 'assets',
            name: 'AssetLibrary',
            component: () => import('@/views/tabs/AssetLibrary.vue'),
            meta: { title: '资产库' }
          }
        ]
      }
    ]
  },
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

setupGuards(router)

export default router

<!-- components/Layout/AppLayout.vue — 主布局组件
     系分第 2 节项目结构：包含顶部导航 + 内容区域 -->
<template>
  <el-container class="app-layout">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <!-- 平台 Logo/标题 -->
        <span class="app-logo" @click="router.push({ name: 'ProjectList' })">
          AI漫剧生产平台
        </span>
      </div>
      <div class="header-right">
        <!-- 用户信息显示 -->
        <el-dropdown trigger="click">
          <span class="user-info">
            <!-- 显示昵称或用户名 -->
            {{ authStore.userInfo?.nickname || authStore.userInfo?.username || '用户' }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <!-- 登出按钮 -->
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 内容区域 -->
    <el-main class="app-main">
      <!-- 路由出口：子路由页面在此渲染 -->
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
// 引入认证 Store
const authStore = useAuthStore()

// 处理登出：清除认证状态后跳转登录页
const handleLogout = async () => {
  await authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  background-color: $bg-page;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 $page-padding;
  background-color: $bg-card;
  border-bottom: 1px solid $border-color;
}

.app-logo {
  font-size: 18px;
  font-weight: 600;
  color: $primary-color;
  cursor: pointer;

  &:hover {
    opacity: 0.85;
  }
}

.user-info {
  display: flex;
  align-items: center;
  color: $text-primary;
  cursor: pointer;
  font-size: 14px;
}

.app-main {
  padding: $page-padding;
  background-color: $bg-page;
  overflow-y: auto;
}
</style>

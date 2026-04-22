<!-- components/Layout/AppLayout.vue — 主布局组件
     系分第 2 节项目结构：包含顶部导航 + 内容区域 -->
<template>
  <el-container class="app-layout">
    <!-- 左侧霓虹导航栏 -->
    <el-aside class="sidebar" width="80px">
      <div class="sidebar-logo" @click="router.push({ name: 'ProjectList' })">
        <el-icon :size="36" class="logo-icon text-neon"><VideoPlay /></el-icon>
      </div>
      <div class="sidebar-menu">
        <div 
          v-for="item in menuList" 
          :key="item.path"
          class="menu-item"
          :class="{ active: $route.name === item.name }"
          @click="router.push({ name: item.name })"
        >
          <el-icon :size="24" class="menu-icon"><component :is="item.icon" /></el-icon>
          <span class="menu-tooltip">{{ item.title }}</span>
        </div>
      </div>
      <div class="sidebar-footer">
        <div class="menu-item" @click="handleLogout">
          <el-icon :size="24" class="menu-icon"><SwitchButton /></el-icon>
          <span class="menu-tooltip">退出登录</span>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区域 -->
    <el-container class="main-container">
      <!-- 顶部状态栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <h1 class="page-title text-neon">{{ currentPageTitle }}</h1>
        </div>
        <div class="header-right">
          <!-- 用户信息显示 -->
          <div class="user-info">
            <el-avatar :size="36" class="user-avatar">
              {{ (authStore.userInfo?.nickname || authStore.userInfo?.username || '用户').charAt(0) }}
            </el-avatar>
            <span class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username || '用户' }}</span>
          </div>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="app-main">
        <!-- 路由出口：子路由页面在此渲染 -->
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  VideoPlay, 
  FolderOpened, 
  Picture, 
  Grid, 
  Film, 
  DataAnalysis, 
  SwitchButton 
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
// 引入认证 Store
const authStore = useAuthStore()

// 菜单列表
const menuList = [
  {
    name: 'ProjectList',
    path: '/projects',
    title: '项目列表',
    icon: FolderOpened
  },
  {
    name: 'AssetLibrary',
    path: '/projects/:id/assets',
    title: '资产库',
    icon: Picture
  },
  {
    name: 'WorkflowEditor',
    path: '/projects/:id/workflow',
    title: '流程编辑器',
    icon: Grid
  },
  {
    name: 'ShotWorkbench',
    path: '/projects/:id/shots',
    title: '分镜工作台',
    icon: Film
  },
  {
    name: 'ApiCost',
    path: '/projects/:id/cost',
    title: 'API消耗',
    icon: DataAnalysis
  }
]

// 当前页面标题
const currentPageTitle = computed(() => {
  const currentMenu = menuList.find(item => item.name === route.name)
  return currentMenu?.title || 'AI漫剧生产平台'
})

// 处理登出：清除认证状态后跳转登录页
const handleLogout = async () => {
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Login' })
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  background-color: $bg-page;
  display: flex;
}

// 左侧导航栏
.sidebar {
  background: rgba(22, 24, 38, 0.9);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(100, 108, 255, 0.2);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 0 30px rgba(100, 108, 255, 0.2);
    border-right-color: $border-glow-color;
  }

  // Logo区域
  .sidebar-logo {
    margin-bottom: 40px;
    cursor: pointer;
    padding: 10px;
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(100, 108, 255, 0.1);
      box-shadow: 0 0 20px rgba(100, 108, 255, 0.2);
    }

    .logo-icon {
      animation: pulse 2s infinite;
    }
  }

  // 菜单区域
  .sidebar-menu {
    flex: 1;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  // 底部区域
  .sidebar-footer {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: auto;
  }

  // 菜单项
  .menu-item {
    position: relative;
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    color: $text-secondary;

    &:hover {
      background: rgba(100, 108, 255, 0.15);
      color: $primary-color;
      transform: translateY(-2px);

      .menu-tooltip {
        opacity: 1;
        visibility: visible;
        transform: translateX(0);
      }
    }

    &.active {
      background: $primary-gradient;
      color: #fff;
      box-shadow: 0 0 20px rgba(100, 108, 255, 0.4);
    }

    .menu-icon {
      font-size: 24px;
    }

    // 提示框
    .menu-tooltip {
      position: absolute;
      left: 72px;
      top: 50%;
      transform: translateY(-50%) translateX(10px);
      background: rgba(22, 24, 38, 0.95);
      backdrop-filter: blur(12px);
      padding: 8px 16px;
      border-radius: 8px;
      border: 1px solid $border-color;
      color: $text-primary;
      font-size: 14px;
      white-space: nowrap;
      opacity: 0;
      visibility: hidden;
      transition: all 0.3s ease;
      z-index: 1001;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);

      &::before {
        content: '';
        position: absolute;
        left: -6px;
        top: 50%;
        transform: translateY(-50%);
        width: 10px;
        height: 10px;
        background: rgba(22, 24, 38, 0.95);
        border-left: 1px solid $border-color;
        border-bottom: 1px solid $border-color;
        transform: translateY(-50%) rotate(45deg);
      }
    }
  }
}

// 主内容区域
.main-container {
  flex: 1;
  margin-left: 80px;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 $page-padding;
  background: rgba(22, 24, 38, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(100, 108, 255, 0.2);
  position: sticky;
  top: 0;
  z-index: 999;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    color: $text-primary;
    font-size: 15px;
    font-weight: 500;

    .user-avatar {
      background: $primary-gradient;
      color: #fff;
      font-weight: 600;
      box-shadow: 0 0 12px rgba(100, 108, 255, 0.3);
    }
  }
}

.app-main {
  padding: $page-padding;
  background-color: $bg-page;
  overflow-y: auto;
  flex: 1;
}

// 脉冲动画
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>

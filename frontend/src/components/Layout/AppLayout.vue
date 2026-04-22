<!-- components/Layout/AppLayout.vue — 主布局组件
     系分第 2 节项目结构：包含顶部导航 + 内容区域 -->
<template>
  <el-container class="app-layout">
    <!-- 左侧霓虹导航栏 -->
    <el-aside class="sidebar" width="220px">
      <div class="sidebar-logo" @click="router.push({ name: 'ProjectList' })">
        <el-icon :size="36" class="logo-icon text-neon"><VideoPlay /></el-icon>
      </div>
        <div class="sidebar-menu">
            <el-tooltip 
              v-for="item in menuList" 
              :key="item.path"
              :disabled="item.name === 'ProjectList' || !!$route.params.id"
              content="请先选择一个项目进入详情页"
              placement="right"
            >
              <div 
                class="menu-item"
                :class="{ active: $route.name === item.name, disabled: item.name !== 'ProjectList' && !$route.params.id }"
                @click="handleMenuClick(item)"
              >
                <div class="menu-indicator"></div>
                <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
                <span class="menu-title">{{ item.title }}</span>
              </div>
            </el-tooltip>
        </div>
       <div class="sidebar-footer">
        <div class="menu-item" @click="handleLogout">
          <div class="menu-indicator"></div>
          <el-icon class="menu-icon"><SwitchButton /></el-icon>
          <span class="menu-title">退出登录</span>
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
    title: '项目管理',
    icon: FolderOpened
  },
  {
    name: 'AssetLibrary',
    path: '/projects/:id/assets',
    title: '素材中心',
    icon: Picture
  },
  {
    name: 'WorkflowEditor',
    path: '/projects/:id/workflow',
    title: '工作流配置',
    icon: Grid
  },
  {
    name: 'ShotWorkbench',
    path: '/projects/:id/shots',
    title: '分镜制作',
    icon: Film
  },
  {
    name: 'ApiCost',
    path: '/projects/:id/cost',
    title: '数据统计',
    icon: DataAnalysis
  }
]

// 当前页面标题
const currentPageTitle = computed(() => {
  const currentMenu = menuList.find(item => item.name === route.name)
  return currentMenu?.title || 'AI漫剧生产平台'
})

// 处理菜单点击
const handleMenuClick = (item: any) => {
  // 项目管理不需要参数，直接跳转
  if (item.name === 'ProjectList') {
    router.push({ name: item.name })
    return
  }

  // 其他菜单需要项目ID参数
  const projectId = route.params.id
  if (!projectId) {
    ElMessage.warning('请先选择一个项目进入详情页')
    return
  }

  // 有ID的话正常跳转
  router.push({ name: item.name, params: { id: projectId } })
}

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

// 左侧导航栏（展开状态）
.sidebar {
  background: rgba(22, 24, 38, 0.9);
  backdrop-filter: blur(12px);
  border-right: 1px solid rgba(100, 108, 255, 0.2);
  height: 100vh;
  width: 220px;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 20px 16px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    box-shadow: 0 0 30px rgba(100, 108, 255, 0.25);
    border-right-color: $border-glow-color;
  }

  // Logo区域
  .sidebar-logo {
    margin-bottom: 40px;
    cursor: pointer;
    padding: 12px 16px;
    border-radius: 12px;
    width: 100%;
    display: flex;
    align-items: center;
    gap: 12px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    user-select: none;

    &:hover {
      background: rgba(100, 108, 255, 0.1);
      box-shadow: 0 0 20px rgba(100, 108, 255, 0.2);
      padding-left: 20px;
    }

    &:active {
      transform: scale(0.98);
    }

    .logo-icon {
      animation: pulse 2s infinite;
      font-size: 32px;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 700;
      background: $primary-gradient;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  // 菜单区域
  .sidebar-menu {
    flex: 1;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  // 底部区域
  .sidebar-footer {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: auto;
  }

  // 菜单项
  .menu-item {
    position: relative;
    width: 100%;
    height: 52px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 16px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    color: $text-secondary;
    user-select: none;

    &:hover {
      background: rgba(100, 108, 255, 0.15);
      color: $primary-color;
      padding-left: 20px;
    }

    &:active {
      transform: scale(0.98);
    }

    &.active {
      background: $primary-gradient;
      color: #fff;
      box-shadow: 0 0 20px rgba(100, 108, 255, 0.4);
      padding-left: 20px;

      .menu-indicator {
        opacity: 1;
        transform: scaleY(1);
      }
    }

    &.disabled {
      opacity: 0.4;
      cursor: not-allowed;
      pointer-events: all;
    }

    .menu-indicator {
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%) scaleY(0);
      width: 3px;
      height: 24px;
      background: #fff;
      border-radius: 0 2px 2px 0;
      opacity: 0;
      transition: all 0.2s ease;
    }

    .menu-icon {
      font-size: 22px;
      transition: all 0.2s ease;
    }

    .menu-title {
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s ease;
    }
  }
}

// 主内容区域
.main-container {
  flex: 1;
  margin-left: 220px;
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
  padding-bottom: 4px; /* 大幅减小底部padding，让分页离底边更近 */
  background-color: $bg-page;
  overflow-y: auto;
  flex: 1;
  box-sizing: border-box;
  height: calc(100vh - 64px);
  /* 隐藏滚动条但保留滚动功能 */
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 108, 255, 0.5) transparent;
}
.app-main::-webkit-scrollbar {
  width: 6px;
}
.app-main::-webkit-scrollbar-track {
  background: transparent;
}
.app-main::-webkit-scrollbar-thumb {
  background: rgba(100, 108, 255, 0.5);
  border-radius: 3px;
}
.app-main::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 108, 255, 0.7);
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

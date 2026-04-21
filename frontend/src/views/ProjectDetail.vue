<!-- views/ProjectDetail.vue — 项目详情页（Tab 容器）
     系分第 4.3 节：顶部面包屑 + Tab 栏 + 子路由出口 -->
<template>
  <div class="project-detail-page">
    <!-- 顶部面包屑 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item
        :to="{ name: 'ProjectList' }"
      >
        项目列表
      </el-breadcrumb-item>
      <!-- 当前项目名称 -->
      <el-breadcrumb-item>{{ projectInfo?.name || '加载中...' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 项目信息栏 -->
    <div class="project-info-bar" v-loading="loading">
      <div class="project-basic-info">
        <h2 class="project-name">{{ projectInfo?.name }}</h2>
        <div class="project-meta">
          <!-- <StatusTag :status="projectInfo?.status" style="margin-right: 12px" /> -->
          <span class="status-tag" style="margin-right: 12px">
            {{ projectInfo?.status === 0 ? '草稿' : projectInfo?.status === 1 ? '进行中' : '已完成' }}
          </span>
          <span v-if="projectInfo?.executionLock" class="lock-tag">
            <el-icon><Lock /></el-icon>
            执行中
          </span>
          <span class="create-time">
            <el-icon><Calendar /></el-icon>
            创建于 {{ formatDate(projectInfo?.createTime) }}
          </span>
        </div>
      </div>
      <div class="project-actions">
        <el-button @click="handleEditProject">
          <el-icon><Edit /></el-icon>
          编辑项目
        </el-button>
      </div>
    </div>

    <!-- Tab 导航栏（对应 4 个子路由） -->
    <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="资产库" name="AssetLibrary" />
      <el-tab-pane label="流程编辑器" name="WorkflowEditor" />
      <el-tab-pane label="分镜工作台" name="ShotWorkbench" />
      <el-tab-pane label="API 消耗" name="ApiCost" />
    </el-tabs>

    <!-- 子路由出口：加载对应的 Tab 页面组件 -->
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Calendar, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useProjectStore } from '@/stores/project'
import { projectApi } from '@/api/project'
import type { ProjectVO } from '@/types'
// import StatusTag from '@/components/Common/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

// 状态变量
const loading = ref(false)
const projectInfo = ref<ProjectVO | null>(null)
// 当前激活的 Tab（根据路由名称同步）
const activeTab = ref<string>(route.name as string)

// 加载项目详情
const fetchProjectDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await projectApi.getDetail(id)
    if (res.code === 0) {
      projectInfo.value = res.data
      // 保存到store供子页面使用（store方法不存在，临时注释）
      // projectStore.setCurrentProject(res.data)
    }
  } catch (err) {
    ElMessage.error('获取项目详情失败')
  } finally {
    loading.value = false
  }
}

// 编辑项目
const handleEditProject = () => {
  // TODO: 打开编辑弹窗，复用项目列表的编辑逻辑
  ElMessage.info('编辑功能开发中')
}

// Tab 切换时同步路由（TabPaneName 可能是 string | number）
const handleTabChange = (tabName: string | number) => {
  // 根据 tab 名称跳转到对应子路由
  const routeMap: Record<string, string> = {
    AssetLibrary: 'assets',
    WorkflowEditor: 'workflow',
    ShotWorkbench: 'shots',
    ApiCost: 'cost'
  }
  const subPath = routeMap[String(tabName)]
  if (subPath) {
    router.push({ name: String(tabName), params: route.params })
  }
}

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 进入页面时加载项目详情
onMounted(async () => {
  const id = Number(route.params.id)
  if (id) {
    await fetchProjectDetail(id)
  }
})

// 监听路由参数变化，切换项目时重新加载
watch(() => route.params.id, (newId) => {
  const id = Number(newId)
  if (id) {
    fetchProjectDetail(id)
  }
})
</script>

<style scoped lang="scss">
.project-detail-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.project-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: $card-bg;
  border-radius: 8px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.project-basic-info {
  .project-name {
    font-size: 24px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 8px 0;
  }

  .project-meta {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: $text-secondary;
    gap: 12px;

    .lock-tag {
      display: flex;
      align-items: center;
      gap: 4px;
      color: $warning-color;
    }

    .status-tag {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      color: #fff;
      background: #909399;
      &:nth-of-type(1) {
        background: #909399; /* 草稿 */
      }
      &:nth-of-type(2) {
        background: #409EFF; /* 进行中 */
      }
      &:nth-of-type(3) {
        background: #67C23A; /* 已完成 */
      }
    }

    .create-time {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.detail-tabs {
  margin-bottom: 24px;
  background: $card-bg;
  border-radius: 8px;
  padding: 0 16px;
}

// Tab 样式适配暗色主题
:deep(.el-tabs__header) {
  border-bottom-color: $border-color;
  margin: 0;
}

:deep(.el-tabs__item) {
  color: $text-secondary;
  font-size: 15px;
  height: 48px;
  line-height: 48px;

  &.is-active {
    color: $primary-color;
    font-weight: 500;
  }
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>

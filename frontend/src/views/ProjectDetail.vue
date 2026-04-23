<!-- views/ProjectDetail.vue — 项目详情页（Tab 容器）
     系分第 4.3 节：顶部面包屑 + Tab 栏 + 子路由出口 -->
<template>
  <div class="project-detail-page">
    <!-- 项目信息栏 -->
    <div class="project-info-bar card-glass border-neon" v-loading="loading">
      <div class="project-basic-info">
        <h2 class="project-name text-neon">{{ projectInfo?.name }}</h2>
        <div class="project-meta">
          <span class="status-tag" :class="projectStatusClass">
            {{ projectStatusLabel }}
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
        <el-button class="btn-gradient" @click="handleEditProject">
          <el-icon><Edit /></el-icon>
          编辑项目
        </el-button>
      </div>
    </div>

    <!-- Tab 导航栏（对应 4 个子路由） -->
    <div class="tab-nav">
      <div
        v-for="tab in tabList"
        :key="tab.name"
        class="tab-item"
        :class="{ active: activeTab === tab.name }"
        @click="handleTabChange(tab.name)"
      >
        <el-icon :size="18"><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </div>

    <!-- 子路由出口：加载对应的 Tab 页面组件 -->
    <div class="tab-content">
      <router-view />
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑项目"
      width="520px"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="82px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="editForm.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="editLoading" @click="submitEditProject">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Calendar, Edit, Picture, Grid, Film, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { projectApi } from '@/api/project'
import type { ProjectVO } from '@/types'
import { PROJECT_STATUS_MAP } from '@/constants/status'

const route = useRoute()
const router = useRouter()

// 状态变量
const loading = ref(false)
const projectInfo = ref<ProjectVO | null>(null)
// 当前激活的 Tab（根据路由名称同步）
const activeTab = ref<string>(route.name as string)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = reactive({
  name: '',
  description: ''
})

const editRules: FormRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 1, max: 200, message: '长度在 1 到 200 个字符', trigger: 'blur' }
  ]
}

// Tab列表
const tabList = [
  { name: 'AssetLibrary', label: '资产库', icon: Picture },
  { name: 'WorkflowEditor', label: '流程编辑器', icon: Grid },
  { name: 'ShotWorkbench', label: '分镜工作台', icon: Film },
  { name: 'ApiCost', label: 'API 消耗', icon: DataAnalysis }
]

const projectStatusLabel = computed(() => {
  if (projectInfo.value?.status === undefined) return '加载中'
  return PROJECT_STATUS_MAP[projectInfo.value.status]?.label || '未知'
})

const projectStatusClass = computed(() => {
  if (projectInfo.value?.status === undefined) return 'status-unknown'
  return `status-${projectInfo.value.status}`
})

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
  if (!projectInfo.value) return
  editForm.name = projectInfo.value.name
  editForm.description = projectInfo.value.description || ''
  editDialogVisible.value = true
}

const submitEditProject = async () => {
  if (!projectInfo.value) return
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  editLoading.value = true
  try {
    await projectApi.update(projectInfo.value.id, {
      name: editForm.name.trim(),
      description: editForm.description.trim()
    })
    ElMessage.success('项目已更新')
    editDialogVisible.value = false
    await fetchProjectDetail(projectInfo.value.id)
  } catch {
    ElMessage.error('编辑项目失败')
  } finally {
    editLoading.value = false
  }
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

watch(() => route.name, (name) => {
  activeTab.value = String(name || '')
}, { immediate: true })
</script>

<style scoped lang="scss">
.project-detail-page {
  max-width: 1400px;
  margin: 0 auto;
}

.project-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.project-basic-info {
  .project-name {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    margin: 0 0 12px 0;
  }

  .project-meta {
    display: flex;
    align-items: center;
    font-size: 14px;
    color: $text-secondary;
    gap: 16px;

    .lock-tag {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #f59e0b;
    }

    .status-tag {
      padding: 6px 14px;
      border-radius: 8px;
      font-size: 12px;
      color: #fff;
      backdrop-filter: blur(8px);
      background: rgba(144, 147, 153, 0.8);

      &.status-0 {
        background: rgba(144, 147, 153, 0.8);
      }

      &.status-1 {
        background: rgba(64, 158, 255, 0.8);
      }

      &.status-2 {
        background: rgba(103, 194, 58, 0.8);
      }
    }

    .create-time {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

// 胶囊Tab导航
.tab-nav {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: rgba(22, 24, 38, 0.7);
  border-radius: 12px;
  border: 1px solid rgba(100, 108, 255, 0.2);
  margin-bottom: 24px;
  width: fit-content;

  .tab-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    color: $text-secondary;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(100, 108, 255, 0.1);
      color: $primary-color;
    }

    &.active {
      background: $primary-gradient;
      color: #fff;
      box-shadow: 0 0 15px rgba(100, 108, 255, 0.3);
    }
  }
}

.tab-content {
  min-height: 500px;
}
</style>

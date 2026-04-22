<!-- views/ProjectList.vue — 项目列表页
     系分第 4.2 节：顶部搜索栏 + 「新建项目」按钮 + 主体内容区 + 分页 -->
<template>
  <div class="project-list-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-actions">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索项目名称"
          style="width: 280px; margin-right: 12px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <!-- 创建项目按钮 -->
        <el-button type="primary" class="btn-gradient" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建项目
        </el-button>
      </div>
    </div>

    <!-- 项目列表卡片 -->
    <div class="project-card-list" v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="project in projectList" :key="project.id">
          <el-card class="project-card card-glass border-neon" @click="goToDetail(project.id)">
            <!-- 项目封面 -->
            <div class="project-cover">
              <img :src="getProjectCover(project.id)" alt="项目封面" class="cover-img">
              <div class="cover-overlay">
                <span class="status-tag">{{ project.status === 0 ? '草稿' : project.status === 1 ? '进行中' : '已完成' }}</span>
              </div>
            </div>
            
            <div class="card-content">
              <div class="card-header">
                <span class="project-name text-neon" :title="project.name">{{ project.name }}</span>
              </div>
              <div class="project-desc" :title="project.description || '暂无描述'">
                {{ project.description || '暂无描述' }}
              </div>
              <div class="project-meta">
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(project.createTime) }}
                </span>
                <span class="meta-item" v-if="project.executionLock">
                  <el-icon><Lock /></el-icon>
                  执行中
                </span>
              </div>
              <div class="card-actions">
                <el-button
                  size="small"
                  @click.stop="openEditDialog(project)"
                  :disabled="project.status !== 0"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click.stop="handleDelete(project)"
                  :disabled="project.executionLock === 1"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <div v-if="projectList.length === 0 && !loading" class="empty-container">
        <el-empty description="暂无项目，点击「新建项目」开始" />
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageParams.page"
        v-model:page-size="pageParams.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchProjectList"
        @current-change="fetchProjectList"
      />
    </div>

    <!-- 新建/编辑项目弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑项目' : '新建项目'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入项目名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入项目描述（可选）"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="小说文件" v-if="!isEdit">
<!-- 小说文件上传（临时实现，等ImageUploader组件完成后替换） -->
<el-input v-model="formData.novelTosPath" placeholder="请输入小说文件TOS路径" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="400px"
    >
      <p>确定要删除项目「{{ currentDeleteProject?.name }}」吗？此操作不可恢复。</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Calendar, Lock } from '@element-plus/icons-vue'
import { ElMessage, ElForm } from 'element-plus'
import dayjs from 'dayjs'
import { projectApi } from '@/api/project'
import type { ProjectVO, ProjectCreateRequest, ProjectUpdateRequest } from '@/types'
// 移除缺失组件引用
// import StatusTag from '@/components/Common/StatusTag.vue'
// import ImageUploader from '@/components/Asset/ImageUploader.vue'

const router = useRouter()
const formRef = ref<InstanceType<typeof ElForm>>()

// 状态变量
const loading = ref(false)
const submitLoading = ref(false)
const deleteLoading = ref(false)
const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const searchKeyword = ref('')
const projectList = ref<ProjectVO[]>([])
const total = ref(0)
const isEdit = ref(false)
const currentDeleteProject = ref<ProjectVO | null>(null)

// 分页参数
const pageParams = reactive({
  page: 1,
  size: 12
})

// 表单数据
const formData = reactive<ProjectCreateRequest>({
  name: '',
  description: '',
  novelTosPath: ''
})

// 表单校验规则
const formRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' },
    { min: 1, max: 200, message: '长度在 1 到 200 个字符', trigger: 'blur' }
  ]
}

// 获取项目列表
const fetchProjectList = async () => {
  loading.value = true
  try {
    const res = await projectApi.list(pageParams)
    if (res.code === 0) {
      // TODO: 过滤搜索关键词（后续后端支持搜索参数后替换）
      let list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data?.records || [])
      if (searchKeyword.value) {
        list = list.filter((item: ProjectVO) => item.name.includes(searchKeyword.value))
      }
      projectList.value = list
      total.value = res.data?.total || list.length
    }
  } catch (err) {
    ElMessage.error('获取项目列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageParams.page = 1
  fetchProjectList()
}

// 打开新建弹窗
const openCreateDialog = () => {
  isEdit.value = false
  formData.name = ''
  formData.description = ''
  formData.novelTosPath = ''
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (project: ProjectVO) => {
  isEdit.value = true
  formData.name = project.name
  formData.description = project.description || ''
  formData.novelTosPath = project.novelTosPath || ''
  dialogVisible.value = true
}

// 小说上传成功处理
const handleNovelUploadSuccess = (keys: string[]) => {
  if (keys.length > 0) {
    formData.novelTosPath = keys[0]
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      // 编辑项目
      const updateData: ProjectUpdateRequest = {
        name: formData.name,
        description: formData.description
      }
      // TODO: 获取当前编辑项目ID
      // await projectApi.update(currentEditId, updateData)
      ElMessage.success('编辑成功')
    } else {
      // 新建项目
      await projectApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchProjectList()
  } catch (err) {
    ElMessage.error(isEdit.value ? '编辑失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 打开删除确认弹窗
const handleDelete = (project: ProjectVO) => {
  currentDeleteProject.value = project
  deleteDialogVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!currentDeleteProject.value) return
  deleteLoading.value = true
  try {
    await projectApi.delete(currentDeleteProject.value.id)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    fetchProjectList()
  } catch (err: any) {
    if (err.response?.data?.code === 40901) {
      ElMessage.error('项目正在执行中，无法删除')
    } else {
      ElMessage.error('删除失败')
    }
  } finally {
    deleteLoading.value = false
  }
}

// 跳转到项目详情
const goToDetail = (id: number) => {
  router.push(`/projects/${id}`)
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 获取项目默认封面
const getProjectCover = (id: number) => {
  const covers = [
    '/assets/images/project-cover-japanese.png',
    '/assets/images/project-cover-chinese.png',
    '/assets/images/project-cover-scifi.png',
    '/assets/images/project-cover-q.png'
  ]
  return covers[id % covers.length]
}

// 页面加载时获取项目列表
onMounted(() => {
  fetchProjectList()
})
</script>

<style scoped lang="scss">
.project-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid $border-color;
}

.toolbar-actions {
  display: flex;
  align-items: center;
}

.project-card-list {
  min-height: 400px;
}

.project-card {
  height: 300px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
  }

  // 项目封面
  .project-cover {
    position: relative;
    height: 130px;
    width: 100%;
    overflow: hidden;
    border-radius: 12px 12px 0 0;

    .cover-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s ease;
    }

    .cover-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(to bottom, rgba(0,0,0,0.1), rgba(10,10,18,0.8));
      display: flex;
      align-items: flex-end;
      justify-content: flex-end;
      padding: 12px;

      .status-tag {
        padding: 4px 10px;
        border-radius: 20px;
        font-size: 12px;
        color: #fff;
        backdrop-filter: blur(8px);
        &:contains('草稿') {
          background: rgba(144, 147, 153, 0.8);
        }
        &:contains('进行中') {
          background: rgba(64, 158, 255, 0.8);
        }
        &:contains('已完成') {
          background: rgba(103, 194, 58, 0.8);
        }
      }
    }
  }

  &:hover .cover-img {
    transform: scale(1.05);
  }

  // 卡片内容
  .card-content {
    padding: 16px;
  }

  .card-header {
    margin-bottom: 8px;
  }

  .project-name {
    font-weight: 600;
    font-size: 17px;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }

  .project-desc {
    height: 44px;
    font-size: 13px;
    color: $text-secondary;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 12px;
  }

  .project-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: $text-tertiary;
    margin-bottom: 12px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.empty-container {
  padding: 60px 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>

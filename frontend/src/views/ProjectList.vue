<template>
  <div class="project-list-page">
    <section class="project-hero card-glass border-neon hud-panel hud-corner">
      <div>
        <p class="eyebrow">Project Dock</p>
        <h2 class="hud-title">创作舰队</h2>
      </div>
      <div class="hero-stats">
        <div>
          <span>项目数</span>
          <strong>{{ projectList.length }}</strong>
        </div>
        <div>
          <span>已上传剧本</span>
          <strong>{{ uploadedCount }}</strong>
        </div>
      </div>
    </section>

    <section class="toolbar card-glass">
      <el-input
        v-model="searchKeyword"
        class="search-box"
        placeholder="搜索项目名称"
        clearable
        @input="fetchProjectList"
      />
      <vi-button color="blue" radius="round" mutate @click="openCreateDialog">新建项目</vi-button>
    </section>

    <section class="project-grid" v-loading="loading">
      <article v-for="project in filteredList" :key="project.id" class="project-card card-glass border-neon hud-corner">
        <div class="cover scanline" :style="{ backgroundImage: `url(${getProjectCover(project)})` }" @click="goToDetail(project.id)">
          <span class="style-tag">{{ project.style || '未设置风格' }}</span>
          <span class="launch-tag">OPEN</span>
        </div>
        <div class="card-body">
          <div class="card-head">
            <div>
              <h3>{{ project.name }}</h3>
              <p>{{ formatDate(project.createTime) }}</p>
            </div>
            <div class="actions">
              <el-button text @click="openEditDialog(project)">编辑</el-button>
              <el-button text type="danger" @click="handleDelete(project)">删除</el-button>
            </div>
          </div>
          <p class="description">{{ project.description || '未填写项目描述。' }}</p>
          <div class="meta">
            <span>{{ project.ratio || '16:9' }}</span>
            <span>{{ project.definition || '1080P' }}</span>
            <span>{{ project.novelTosPath ? '已上传剧本' : '未上传剧本' }}</span>
          </div>
        </div>
      </article>

      <el-empty v-if="!loading && filteredList.length === 0" description="暂无项目" />
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="formData.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="小说文件" prop="novelTosPath">
          <TosUpload
            v-model="formData.novelTosPath"
            :project-id="0"
            file-type="novel"
            button-text="上传小说"
            tip-text="支持 txt/md/doc/docx/pdf，最大 100MB"
            accept=".txt,.md,.markdown,.doc,.docx,.pdf"
            :allowed-types="['text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/markdown', 'text/x-markdown', 'application/markdown', 'application/pdf']"
            :max-file-size="100 * 1024 * 1024"
            @uploaded="handleNovelUploaded"
          />
        </el-form-item>
        <el-form-item label="默认比例" prop="ratio">
          <el-select v-model="formData.ratio" class="full-width" :disabled="isEdit">
            <el-option v-for="item in PROJECT_RATIO_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认清晰度" prop="definition">
          <el-select v-model="formData.definition" class="full-width" :disabled="isEdit">
            <el-option v-for="item in PROJECT_DEFINITION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="风格" prop="style">
          <el-select v-model="formData.style" class="full-width" :disabled="isEdit">
            <el-option v-for="item in PROJECT_STYLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.style === '自定义'" label="风格描述" prop="styleDesc">
          <el-input v-model="formData.styleDesc" type="textarea" :rows="3" maxlength="300" show-word-limit :disabled="isEdit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deleteDialogVisible" title="删除项目" width="400px">
      <p>确定删除「{{ currentDeleteProject?.name }}」吗？项目删除走软删逻辑。</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import TosUpload from '@/components/Common/TosUpload.vue'
import { PROJECT_DEFINITION_OPTIONS, PROJECT_RATIO_OPTIONS, PROJECT_STYLE_OPTIONS } from '@/constants/options'
import { projectApi } from '@/api/project'
import type { ProjectCreateRequest, ProjectUpdateRequest, ProjectVO } from '@/types'

const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const deleteLoading = ref(false)
const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const formRef = ref<FormInstance>()
const projectList = ref<ProjectVO[]>([])
const currentEditId = ref<number | null>(null)
const currentDeleteProject = ref<ProjectVO | null>(null)

const formData = reactive<ProjectCreateRequest>({
  name: '',
  description: '',
  novelFile: undefined,
  novelTosPath: '',
  ratio: '16:9',
  definition: '1080P',
  style: '2D次元风',
  styleDesc: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  novelTosPath: [{ required: true, message: '请上传小说文件', trigger: 'change' }],
  ratio: [{ required: true, message: '请选择比例', trigger: 'change' }],
  definition: [{ required: true, message: '请选择清晰度', trigger: 'change' }],
  style: [{ required: true, message: '请选择风格', trigger: 'change' }]
}

const filteredList = computed(() => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return projectList.value
  return projectList.value.filter(project => project.name.includes(keyword))
})
const uploadedCount = computed(() => projectList.value.filter(project => Boolean(project.novelTosPath)).length)

const fetchProjectList = async () => {
  loading.value = true
  try {
    const res = await projectApi.list({ page: 1, size: 99, keyword: searchKeyword.value.trim() || undefined })
    projectList.value = res.data.list || res.data.records || []
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formData.name = ''
  formData.description = ''
  formData.novelFile = undefined
  formData.novelTosPath = ''
  formData.ratio = '16:9'
  formData.definition = '1080P'
  formData.style = '2D次元风'
  formData.styleDesc = ''
}

const openCreateDialog = () => {
  isEdit.value = false
  currentEditId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (project: ProjectVO) => {
  isEdit.value = true
  currentEditId.value = project.id
  formData.name = project.name
  formData.description = project.description || ''
  formData.novelTosPath = project.novelTosPath || ''
  formData.novelFile = undefined
  formData.ratio = project.ratio || '16:9'
  formData.definition = project.definition || '1080P'
  formData.style = project.style || '2D次元风'
  formData.styleDesc = project.styleDesc || ''
  dialogVisible.value = true
}

const handleNovelUploaded = (payload: { url: string; key: string; fileName: string; fileSize: number }) => {
  formData.novelTosPath = payload.url
  formData.novelFile = {
    fileName: payload.fileName,
    fileKey: payload.key,
    fileSize: payload.fileSize
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && currentEditId.value) {
      const payload: ProjectUpdateRequest = {
        name: formData.name.trim(),
        description: formData.description?.trim()
      }
      await projectApi.update(currentEditId.value, payload)
      ElMessage.success('项目已更新')
    } else {
      await projectApi.create({
        name: formData.name.trim(),
        description: formData.description?.trim(),
        novelFile: formData.novelFile,
        ratio: formData.ratio,
        definition: formData.definition,
        style: formData.style,
        styleDesc: formData.style === '自定义' ? formData.styleDesc?.trim() : ''
      })
      ElMessage.success('项目已创建')
    }
    dialogVisible.value = false
    await fetchProjectList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (project: ProjectVO) => {
  currentDeleteProject.value = project
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  if (!currentDeleteProject.value) return
  deleteLoading.value = true
  try {
    await projectApi.delete(currentDeleteProject.value.id)
    ElMessage.success('项目已删除')
    deleteDialogVisible.value = false
    await fetchProjectList()
  } finally {
    deleteLoading.value = false
  }
}

const goToDetail = (id: number) => {
  router.push({ name: 'ScriptPreview', params: { id } })
}

const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm')

const getProjectCover = (project: ProjectVO) => {
  const map: Record<string, string> = {
    '日漫风': '/assets/images/project-cover-japanese.png',
    '国漫风': '/assets/images/project-cover-chinese.png',
    '现代写实': '/assets/images/project-cover-scifi.png',
    '2D次元风': '/assets/images/project-cover-japanese.png',
    '古风': '/assets/images/project-cover-chinese.png',
    '自定义': '/assets/images/project-cover-q.png'
  }
  return map[project.style || ''] || '/assets/images/project-cover-q.png'
}

onMounted(fetchProjectList)
</script>

<style scoped lang="scss">
.project-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.project-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;

  h2 {
    margin: 0;
    color: $text-primary;
    font-size: 38px;
  }

  p {
    max-width: 720px;
    margin: 10px 0 0;
    color: $text-secondary;
    line-height: 1.7;
  }
}

.eyebrow {
  margin: 0 0 8px;
  color: $accent-yellow;
  font-size: 12px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, 118px);
  gap: 12px;

  div {
    padding: 16px;
    border: 1px solid rgba(92, 241, 255, 0.18);
    border-radius: 16px;
    background: rgba(92, 241, 255, 0.06);
  }

  span,
  strong {
    display: block;
  }

  span {
    color: $text-tertiary;
    font-size: 12px;
  }

  strong {
    margin-top: 8px;
    color: $text-primary;
    font-family: $font-display;
    font-size: 34px;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px;
}

.search-box {
  max-width: 320px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.project-card {
  overflow: hidden;
  min-height: 374px;
}

.cover {
  position: relative;
  height: 180px;
  background-size: cover;
  background-position: center;
  cursor: pointer;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(180deg, transparent 35%, rgba(3, 7, 13, 0.82)),
      linear-gradient(90deg, rgba(92, 241, 255, 0.18), transparent 42%);
  }
}

.style-tag {
  position: absolute;
  left: 14px;
  top: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(10, 10, 18, 0.72);
  color: #fff;
  font-size: 12px;
  z-index: 1;
}

.launch-tag {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 1;
  color: $accent-green;
  font-family: $font-display;
  font-size: 12px;
  letter-spacing: 0.18em;
}

.card-body {
  padding: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;

  h3 {
    margin: 0;
    color: $text-primary;
    font-family: $font-display;
    font-size: 22px;
  }

  p {
    margin: 6px 0 0;
    color: $text-tertiary;
    font-size: 12px;
  }
}

.description {
  min-height: 46px;
  margin: 14px 0;
  color: $text-secondary;
  line-height: 1.6;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    padding: 6px 10px;
    border-radius: 10px;
    border: 1px solid rgba(92, 241, 255, 0.16);
    background: rgba(92, 241, 255, 0.06);
    color: $text-secondary;
    font-size: 12px;
  }
}

@media (max-width: 840px) {
  .project-hero {
    flex-direction: column;
  }

  .hero-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    max-width: none;
  }
}

.actions {
  display: flex;
  align-items: flex-start;
}

.full-width {
  width: 100%;
}

@media (max-width: 720px) {
  .toolbar {
    flex-direction: column;
  }

  .search-box {
    max-width: none;
  }
}
</style>

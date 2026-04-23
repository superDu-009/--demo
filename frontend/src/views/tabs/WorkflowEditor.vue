<template>
  <div class="workflow-editor-page">
    <div class="top-bar card-glass border-neon">
      <div>
        <p class="eyebrow">Pipeline Builder</p>
        <h3>工作流配置</h3>
        <span>拖拽调整步骤顺序，保存结构后可直接替换为真实接口执行。</span>
      </div>
      <div class="bar-actions">
        <el-button :loading="saving" @click="saveWorkflow">保存配置</el-button>
        <el-button class="btn-gradient" :loading="starting" :disabled="isRunning" @click="startWorkflow">
          开始执行
        </el-button>
        <el-button type="danger" :disabled="!isRunning" @click="stopWorkflow">终止</el-button>
      </div>
    </div>

    <WorkflowProgress
      :running="isRunning"
      :progress="workflowStatus.overallProgress"
      :current-detail="currentDetail"
      :estimated-remaining-seconds="workflowStatus.estimatedRemainingSeconds"
    />

    <div class="editor-grid">
      <section class="step-section">
        <draggable
          v-model="workflowSteps"
          item-key="stepType"
          handle=".drag-handle"
          ghost-class="sortable-ghost"
          chosen-class="sortable-chosen"
          :animation="180"
          :disabled="isRunning"
        >
          <template #item="{ element, index }">
            <WorkflowNode
              :model-value="element"
              :meta="getStepMeta(element.stepType)"
              :disabled="isRunning"
              @update:model-value="updateStep(index, $event)"
            />
          </template>
        </draggable>
      </section>

      <aside class="status-section card-glass border-neon">
        <div class="status-head">
          <p class="eyebrow">Status</p>
          <h4>执行状态</h4>
        </div>
        <WorkflowStatus
          :steps="workflowStatus.steps"
          :step-name-map="stepNameMap"
          @review="reviewWorkflowStep"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import draggable from 'vuedraggable'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload, Search, Edit, Picture, Film, Folder
} from '@element-plus/icons-vue'
import WorkflowNode from '@/components/Workflow/WorkflowNode.vue'
import WorkflowProgress from '@/components/Workflow/WorkflowProgress.vue'
import WorkflowStatus from '@/components/Workflow/WorkflowStatus.vue'
import { projectApi } from '@/api/project'
import { StepType, WorkflowTaskStatus } from '@/types'
import type { WorkflowStep, WorkflowStatusVO } from '@/types'

const stepMeta = {
  import: {
    name: '导入并拆分剧本',
    description: '解析上传的小说文件，自动拆分章节和场景。',
    icon: Upload
  },
  asset_extract: {
    name: '资产提取',
    description: 'AI 自动提取角色、场景、道具与声音资产。',
    icon: Search
  },
  shot_gen: {
    name: '分镜提示词生成',
    description: '根据分场内容生成中英文分镜提示词。',
    icon: Edit,
    configKey: 'stylePrompt',
    configPlaceholder: '例如：动漫风格，强对比霓虹光'
  },
  image_gen: {
    name: '首帧生图',
    description: '根据提示词和资产参考图生成分镜首帧。',
    icon: Picture,
    configKey: 'imageModel',
    configPlaceholder: '例如：SD3 / Seedream'
  },
  video_gen: {
    name: '视频生成',
    description: '基于首帧生成短视频镜头。',
    icon: Film,
    configKey: 'videoModel',
    configPlaceholder: '例如：Seedance，24fps'
  },
  export: {
    name: '合并导出',
    description: '合并已通过镜头并生成最终成片。',
    icon: Folder
  }
} satisfies Record<StepType, any>

const stepNameMap = Object.entries(stepMeta).reduce<Record<string, string>>((result, [key, value]) => {
  result[key] = value.name
  return result
}, {})

const getStepMeta = (stepType: StepType) => stepMeta[stepType]
const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const workflowSteps = ref<WorkflowStep[]>([
  { stepType: 'import', enabled: true, review: false, config: {} },
  { stepType: 'asset_extract', enabled: true, review: true, config: {} },
  { stepType: 'shot_gen', enabled: true, review: true, config: { stylePrompt: '动漫风格，赛博朋克背景' } },
  { stepType: 'image_gen', enabled: true, review: true, config: { imageModel: 'Seedream，768x1344' } },
  { stepType: 'video_gen', enabled: true, review: true, config: { videoModel: 'Seedance，24fps' } },
  { stepType: 'export', enabled: true, review: false, config: {} }
])

const saving = ref(false)
const starting = ref(false)
const isRunning = ref(false)
let runTimer: number | null = null
const projectVersion = ref(0)

const workflowStatus = reactive<WorkflowStatusVO>({
  executionLock: 0,
  currentStep: null,
  currentEpisodeId: null,
  currentEpisodeTitle: null,
  totalEpisodes: 0,
  overallProgress: 0,
  totalShots: 0,
  processedShots: 0,
  estimatedRemainingSeconds: 0,
  steps: workflowSteps.value.map((step, index) => ({
    stepType: step.stepType,
    stepOrder: index + 1,
    status: WorkflowTaskStatus.NotStarted,
    progress: 0,
    currentDetail: '未执行',
    errorMsg: null,
    reviewComment: null
  }))
})

const currentDetail = computed(() => {
  const current = workflowStatus.steps.find(step => step.status === WorkflowTaskStatus.Running)
  return current?.currentDetail || (isRunning.value ? '准备执行' : '等待执行')
})

const applyWorkflowStatus = (status: WorkflowStatusVO) => {
  workflowStatus.executionLock = status.executionLock
  workflowStatus.currentStep = status.currentStep
  workflowStatus.currentEpisodeId = status.currentEpisodeId
  workflowStatus.currentEpisodeTitle = status.currentEpisodeTitle
  workflowStatus.totalEpisodes = status.totalEpisodes
  workflowStatus.overallProgress = status.overallProgress
  workflowStatus.totalShots = status.totalShots
  workflowStatus.processedShots = status.processedShots
  workflowStatus.estimatedRemainingSeconds = status.estimatedRemainingSeconds
  workflowStatus.steps = status.steps || []
  isRunning.value = status.executionLock === 1
}

const syncStatusSteps = () => {
  workflowStatus.steps = workflowSteps.value.map((step, index) => {
    const old = workflowStatus.steps.find(item => item.stepType === step.stepType)
    return {
      stepType: step.stepType,
      stepOrder: index + 1,
      status: old?.status || WorkflowTaskStatus.NotStarted,
      progress: old?.progress || 0,
      currentDetail: old?.currentDetail || '未执行',
      errorMsg: old?.errorMsg || null,
      reviewComment: old?.reviewComment || null
    }
  })
}

const updateStep = (index: number, step: WorkflowStep) => {
  workflowSteps.value[index] = step
  syncStatusSteps()
}

const saveWorkflow = async () => {
  if (!projectId.value) return
  saving.value = true
  try {
    await projectApi.saveWorkflow(projectId.value, {
      version: projectVersion.value,
      workflowConfig: { steps: workflowSteps.value },
      stylePreset: {}
    })
    ElMessage.success('配置已保存')
    await loadProjectWorkflow()
  } finally {
    saving.value = false
  }
}

const startWorkflow = async () => {
  if (!projectId.value) return
  starting.value = true
  try {
    await projectApi.startWorkflow(projectId.value)
    ElMessage.success('工作流已开始执行')
    await fetchWorkflowStatus()
    startPolling()
  } finally {
    starting.value = false
  }
}

const stopPolling = () => {
  if (runTimer) {
    window.clearInterval(runTimer)
    runTimer = null
  }
}

const stopWorkflow = async () => {
  if (!projectId.value) return
  stopPolling()
  await projectApi.stopWorkflow(projectId.value)
  isRunning.value = false
  await fetchWorkflowStatus()
  ElMessage.info('工作流已终止')
}

const fetchWorkflowStatus = async () => {
  if (!projectId.value) return
  const res = await projectApi.getWorkflowStatus(projectId.value)
  applyWorkflowStatus(res.data)
  if (res.data.executionLock === 0) stopPolling()
}

const startPolling = () => {
  stopPolling()
  runTimer = window.setInterval(fetchWorkflowStatus, 3000)
}

const reviewWorkflowStep = async (stepType: string, action: 'approve' | 'reject') => {
  if (!projectId.value) return
  let comment = ''
  if (action === 'reject') {
    const result = await ElMessageBox.prompt('请输入打回原因', '打回流程步骤', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: value => !!value.trim() || '打回原因不能为空'
    })
    comment = result.value
  }
  await projectApi.reviewWorkflow(projectId.value, { stepType, action, comment })
  ElMessage.success(action === 'approve' ? '步骤已通过' : '步骤已打回')
  await fetchWorkflowStatus()
  if (isRunning.value) startPolling()
}

const loadProjectWorkflow = async () => {
  if (!projectId.value) return
  const res = await projectApi.getDetail(projectId.value)
  projectVersion.value = res.data.version || 0
  if (res.data.workflowConfig?.steps?.length) {
    workflowSteps.value = res.data.workflowConfig.steps
    syncStatusSteps()
  }
  await fetchWorkflowStatus().catch(() => undefined)
  if (isRunning.value) startPolling()
}

onMounted(loadProjectWorkflow)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
.workflow-editor-page {
  width: 100%;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 20px;
  margin-bottom: 18px;
  border-radius: 8px;

  .eyebrow {
    margin: 0 0 6px;
    color: $accent-green;
    font-size: 12px;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  h3 {
    margin: 0;
    color: $text-primary;
    font-size: 20px;
  }

  span {
    display: block;
    margin-top: 8px;
    color: $text-secondary;
    font-size: 13px;
  }
}

.bar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.editor-grid {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.step-section {
  display: flex;
  flex-direction: column;
  gap: 12px;

  :deep(.workflow-node) {
    margin-bottom: 12px;
  }
}

.status-section {
  padding: 18px;
  border-radius: 8px;
}

.status-head {
  margin-bottom: 14px;

  .eyebrow {
    margin: 0 0 6px;
    color: $accent-green;
    font-size: 12px;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  h4 {
    margin: 0;
    color: $text-primary;
    font-size: 17px;
  }
}

:deep(.sortable-ghost) {
  opacity: 0.35;
}

:deep(.sortable-chosen) {
  border-color: rgba(34, 211, 238, 0.72);
}

@media (max-width: 768px) {
  .top-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

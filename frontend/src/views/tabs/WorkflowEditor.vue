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
        <WorkflowStatus :steps="workflowStatus.steps" :step-name-map="stepNameMap" />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import draggable from 'vuedraggable'
import { ElMessage } from 'element-plus'
import {
  Upload, Search, Edit, Picture, Film, Folder
} from '@element-plus/icons-vue'
import WorkflowNode from '@/components/Workflow/WorkflowNode.vue'
import WorkflowProgress from '@/components/Workflow/WorkflowProgress.vue'
import WorkflowStatus from '@/components/Workflow/WorkflowStatus.vue'
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
  saving.value = true
  try {
    // 这里先保留前端 mock 保存，后续可替换为 projectApi.saveWorkflow(projectId, payload)。
    const payload = {
      workflowConfig: { steps: workflowSteps.value },
      stylePreset: {}
    }
    console.info('[mock save workflow]', payload)
    await new Promise(resolve => window.setTimeout(resolve, 500))
    ElMessage.success('配置已保存')
  } finally {
    saving.value = false
  }
}

const startWorkflow = async () => {
  starting.value = true
  try {
    await new Promise(resolve => window.setTimeout(resolve, 400))
    isRunning.value = true
    workflowStatus.executionLock = 1
    workflowStatus.overallProgress = 0
    workflowStatus.estimatedRemainingSeconds = 900
    syncStatusSteps()
    workflowStatus.steps.forEach(step => {
      step.status = WorkflowTaskStatus.NotStarted
      step.progress = 0
      step.currentDetail = '等待执行'
    })
    runMockExecution()
    ElMessage.success('工作流已开始执行')
  } finally {
    starting.value = false
  }
}

const stopWorkflow = () => {
  if (runTimer) {
    window.clearInterval(runTimer)
    runTimer = null
  }
  isRunning.value = false
  workflowStatus.executionLock = 0
  workflowStatus.currentStep = null
  workflowStatus.estimatedRemainingSeconds = 0
  workflowStatus.steps.forEach(step => {
    if (step.status === WorkflowTaskStatus.Running) {
      step.status = WorkflowTaskStatus.NotStarted
      step.currentDetail = '已终止'
    }
  })
  ElMessage.info('工作流已终止')
}

const runMockExecution = () => {
  const enabledSteps = workflowSteps.value.filter(step => step.enabled)
  let activeIndex = 0
  let activeProgress = 0

  runTimer = window.setInterval(() => {
    const active = enabledSteps[activeIndex]
    if (!active) {
      stopWorkflow()
      workflowStatus.overallProgress = 100
      workflowStatus.steps.forEach(step => {
        if (step.status !== WorkflowTaskStatus.Success && workflowSteps.value.find(item => item.stepType === step.stepType)?.enabled) {
          step.status = WorkflowTaskStatus.Success
          step.progress = 100
          step.currentDetail = '执行成功'
        }
      })
      ElMessage.success('工作流执行完成')
      return
    }

    const statusStep = workflowStatus.steps.find(step => step.stepType === active.stepType)
    if (!statusStep) return

    statusStep.status = WorkflowTaskStatus.Running
    statusStep.currentDetail = `${stepNameMap[active.stepType]} - mock 执行中`
    activeProgress += 10
    statusStep.progress = Math.min(activeProgress, 100)
    workflowStatus.currentStep = active.stepType
    workflowStatus.overallProgress = Math.min(
      100,
      Math.round(((activeIndex + activeProgress / 100) / enabledSteps.length) * 100)
    )
    workflowStatus.estimatedRemainingSeconds = Math.max(0, workflowStatus.estimatedRemainingSeconds - 30)

    if (activeProgress >= 100) {
      statusStep.status = active.review ? WorkflowTaskStatus.WaitingReview : WorkflowTaskStatus.Success
      statusStep.currentDetail = active.review ? '待人工审核' : '执行成功'
      statusStep.progress = 100
      activeIndex += 1
      activeProgress = 0
    }
  }, 500)
}

onBeforeUnmount(() => {
  if (runTimer) window.clearInterval(runTimer)
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

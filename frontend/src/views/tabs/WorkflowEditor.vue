<!-- views/tabs/WorkflowEditor.vue — 流程编辑器页面 -->
<template>
  <div class="workflow-editor-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar card-glass">
      <div class="bar-left">
        <h3 class="bar-title">工作流配置</h3>
        <el-tag type="warning" v-if="isRunning">执行中</el-tag>
        <el-tag type="success" v-else>可编辑</el-tag>
      </div>
      <div class="bar-right">
        <el-button @click="saveWorkflow" :loading="saving">保存配置</el-button>
        <el-button class="btn-gradient" @click="startWorkflow" :loading="starting" :disabled="isRunning">
          开始执行
        </el-button>
        <el-button type="danger" @click="stopWorkflow" :disabled="!isRunning">终止</el-button>
      </div>
    </div>

    <!-- 执行进度面板 -->
    <div class="progress-panel card-glass" v-if="isRunning">
      <div class="progress-header">
        <h4>执行进度</h4>
        <span class="progress-text">{{ overallProgress }}%</span>
      </div>
      <el-progress :percentage="overallProgress" :show-text="false" :stroke-width="12" />
      <p class="progress-desc">{{ currentStepDesc }}</p>
    </div>

    <!-- 步骤卡片列表 -->
    <div class="step-list">
      <div 
        v-for="(step, index) in workflowSteps" 
        :key="step.id"
        class="step-card card-glass"
        :class="{ 'step-running': step.status === 'running', 'step-success': step.status === 'success', 'step-error': step.status === 'error', 'step-disabled': !step.enabled }"
      >
        <div class="step-header">
          <div class="step-info">
            <el-icon :size="24" class="step-icon"><component :is="step.icon" /></el-icon>
            <div>
              <h4 class="step-name">{{ step.name }}</h4>
              <p class="step-desc">{{ step.description }}</p>
            </div>
          </div>
          <div class="step-actions">
            <el-switch v-model="step.enabled" :disabled="isRunning" />
          </div>
        </div>

        <div class="step-config" v-if="step.configurable && step.enabled">
          <el-form label-width="100px">
            <el-form-item label="需要审核">
              <el-switch v-model="step.needReview" :disabled="isRunning" />
            </el-form-item>
            <el-form-item v-if="step.extraConfig" label="配置参数">
              <el-input v-model="step.configValue" placeholder="请输入配置参数" :disabled="isRunning" />
            </el-form-item>
          </el-form>
        </div>

        <!-- 执行状态 -->
        <div class="step-status" v-if="step.status === 'running'">
          <span class="running-text">执行中...</span>
          <el-progress type="circle" :percentage="step.progress" :width="32" />
        </div>
        <div class="step-status success" v-if="step.status === 'success'">
          <el-icon :size="20" class="success-icon"><Check /></el-icon>
          <span>执行成功</span>
        </div>
        <div class="step-status error" v-if="step.status === 'error'">
          <el-icon :size="20" class="error-icon"><Close /></el-icon>
          <span>执行失败：{{ step.errorMsg }}</span>
        </div>

        <!-- 拖拽手柄 -->
        <div class="drag-handle" v-if="!isRunning">
          <el-icon :size="20"><Rank /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Upload, Search, Edit, Picture, Film, Folder, Rank, Check, Close
} from '@element-plus/icons-vue'

// 工作流步骤数据
const workflowSteps = reactive([
  {
    id: 1,
    name: '导入并拆分剧本',
    description: '解析上传的小说文件，自动拆分章节和场景',
    icon: Upload,
    enabled: true,
    configurable: true,
    needReview: false,
    extraConfig: false,
    configValue: '',
    status: '', // running / success / error
    progress: 0,
    errorMsg: ''
  },
  {
    id: 2,
    name: '资产提取',
    description: 'AI自动提取小说中的角色、场景、道具等资产',
    icon: Search,
    enabled: true,
    configurable: true,
    needReview: true,
    extraConfig: false,
    configValue: '',
    status: '',
    progress: 0,
    errorMsg: ''
  },
  {
    id: 3,
    name: '分镜提示词生成',
    description: '根据场景内容生成中英文AI绘画提示词',
    icon: Edit,
    enabled: true,
    configurable: true,
    needReview: true,
    extraConfig: true,
    configValue: '动漫风格，赛博朋克背景',
    status: '',
    progress: 0,
    errorMsg: ''
  },
  {
    id: 4,
    name: '图片生成',
    description: '调用AI绘画接口批量生成所有分镜图片',
    icon: Picture,
    enabled: true,
    configurable: true,
    needReview: true,
    extraConfig: true,
    configValue: 'SD3模型，768*1344分辨率',
    status: '',
    progress: 0,
    errorMsg: ''
  },
  {
    id: 5,
    name: '视频生成',
    description: '根据分镜图片生成连贯的动画视频',
    icon: Film,
    enabled: true,
    configurable: true,
    needReview: true,
    extraConfig: true,
    configValue: '24fps，镜头平移',
    status: '',
    progress: 0,
    errorMsg: ''
  },
  {
    id: 6,
    name: '合并导出',
    description: '把所有分镜视频合并成完整的动画作品',
    icon: Folder,
    enabled: true,
    configurable: true,
    needReview: false,
    extraConfig: false,
    configValue: '',
    status: '',
    progress: 0,
    errorMsg: ''
  }
])

// 状态
const isRunning = ref(false)
const saving = ref(false)
const starting = ref(false)
const overallProgress = ref(0)
const currentStepDesc = ref('等待执行...')

// 保存配置
const saveWorkflow = async () => {
  saving.value = true
  try {
    // 模拟保存请求
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('配置保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 开始执行
const startWorkflow = async () => {
  starting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    isRunning.value = true
    ElMessage.success('工作流开始执行')
    // 模拟执行进度
    simulateExecution()
  } catch {
    ElMessage.error('启动失败')
  } finally {
    starting.value = false
  }
}

// 终止执行
const stopWorkflow = () => {
  isRunning.value = false
  workflowSteps.forEach(step => {
    step.status = ''
    step.progress = 0
  })
  overallProgress.value = 0
  currentStepDesc.value = '已终止'
  ElMessage.info('工作流已终止')
}

// 模拟执行流程
const simulateExecution = () => {
  let currentStepIndex = 0
  const stepCount = workflowSteps.filter(s => s.enabled).length

  const executeNextStep = () => {
    if (!isRunning.value || currentStepIndex >= workflowSteps.length) return
    
    const step = workflowSteps[currentStepIndex]
    if (!step.enabled) {
      currentStepIndex++
      executeNextStep()
      return
    }

    step.status = 'running'
    currentStepDesc.value = `正在执行：${step.name}`
    let progress = 0

    const interval = setInterval(() => {
      if (!isRunning.value) {
        clearInterval(interval)
        return
      }
      progress += 10
      step.progress = progress
      overallProgress.value = Math.round(((currentStepIndex + progress/100) / stepCount) * 100)

      if (progress >= 100) {
        clearInterval(interval)
        step.status = 'success'
        currentStepIndex++
        if (currentStepIndex >= stepCount) {
          overallProgress.value = 100
          currentStepDesc.value = '全部执行完成！'
          isRunning.value = false
          ElMessage.success('工作流执行完成')
        } else {
          executeNextStep()
        }
      }
    }, 300)
  }

  executeNextStep()
}
</script>

<style scoped lang="scss">
.workflow-editor-page {
  width: 100%;
}

// 顶部操作栏
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  margin-bottom: 20px;

  .bar-left {
    display: flex;
    align-items: center;
    gap: 12px;
    .bar-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      color: $text-primary;
    }
  }

  .bar-right {
    display: flex;
    gap: 12px;
  }
}

// 进度面板
.progress-panel {
  padding: 20px;
  margin-bottom: 20px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    h4 {
      font-size: 16px;
      font-weight: 600;
      margin: 0;
      color: $text-primary;
    }
    .progress-text {
      font-weight: 600;
      color: $primary-color;
    }
  }

  .progress-desc {
    margin: 12px 0 0 0;
    color: $text-secondary;
    font-size: 14px;
  }

  :deep(.el-progress-bar__outer) {
    background: rgba(100, 108, 255, 0.1);
    border-radius: 6px;
  }

  :deep(.el-progress-bar__inner) {
    background: $primary-gradient;
    border-radius: 6px;
  }
}

// 步骤列表
.step-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-card {
  padding: 20px;
  position: relative;
  transition: all 0.3s ease;

  &.step-running {
    border-color: $primary-color;
    box-shadow: 0 0 20px rgba(100, 108, 255, 0.3);
    animation: pulse 2s infinite;
  }

  &.step-success {
    border-color: #10b981;
    box-shadow: 0 0 20px rgba(16, 185, 129, 0.2);
  }

  &.step-error {
    border-color: #ef4444;
    box-shadow: 0 0 20px rgba(239, 68, 68, 0.2);
  }

  &.step-disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .step-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .step-info {
      display: flex;
      align-items: center;
      gap: 12px;
      .step-icon {
        background: $primary-gradient;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
      .step-name {
        font-size: 16px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 4px 0;
      }
      .step-desc {
        font-size: 13px;
        color: $text-secondary;
        margin: 0;
      }
    }
  }

  .step-config {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid rgba(100, 108, 255, 0.1);
  }

  .step-status {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid rgba(100, 108, 255, 0.1);
    color: $text-secondary;
    font-size: 14px;

    &.success {
      color: #10b981;
      .success-icon {
        color: #10b981;
      }
    }

    &.error {
      color: #ef4444;
      .error-icon {
        color: #ef4444;
      }
    }
  }

  .drag-handle {
    position: absolute;
    right: 20px;
    top: 50%;
    transform: translateY(-50%);
    cursor: grab;
    color: $text-tertiary;
    opacity: 0.5;
    transition: opacity 0.3s ease;

    &:hover {
      opacity: 1;
      color: $primary-color;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 20px rgba(100, 108, 255, 0.3), 0 8px 32px rgba(0, 0, 0, 0.3);
  }
  50% {
    box-shadow: 0 0 30px rgba(100, 108, 255, 0.5), 0 8px 32px rgba(0, 0, 0, 0.3);
  }
}
</style>
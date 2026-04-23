<template>
  <div class="workflow-status">
    <div
      v-for="step in steps"
      :key="step.stepType"
      class="status-row"
    >
      <span class="status-dot" :style="{ backgroundColor: getStatusMeta(step.status).color }" />
      <div class="status-info">
        <strong>{{ stepNameMap[step.stepType] || step.stepType }}</strong>
        <span>{{ step.currentDetail || getStatusMeta(step.status).label }}</span>
      </div>
      <el-progress
        class="row-progress"
        :percentage="step.progress || 0"
        :show-text="false"
        :stroke-width="6"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { WORKFLOW_TASK_STATUS_MAP } from '@/constants/status'
import { WorkflowTaskStatus } from '@/types'
import type { WorkflowStepStatus } from '@/types'

defineProps<{
  steps: WorkflowStepStatus[]
  stepNameMap: Record<string, string>
}>()

const fallbackStatus = { label: '未执行', color: '#909399' }

const getStatusMeta = (status: number) => {
  return WORKFLOW_TASK_STATUS_MAP[status as WorkflowTaskStatus] || fallbackStatus
}
</script>

<style scoped lang="scss">
.workflow-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-row {
  display: grid;
  grid-template-columns: 12px minmax(160px, 1fr) 160px;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(100, 108, 255, 0.14);
  border-radius: 8px;
  background: rgba(100, 108, 255, 0.04);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: $text-primary;
    font-size: 13px;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .status-row {
    grid-template-columns: 12px 1fr;
  }

  .row-progress {
    grid-column: 2;
    width: 100%;
  }
}
</style>

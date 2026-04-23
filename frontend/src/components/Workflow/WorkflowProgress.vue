<template>
  <div class="workflow-progress card-glass border-neon">
    <div class="progress-head">
      <div>
        <p class="eyebrow">Execution</p>
        <h4>{{ running ? '流程执行中' : '流程未运行' }}</h4>
      </div>
      <span class="progress-value">{{ progress }}%</span>
    </div>
    <el-progress :percentage="progress" :show-text="false" :stroke-width="12" />
    <div class="progress-meta">
      <span>当前：{{ currentDetail || '等待执行' }}</span>
      <span>预计剩余：{{ remainingText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  running: boolean
  progress: number
  currentDetail?: string
  estimatedRemainingSeconds?: number
}>()

const remainingText = computed(() => {
  const seconds = props.estimatedRemainingSeconds || 0
  if (!props.running) return '-'
  if (seconds <= 0) return '计算中'
  const minutes = Math.ceil(seconds / 60)
  return `约 ${minutes} 分钟`
})
</script>

<style scoped lang="scss">
.workflow-progress {
  padding: 18px;
  margin-bottom: 18px;
  border-radius: 8px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

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

.progress-value {
  color: $accent-green;
  font-size: 28px;
  font-weight: 700;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-top: 12px;
  color: $text-secondary;
  font-size: 13px;
}

@media (max-width: 768px) {
  .progress-meta {
    flex-direction: column;
  }
}
</style>

<template>
  <div class="workflow-node card-glass border-neon" :class="{ disabled: !modelValue.enabled }">
    <div class="drag-handle">
      <el-icon><Rank /></el-icon>
    </div>
    <div class="node-main">
      <div class="node-head">
        <div class="node-title">
          <el-icon :size="24"><component :is="meta.icon" /></el-icon>
          <div>
            <h4>{{ meta.name }}</h4>
            <p>{{ meta.description }}</p>
          </div>
        </div>
        <el-tag :type="modelValue.enabled ? 'success' : 'info'" size="small">
          {{ modelValue.enabled ? '启用' : '停用' }}
        </el-tag>
      </div>

      <div class="node-controls">
        <label>
          <span>启用步骤</span>
          <el-switch v-model="localStep.enabled" :disabled="disabled" @change="emitUpdate" />
        </label>
        <label>
          <span>需要人工审核</span>
          <el-switch v-model="localStep.review" :disabled="disabled || !localStep.enabled" @change="emitUpdate" />
        </label>
        <el-input
          v-if="meta.configKey"
          v-model="configValue"
          :disabled="disabled || !localStep.enabled"
          :placeholder="meta.configPlaceholder"
          @change="emitUpdate"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { Rank } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { WorkflowStep } from '@/types'

const props = defineProps<{
  modelValue: WorkflowStep
  meta: {
    name: string
    description: string
    icon: Component
    configKey?: string
    configPlaceholder?: string
  }
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: WorkflowStep]
}>()

const localStep = reactive<WorkflowStep>({
  stepType: props.modelValue.stepType,
  enabled: props.modelValue.enabled,
  review: props.modelValue.review,
  config: { ...(props.modelValue.config || {}) }
})

watch(() => props.modelValue, (value) => {
  localStep.stepType = value.stepType
  localStep.enabled = value.enabled
  localStep.review = value.review
  localStep.config = { ...(value.config || {}) }
}, { deep: true })

const configValue = computed({
  get: () => props.meta.configKey ? String(localStep.config?.[props.meta.configKey] || '') : '',
  set: (value) => {
    if (!props.meta.configKey) return
    localStep.config = {
      ...(localStep.config || {}),
      [props.meta.configKey]: value
    }
  }
})

const emitUpdate = () => {
  emit('update:modelValue', {
    stepType: localStep.stepType,
    enabled: localStep.enabled,
    review: localStep.review,
    config: { ...(localStep.config || {}) }
  })
}
</script>

<style scoped lang="scss">
.workflow-node {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  transition: opacity 0.2s ease, border-color 0.2s ease;

  &.disabled {
    opacity: 0.62;
  }
}

.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-tertiary;
  cursor: grab;
}

.node-main {
  min-width: 0;
}

.node-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.node-title {
  display: flex;
  gap: 12px;
  min-width: 0;

  h4 {
    margin: 0 0 4px;
    color: $text-primary;
    font-size: 16px;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.node-controls {
  display: grid;
  grid-template-columns: 130px 160px minmax(180px, 1fr);
  align-items: center;
  gap: 12px;
  margin-top: 14px;

  label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    color: $text-secondary;
    font-size: 13px;
  }
}

@media (max-width: 900px) {
  .node-controls {
    grid-template-columns: 1fr;
  }
}
</style>

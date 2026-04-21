// stores/workflow.ts — 系分第 7 节：流程状态 Store（v1.1 修订：轮询逻辑移至 composable）

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { projectApi } from '@/api/project'
import type { WorkflowConfig, WorkflowStatusVO, StepType } from '@/types'
import { useWorkflowPolling } from '@/composables/useWorkflowPolling'
import { useProjectStore } from '@/stores/project'

export const useWorkflowStore = defineStore('workflow', () => {
  const config = ref<WorkflowConfig>({ steps: [] })
  const status = ref<WorkflowStatusVO | null>(null)
  // 计算属性：流程是否正在执行中
  const isRunning = computed(() => status.value?.executionLock === 1)

  // v1.1：轮询逻辑移至 composable，store 不再直接管理定时器
  const { startPolling, stopPolling, pollingStatus } = useWorkflowPolling()

  // 默认步骤配置
  const defaultSteps: { stepType: StepType; enabled: boolean; review: boolean }[] = [
    { stepType: 'import', enabled: true, review: false },
    { stepType: 'asset_extract', enabled: true, review: true },
    { stepType: 'shot_gen', enabled: true, review: true },
    { stepType: 'image_gen', enabled: true, review: true },
    { stepType: 'video_gen', enabled: true, review: true },
    { stepType: 'export', enabled: true, review: false }
  ]

  // 初始化默认流程配置
  function initConfig() {
    config.value = {
      steps: defaultSteps.map(s => ({ ...s, config: {} }))
    }
  }

  // 保存流程配置
  async function saveConfig(projectId: number) {
    const version = useProjectStore().currentProject?.version || 0
    await projectApi.saveWorkflow(projectId, {
      version,
      workflowConfig: config.value,
      stylePreset: useProjectStore().currentProject?.stylePreset || undefined
    })
    await useProjectStore().fetchProjectDetail(projectId)
  }

  // 开始执行流程
  async function startExecution(projectId: number) {
    await projectApi.startWorkflow(projectId)
    // v1.1：通过 composable 启动轮询
    startPolling(projectId, {
      onStatusUpdate: (s) => { status.value = s }
    })
  }

  // 审核流程步骤
  async function reviewStep(projectId: number, stepType: string, action: 'approve' | 'reject', comment?: string) {
    await projectApi.reviewWorkflow(projectId, { stepType, action, comment })
    // 审核后继续轮询
    startPolling(projectId, {
      onStatusUpdate: (s) => { status.value = s }
    })
  }

  // 停止执行流程
  async function stopExecution(projectId: number) {
    await projectApi.stopWorkflow(projectId)
    stopPolling()
  }

  return {
    config, status, isRunning,
    initConfig, saveConfig, startExecution, reviewStep, stopExecution,
    startPolling, stopPolling, pollingStatus
  }
})

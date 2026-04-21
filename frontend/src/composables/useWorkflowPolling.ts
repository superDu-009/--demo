// composables/useWorkflowPolling.ts — 系分第 8.2 节：工作流轮询（三档退避，v1.1 新增）

import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'
import type { WorkflowStatusVO } from '@/types'

// v1.1：三档退避（与 useTaskPolling 保持一致）
function getPollInterval(count: number): number {
  if (count < 3) return 3000
  if (count < 10) return 10000
  return 30000
}

export interface WorkflowPollingOptions {
  onStatusUpdate?: (status: WorkflowStatusVO) => void
}

export function useWorkflowPolling() {
  const pollingStatus = ref<'idle' | 'polling' | 'timeout'>('idle')
  let timer: number | null = null
  let pollCount = 0
  const maxWorkflowPolls = 2400  // 30s × 2400 = 2h，对齐 Redis 锁 TTL

  // 启动轮询
  function startPolling(projectId: number, options?: WorkflowPollingOptions) {
    stopPolling()
    pollCount = 0
    pollingStatus.value = 'polling'

    const poll = async () => {
      // v1.1：超时保护
      if (pollCount >= maxWorkflowPolls) {
        ElMessage.warning('流程执行超时，请刷新页面查看状态')
        pollingStatus.value = 'timeout'
        stopPolling()
        return
      }

      try {
        const res = await projectApi.getWorkflowStatus(projectId)
        options?.onStatusUpdate?.(res.data)

        // 如果不再执行中，停止轮询
        if (res.data.executionLock === 0) {
          pollingStatus.value = 'idle'
          stopPolling()
          return
        }
      } catch (error) {
        console.error('Workflow polling error:', error)
      }

      pollCount++
      // v1.1：三档退避
      const interval = getPollInterval(pollCount)
      timer = window.setTimeout(poll, interval)
    }

    poll()
  }

  // 停止轮询
  function stopPolling() {
    pollingStatus.value = 'idle'
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  return { pollingStatus, startPolling, stopPolling }
}

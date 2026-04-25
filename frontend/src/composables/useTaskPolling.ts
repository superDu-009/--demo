import { ref, onUnmounted } from 'vue'
import { taskApi } from '@/api/task'
import { AiTaskStatus, type TaskVO } from '@/types'

// v1.1：三档退避间隔计算（前3次 3s，3-10次 10s，10次后 30s）
function getPollInterval(count: number): number {
  if (count < 3) return 3000    // 前3次：3s 间隔（即时反馈）
  if (count < 10) return 10000  // 3-10次：10s 间隔
  return 30000                   // 10次以后：30s 封顶
}

export interface PollingOptions {
  onProgress?: (task: TaskVO) => void
  onDone?: (task: TaskVO) => void
}

export function useTaskPolling() {
  const task = ref<TaskVO | null>(null)
  const isPolling = ref(false)
  let timer: number | null = null
  let pollCount = 0
  const maxPolls = 2400

  async function startPolling(taskId: number, options?: PollingOptions) {
    stopPolling()
    isPolling.value = true
    pollCount = 0

    const poll = async () => {
      if (!isPolling.value || pollCount >= maxPolls) {
        if (pollCount >= maxPolls) {
          console.warn(`[TaskPolling] 轮询次数已达上限 (${maxPolls})，已停止`)
        }
        stopPolling()
        return
      }

      try {
        const res = await taskApi.getStatus(taskId)
        task.value = res.data
        options?.onProgress?.(res.data)

        if (res.data.status === AiTaskStatus.Success || res.data.status === AiTaskStatus.Failed) {
          stopPolling()
          options?.onDone?.(res.data)
          return
        }
      } catch (error) {
        console.error('Task polling error:', error)
      }

      pollCount++
      const interval = getPollInterval(pollCount)
      timer = window.setTimeout(poll, interval)
    }

    poll()
  }

  // 停止轮询
  function stopPolling() {
    isPolling.value = false
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  // 组件卸载时自动清理
  onUnmounted(() => {
    stopPolling()
  })

  return { task, isPolling, startPolling, stopPolling }
}

// composables/useTaskPolling.ts — 系分第 8.1 节：AI 任务轮询（三档退避）

import { ref, onUnmounted } from 'vue'
import { aiApi } from '@/api/ai'
import type { AiTaskVO } from '@/types'

// v1.1：三档退避间隔计算（前3次 3s，3-10次 10s，10次后 30s）
function getPollInterval(count: number): number {
  if (count < 3) return 3000    // 前3次：3s 间隔（即时反馈）
  if (count < 10) return 10000  // 3-10次：10s 间隔
  return 30000                   // 10次以后：30s 封顶
}

export interface PollingOptions {
  // v1.1：任务完成或失败时触发，由调用方根据 task.status 自行处理
  onDone?: (task: AiTaskVO) => void
}

export function useTaskPolling() {
  const task = ref<AiTaskVO | null>(null)
  const isPolling = ref(false)
  let timer: number | null = null
  let pollCount = 0
  const maxPolls = 2400  // v1.1：与 Redis 锁 TTL 对齐（30s × 2400 ≈ 2h）

  // 启动轮询
  async function startPolling(shotId: number, options?: PollingOptions) {
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
        const res = await aiApi.getLatestTask(shotId)
        task.value = res.data

        // 任务完成或失败 → 停止轮询 → 触发 onDone
        if (res.data.status === 2 || res.data.status === 3) {
          stopPolling()
          options?.onDone?.(res.data)
          return
        }
      } catch (error) {
        console.error('Task polling error:', error)
      }

      pollCount++
      // v1.1：根据轮询次数动态计算间隔（三档退避）
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

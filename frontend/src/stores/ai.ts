// stores/ai.ts — 系分第 7 节：AI 任务状态 Store（骨架）

import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AiTaskVO } from '@/types'

export const useAiStore = defineStore('ai', () => {
  // taskId → AiTaskVO 映射
  const taskMap = ref<Record<number, AiTaskVO>>({})
  const loading = ref(false)

  // 更新任务状态（由轮询 composable 调用）
  function updateTask(task: AiTaskVO) {
    taskMap.value[task.id] = task
  }

  // 清除任务
  function clearTask(taskId: number) {
    delete taskMap.value[taskId]
  }

  return {
    taskMap, loading,
    updateTask, clearTask
  }
})

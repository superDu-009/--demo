// stores/content.ts — 内容状态 Store（按项目 → 分集 → 分镜口径收口）

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { contentApi } from '@/api/content'
import type { EpisodeVO, ShotVO } from '@/types'

export const useContentStore = defineStore('content', () => {
  const episodes = ref<EpisodeVO[]>([])
  const shots = ref<ShotVO[]>([])
  const currentEpisodeId = ref<number | null>(null)
  const loading = ref(false)

  async function fetchEpisodes(projectId: number) {
    const res = await contentApi.listEpisodes(projectId)
    episodes.value = res.data
  }

  async function fetchShots(episodeId: number, params?: { promptStatus?: string; imageStatus?: string; videoStatus?: string }) {
    loading.value = true
    try {
      const res = await contentApi.listShots(episodeId, params)
      shots.value = res.data
    } finally {
      loading.value = false
    }
  }

  return {
    episodes, shots, currentEpisodeId, loading,
    fetchEpisodes, fetchShots
  }
})

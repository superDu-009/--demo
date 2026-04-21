// stores/content.ts — 系分第 7 节：内容状态 Store（Sprint 2 完整实现）

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { contentApi } from '@/api/content'
import type { EpisodeVO, SceneVO, ShotVO, PageResult } from '@/types'

export const useContentStore = defineStore('content', () => {
  const episodes = ref<EpisodeVO[]>([])
  const scenes = ref<SceneVO[]>([])
  const shots = ref<PageResult<ShotVO>>({
    total: 0, page: 1, size: 20, hasNext: false, list: []
  })
  const currentEpisodeId = ref<number | null>(null)
  const currentSceneId = ref<number | null>(null)
  const loading = ref(false)

  async function fetchEpisodes(projectId: number) {
    const res = await contentApi.listEpisodes(projectId)
    episodes.value = res.data
  }

  async function fetchScenes(episodeId: number) {
    const res = await contentApi.listScenes(episodeId)
    scenes.value = res.data
  }

  async function fetchShots(sceneId: number, page = 1, size = 20, status?: number) {
    loading.value = true
    try {
      const res = await contentApi.listShots(sceneId, { page, size, status })
      shots.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function batchReview(data: { shotIds: number[]; action: 'approve' | 'reject'; comment?: string }) {
    return await contentApi.batchReview(data)
  }

  async function bindAsset(shotId: number, assetId: number, assetType: string) {
    await contentApi.bindAsset(shotId, { assetId, assetType })
  }

  async function unbindAsset(shotId: number, assetId: number) {
    await contentApi.unbindAsset(shotId, assetId)
  }

  return {
    episodes, scenes, shots, currentEpisodeId, currentSceneId, loading,
    fetchEpisodes, fetchScenes, fetchShots, batchReview, bindAsset, unbindAsset
  }
})

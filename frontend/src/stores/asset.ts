// stores/asset.ts — 系分第 7 节：资产状态 Store（Sprint 2 完整实现）

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assetApi } from '@/api/asset'
import type { AssetVO, AssetType, PageResult } from '@/types'

export const useAssetStore = defineStore('asset', () => {
  const assetsByType = ref<Record<AssetType, AssetVO[]>>({
    character: [],
    scene: [],
    prop: [],
    voice: []
  })
  const loading = ref(false)

  async function fetchAssets(projectId: number, assetType?: AssetType) {
    loading.value = true
    try {
      const res = await assetApi.list(projectId, { type: assetType })
      const pageData = res.data as PageResult<AssetVO>
      const list = pageData.records || pageData.list
      if (assetType) {
        assetsByType.value[assetType] = list
      } else {
        // 按类型分组
        const grouped: Record<AssetType, AssetVO[]> = {
          character: [], scene: [], prop: [], voice: []
        }
        list.forEach(asset => {
          if (grouped[asset.assetType]) {
            grouped[asset.assetType].push(asset)
          }
        })
        assetsByType.value = grouped
      }
    } finally {
      loading.value = false
    }
  }

  async function createAsset(projectId: number, data: {
    assetType: AssetType
    name: string
    description?: string
    referenceImages?: string[]
  }) {
    await assetApi.create(projectId, data)
    await fetchAssets(projectId, data.assetType)
  }

  async function deleteAsset(projectId: number, id: number) {
    await assetApi.delete(id)
    await fetchAssets(projectId)
  }

  async function confirmAsset(id: number) {
    await assetApi.confirm(id)
  }

  return {
    assetsByType, loading,
    fetchAssets, createAsset, deleteAsset, confirmAsset
  }
})

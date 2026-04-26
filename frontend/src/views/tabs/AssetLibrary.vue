<template>
  <div class="asset-library-page">
    <section class="toolbar card-glass border-neon hud-panel hud-corner">
      <div>
        <p class="eyebrow">Asset Library</p>
        <h3 class="hud-title">资产库</h3>
        <p>角色、场景、道具、声音统一维护，支持父子资产关系树。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="openExtractDialog">提取资产</el-button>
        <vi-button color="green" radius="round" mutate @click="openCreateDialog">新建资产</vi-button>
      </div>
    </section>

    <section class="filter-bar card-glass hud-corner">
      <el-tabs v-model="assetType">
        <el-tab-pane label="角色" name="character" />
        <el-tab-pane label="场景" name="scene" />
        <el-tab-pane label="道具" name="prop" />
        <el-tab-pane label="声音" name="voice" />
      </el-tabs>
      <el-input v-model="searchKeyword" placeholder="搜索资产名称或描述" clearable class="search-box" />
    </section>

    <section class="asset-grid" v-loading="loading">
      <AssetCard
        v-for="asset in filteredAssets"
        :key="asset.id"
        :asset="asset"
        :generating-status="generationState[asset.id] || GenerationStatus.Pending"
        :parent-asset-names="getParentNames(asset)"
        :child-preview-list="getChildPreviews(asset)"
        @edit="openEditDialog"
        @delete="handleDelete"
        @generate="generateAsset"
      />
      <el-empty v-if="!loading && filteredAssets.length === 0" description="当前分类暂无资产" />
    </section>

    <el-dialog v-model="assetDialogVisible" :title="editingAsset ? '编辑资产' : '新建资产'" width="640px">
      <AssetForm ref="assetFormRef" :mode="editingAsset ? 'edit' : 'create'" :asset="editingAsset" :project-id="projectId" :assets="assetList" />
      <template #footer>
        <el-button @click="assetDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="submitLoading" @click="submitAsset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="extractDialogVisible" title="提取资产" width="520px">
      <el-form label-width="88px">
        <el-form-item label="选择分集">
          <el-select v-model="selectedEpisodeIds" class="full-width" multiple collapse-tags>
            <el-option v-for="episode in episodes" :key="episode.id" :label="episode.title" :value="episode.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extractDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="extracting" @click="extractAssets">开始提取</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="duplicateDialogVisible" title="检测到重复资产" width="500px">
      <el-alert
        :title="duplicateDialogTitle"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form label-width="96px" class="duplicate-form">
        <el-form-item label="处理方式">
          <el-select v-model="duplicateDecision" class="full-width">
            <el-option v-for="item in DUPLICATE_DECISION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="duplicateDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" @click="confirmDuplicateDecision">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AssetCard from '@/components/Asset/AssetCard.vue'
import AssetForm from '@/components/Asset/AssetForm.vue'
import { DUPLICATE_DECISION_OPTIONS } from '@/constants/options'
import { assetApi } from '@/api/asset'
import { contentApi } from '@/api/content'
import { taskApi } from '@/api/task'
import { AiTaskStatus, AssetType, GenerationStatus } from '@/types'
import type { AssetCreateRequest, AssetVO, EpisodeVO } from '@/types'

type DuplicateDecision = 'merge' | 'delete' | 'keep'

const route = useRoute()
const projectId = Number(route.params.id)

const loading = ref(false)
const submitLoading = ref(false)
const extracting = ref(false)
const assetType = ref<AssetType>(AssetType.Character)
const searchKeyword = ref('')
const assetDialogVisible = ref(false)
const extractDialogVisible = ref(false)
const duplicateDialogVisible = ref(false)
const assetFormRef = ref<InstanceType<typeof AssetForm>>()
const editingAsset = ref<AssetVO | null>(null)
const assetList = ref<AssetVO[]>([])
const episodes = ref<EpisodeVO[]>([])
const selectedEpisodeIds = ref<number[]>([])
const duplicateDecision = ref<DuplicateDecision>('merge')
const generationState = ref<Record<number, GenerationStatus>>({})
const relationTree = ref<Record<number, { parentIds: number[]; childIds: number[] }>>({})
const duplicateAssetIds = ref<number[]>([])

const filteredAssets = computed(() => {
  const keyword = searchKeyword.value.trim()
  return assetList.value.filter(asset => {
    const typeMatch = asset.assetType === assetType.value
    const keywordMatch = !keyword || asset.name.includes(keyword) || (asset.description || '').includes(keyword)
    return typeMatch && keywordMatch
  })
})

const duplicateAssets = computed(() => duplicateAssetIds.value.map(id => assetList.value.find(item => item.id === id)).filter(Boolean) as AssetVO[])
const duplicateDialogTitle = computed(() => {
  if (duplicateAssets.value.length < 2) return '检测到重复资产，请确认处理方式'
  return `检测到「${duplicateAssets.value.map(item => item.name).join(' / ')}」疑似重复，请确认处理方式`
})

const normalizeAsset = (asset: AssetVO): AssetVO => ({
  ...asset,
  parentIds: asset.parentIds || [],
  draftContent: asset.draftContent || '',
  referenceImages: asset.referenceImages || []
})

const waitForTask = async (taskId: number) => {
  const maxPolls = 240
  for (let count = 0; count < maxPolls; count += 1) {
    const res = await taskApi.getStatus(taskId)
    if (res.data.status === AiTaskStatus.Success || res.data.status === AiTaskStatus.Failed) {
      return res.data
    }
    await new Promise(resolve => window.setTimeout(resolve, count < 3 ? 3000 : 5000))
  }
  throw new Error('任务轮询超时')
}

const fetchAssets = async () => {
  loading.value = true
  try {
    const [assetRes, treeRes] = await Promise.all([
      assetApi.list(projectId),
      assetApi.getTree(projectId).catch(() => ({ data: [] as Array<{ id: number; parentIds: number[]; childIds: number[] }> }))
    ])
    assetList.value = (assetRes.data.list || assetRes.data.records || []).map(normalizeAsset)
    relationTree.value = Object.fromEntries((treeRes.data || []).map(item => [item.id, item]))
  } finally {
    loading.value = false
  }
}

const fetchEpisodes = async () => {
  const res = await contentApi.listEpisodes(projectId)
  episodes.value = res.data || []
}

const openCreateDialog = () => {
  editingAsset.value = null
  assetDialogVisible.value = true
}

const openEditDialog = (asset: AssetVO) => {
  editingAsset.value = asset
  assetDialogVisible.value = true
}

const checkDuplicates = async (focusAssetId?: number) => {
  const res = await assetApi.getDuplicates(projectId).catch(() => ({ data: [] as Array<{ assetIds: number[] }> }))
  const groups = res.data || []
  if (groups.length === 0) return
  const targetGroup = focusAssetId
    ? groups.find(group => group.assetIds.includes(focusAssetId))
    : groups[0]
  if (!targetGroup) return
  duplicateAssetIds.value = targetGroup.assetIds
  duplicateDecision.value = 'merge'
  duplicateDialogVisible.value = true
}

const syncRelations = async (assetId: number, parentIds: number[]) => {
  await assetApi.updateRelations(assetId, { parentIds })
}

const submitAsset = async () => {
  const payload = await assetFormRef.value?.validate()
  if (!payload) return
  submitLoading.value = true
  try {
    if (editingAsset.value) {
      await assetApi.update(editingAsset.value.id, payload)
      await syncRelations(editingAsset.value.id, payload.parentIds || [])
      ElMessage.success('资产已更新')
      assetDialogVisible.value = false
      await fetchAssets()
      await checkDuplicates(editingAsset.value.id)
      return
    }

    const createRes = await assetApi.create(projectId, payload as AssetCreateRequest)
    await syncRelations(createRes.data, (payload as AssetCreateRequest).parentIds || [])
    assetDialogVisible.value = false
    ElMessage.success('资产已创建')
    await fetchAssets()
    await checkDuplicates(createRes.data)
  } finally {
    submitLoading.value = false
  }
}

const confirmDuplicateDecision = async () => {
  if (duplicateAssets.value.length < 2) {
    duplicateDialogVisible.value = false
    return
  }
  const [primaryAsset, secondaryAsset] = duplicateAssets.value
  if (!primaryAsset || !secondaryAsset) return

  if (duplicateDecision.value === 'delete') {
    await assetApi.delete(secondaryAsset.id)
    ElMessage.info('已删除重复资产')
  } else if (duplicateDecision.value === 'merge') {
    await assetApi.update(primaryAsset.id, {
      description: [primaryAsset.description, secondaryAsset.description].filter(Boolean).join(' / '),
      referenceImages: Array.from(new Set([...(primaryAsset.referenceImages || []), ...(secondaryAsset.referenceImages || [])])),
      parentIds: Array.from(new Set([...(primaryAsset.parentIds || []), ...(secondaryAsset.parentIds || [])])),
      draftContent: [primaryAsset.draftContent, secondaryAsset.draftContent].filter(Boolean).join('\n')
    })
    await assetApi.delete(secondaryAsset.id)
    ElMessage.success('重复资产已合并')
  } else {
    ElMessage.success('已保留重复资产')
  }

  duplicateDialogVisible.value = false
  duplicateAssetIds.value = []
  await fetchAssets()
}

const extractAssets = async () => {
  if (selectedEpisodeIds.value.length === 0) {
    ElMessage.warning('请选择至少一个分集')
    return
  }
  if (selectedEpisodeIds.value.length > 1) {
    ElMessage.warning('当前后端仅支持单分集提取，将使用你选择的第一个分集')
  }
  extracting.value = true
  try {
    const res = await assetApi.extract(projectId, { episodeIds: selectedEpisodeIds.value })
    const task = await waitForTask(res.data)
    if (task.status === AiTaskStatus.Failed) {
      throw new Error(task.errorMsg || '资产提取失败')
    }
    extractDialogVisible.value = false
    await fetchAssets()
    await checkDuplicates()
    ElMessage.success('资产提取完成')
  } catch (error: any) {
    ElMessage.error(error.message || '资产提取失败')
  } finally {
    extracting.value = false
  }
}

const openExtractDialog = () => {
  selectedEpisodeIds.value = []
  extractDialogVisible.value = true
}

const handleDelete = async (asset: AssetVO) => {
  const referenceRes = await assetApi.getReferences(asset.id, { page: 1, size: 1 }).catch(() => null)
  if (referenceRes?.data?.total) {
    ElMessage.warning('被分镜绑定的资产禁止删除')
    return
  }
  await ElMessageBox.confirm(`确认删除「${asset.name}」吗？`, '删除资产', { type: 'warning' })
  await assetApi.delete(asset.id)
  ElMessage.success('资产已删除')
  await fetchAssets()
}

const generateAsset = async (asset: AssetVO) => {
  ElMessage.warning(`资产「${asset.name}」当前仅支持上传参考图，后端尚未提供资产生图接口`)
}

const getParentNames = (asset: AssetVO) => {
  const parentIds = relationTree.value[asset.id]?.parentIds || asset.parentIds || []
  return parentIds
    .map(id => assetList.value.find(item => item.id === id)?.name)
    .filter(Boolean) as string[]
}

const getChildPreviews = (asset: AssetVO) => {
  const childIds = relationTree.value[asset.id]?.childIds
    || assetList.value.filter(item => (item.parentIds || []).includes(asset.id)).map(item => item.id)
  return childIds
    .map(id => assetList.value.find(item => item.id === id)?.referenceImages?.[0] || '')
    .filter(Boolean)
    .slice(0, 4)
}

onMounted(async () => {
  await Promise.all([fetchAssets(), fetchEpisodes()])
})
</script>

<style scoped lang="scss">
.asset-library-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar,
.filter-bar {
  padding: 20px 22px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: $accent-green;
  color: $accent-yellow;
  font-size: 12px;
}

.toolbar h3 {
  margin: 0;
  color: $text-primary;
  font-size: 28px;
}

.toolbar p {
  margin: 8px 0 0;
  color: $text-secondary;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  border: 1px solid rgba(92, 241, 255, 0.14);
}

.search-box,
.full-width {
  width: 280px;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.duplicate-form {
  margin-top: 16px;
}

@media (max-width: 840px) {
  .toolbar,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box,
  .full-width {
    width: 100%;
  }
}
</style>

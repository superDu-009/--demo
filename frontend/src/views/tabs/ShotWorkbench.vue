<template>
  <div class="shot-workbench-page">
    <aside class="scene-panel card-glass border-neon">
      <div class="panel-head">
        <p class="eyebrow">Scene Index</p>
        <h3>分集 / 分场</h3>
      </div>
      <div class="episode-list">
        <section v-for="episode in episodes" :key="episode.id" class="episode-block">
          <div class="episode-title">
            <span>{{ episode.title }}</span>
            <em>{{ episode.shotStats?.approved || 0 }}/{{ episode.shotStats?.total || 0 }}</em>
          </div>
          <button
            v-for="scene in scenesByEpisode[episode.id]"
            :key="scene.id"
            class="scene-item"
            :class="{ active: currentSceneId === scene.id && viewMode === 'scene' }"
            @click="selectScene(scene.id)"
          >
            <span>{{ scene.title }}</span>
            <small>{{ scene.shotStats?.approved || 0 }}/{{ scene.shotStats?.total || 0 }} 通过</small>
          </button>
        </section>
      </div>
    </aside>

    <main class="shot-panel">
      <div class="workbench-hero card-glass border-neon">
        <div>
          <p class="eyebrow">Shot Review Desk</p>
          <h3>{{ currentSceneTitle }}</h3>
          <p>用分页列表审核分镜、查看生成状态，并维护镜头资产绑定。</p>
        </div>
        <el-radio-group v-model="viewMode" @change="handleViewModeChange">
          <el-radio-button label="scene">当前分场</el-radio-button>
          <el-radio-button label="project">全部镜头</el-radio-button>
        </el-radio-group>
      </div>

      <div class="toolbar card-glass">
        <el-segmented v-model="statusFilter" :options="statusOptions" @change="resetPage" />
        <div class="toolbar-actions">
          <span class="selected-count">已选 {{ selectedShotIds.length }}</span>
          <el-button :disabled="selectedShotIds.length === 0" type="success" @click="openBatchReview('approve')">
            批量通过
          </el-button>
          <el-button :disabled="selectedShotIds.length === 0" type="danger" @click="openBatchReview('reject')">
            批量打回
          </el-button>
          <el-button :disabled="selectedShotIds.length === 0" @click="regenerateSelected">
            重新生成
          </el-button>
        </div>
      </div>

      <div class="shot-list" v-loading="loadingShots">
        <ShotCard
          v-for="shot in currentPageShots"
          :key="shot.id"
          :shot="shot"
          :selected="selectedShotIds.includes(shot.id)"
          :scene-title="viewMode === 'project' ? sceneTitleMap[shot.sceneId] : ''"
          @toggle-select="toggleSelect"
          @review="openSingleReview"
          @regenerate="regenerateShot"
          @bind-assets="openBindPanel"
        />
      </div>

      <EmptyState
        v-if="currentPageShots.length === 0"
        type="shot"
        title="暂无分镜"
        description="当前筛选条件下没有分镜。"
      />

      <div v-if="totalShots > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalShots"
          layout="total, prev, pager, next"
          @current-change="fetchShots"
        />
      </div>
    </main>

    <ShotReviewDialog
      v-model="reviewDialogVisible"
      :action="reviewAction"
      :count="reviewTargetIds.length"
      @submit="submitReview"
    />

    <AssetBindPanel
      v-model="bindPanelVisible"
      :shot="bindingShot"
      :assets="assets"
      @save="saveAssetBindings"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ShotCard from '@/components/Shot/ShotCard.vue'
import ShotReviewDialog from '@/components/Shot/ShotReviewDialog.vue'
import AssetBindPanel from '@/components/Shot/AssetBindPanel.vue'
import EmptyState from '@/components/Common/EmptyState.vue'
import { ShotStatus } from '@/types'
import { assetApi } from '@/api/asset'
import { contentApi } from '@/api/content'
import { projectApi } from '@/api/project'
import type { AssetVO, EpisodeVO, SceneVO, ShotVO } from '@/types'

const pageSize = 20
const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const currentPage = ref(1)
const currentSceneId = ref<number | null>(null)
const viewMode = ref<'scene' | 'project'>('scene')
const statusFilter = ref<'all' | ShotStatus>('all')
const selectedShotIds = ref<number[]>([])
const reviewDialogVisible = ref(false)
const reviewAction = ref<'approve' | 'reject'>('approve')
const reviewTargetIds = ref<number[]>([])
const bindPanelVisible = ref(false)
const bindingShot = ref<ShotVO | null>(null)
const episodes = ref<EpisodeVO[]>([])
const scenes = ref<SceneVO[]>([])
const shots = ref<ShotVO[]>([])
const assets = ref<AssetVO[]>([])
const totalShots = ref(0)
const loadingShots = ref(false)

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待处理', value: ShotStatus.Pending },
  { label: '生成中', value: ShotStatus.Generating },
  { label: '待审核', value: ShotStatus.WaitingReview },
  { label: '已通过', value: ShotStatus.Approved },
  { label: '已打回', value: ShotStatus.Rejected }
]

const scenesByEpisode = computed(() => {
  return scenes.value.reduce<Record<number, SceneVO[]>>((result, scene) => {
    if (!result[scene.episodeId]) result[scene.episodeId] = []
    result[scene.episodeId].push(scene)
    return result
  }, {})
})

const sceneTitleMap = computed(() => {
  return scenes.value.reduce<Record<number, string>>((result, scene) => {
    result[scene.id] = scene.title
    return result
  }, {})
})

const currentSceneTitle = computed(() => {
  if (viewMode.value === 'project') return '全部镜头'
  return currentSceneId.value ? sceneTitleMap.value[currentSceneId.value] || '当前分场' : '当前分场'
})

const filteredShots = computed(() => {
  return shots.value
})

const currentPageShots = computed(() => {
  return shots.value
})

const resetPage = () => {
  currentPage.value = 1
  selectedShotIds.value = []
  fetchShots()
}

const selectScene = (sceneId: number) => {
  currentSceneId.value = sceneId
  viewMode.value = 'scene'
  resetPage()
}

const handleViewModeChange = () => {
  resetPage()
}

const toggleSelect = (shotId: number) => {
  selectedShotIds.value = selectedShotIds.value.includes(shotId)
    ? selectedShotIds.value.filter(id => id !== shotId)
    : [...selectedShotIds.value, shotId]
}

const openBatchReview = (action: 'approve' | 'reject') => {
  reviewAction.value = action
  reviewTargetIds.value = [...selectedShotIds.value]
  reviewDialogVisible.value = true
}

const openSingleReview = (shot: ShotVO, action: 'approve' | 'reject') => {
  reviewAction.value = action
  reviewTargetIds.value = [shot.id]
  reviewDialogVisible.value = true
}

const submitReview = async ({ action, comment }: { action: 'approve' | 'reject'; comment?: string }) => {
  await contentApi.batchReview({ shotIds: reviewTargetIds.value, action, comment })
  selectedShotIds.value = selectedShotIds.value.filter(id => !reviewTargetIds.value.includes(id))
  ElMessage.success(action === 'approve' ? '审核已通过' : '分镜已打回')
  fetchShots()
}

const regenerateShot = (_shot: ShotVO) => {
  ElMessage.info('后端暂未提供单镜头重新生成接口')
}

const regenerateSelected = () => {
  ElMessage.info('后端暂未提供批量重新生成接口')
  selectedShotIds.value = []
}

const openBindPanel = (shot: ShotVO) => {
  bindingShot.value = shot
  bindPanelVisible.value = true
}

const saveAssetBindings = async (assetIds: number[]) => {
  if (!bindingShot.value) return
  const oldIds = bindingShot.value.assetRefs.map(item => item.assetId)
  const addIds = assetIds.filter(id => !oldIds.includes(id))
  const removeIds = oldIds.filter(id => !assetIds.includes(id))
  await Promise.all(addIds.map((assetId) => {
    const asset = assets.value.find(item => item.id === assetId)
    return asset ? contentApi.bindAsset(bindingShot.value!.id, { assetId, assetType: asset.assetType }) : Promise.resolve()
  }))
  await Promise.all(removeIds.map(assetId => contentApi.unbindAsset(bindingShot.value!.id, assetId)))
  ElMessage.success('资产绑定已更新')
  fetchShots()
}

const fetchEpisodesAndScenes = async () => {
  if (!projectId.value) return
  const episodeRes = await contentApi.listEpisodes(projectId.value)
  episodes.value = episodeRes.data
  const sceneLists = await Promise.all(episodes.value.map(episode => contentApi.listScenes(episode.id)))
  scenes.value = sceneLists.flatMap(res => res.data)
  if (!currentSceneId.value && scenes.value.length > 0) currentSceneId.value = scenes.value[0].id
}

const fetchAssets = async () => {
  if (!projectId.value) return
  const res = await assetApi.list(projectId.value)
  assets.value = res.data.list
}

const fetchShots = async () => {
  if (!projectId.value) return
  if (viewMode.value === 'scene' && !currentSceneId.value) {
    shots.value = []
    totalShots.value = 0
    return
  }
  loadingShots.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value,
      sceneId: viewMode.value === 'scene' ? currentSceneId.value || undefined : undefined
    }
    const res = viewMode.value === 'project'
      ? await projectApi.getShots(projectId.value, params)
      : await contentApi.listShots(currentSceneId.value!, params)
    shots.value = res.data.list || res.data.records || []
    totalShots.value = res.data.total
  } finally {
    loadingShots.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchEpisodesAndScenes(), fetchAssets()])
  await fetchShots()
})

watch(() => route.params.id, async () => {
  currentSceneId.value = null
  currentPage.value = 1
  await Promise.all([fetchEpisodesAndScenes(), fetchAssets()])
  await fetchShots()
})
</script>

<style scoped lang="scss">
.shot-workbench-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
}

.scene-panel {
  position: sticky;
  top: 88px;
  align-self: start;
  padding: 18px;
  border-radius: 8px;
}

.panel-head,
.workbench-hero {
  .eyebrow {
    margin: 0 0 6px;
    color: $accent-green;
    font-size: 12px;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  h3 {
    margin: 0;
    color: $text-primary;
    font-size: 20px;
  }
}

.episode-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.episode-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: $text-secondary;
  font-size: 13px;

  em {
    color: $accent-green;
    font-style: normal;
  }
}

.scene-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 10px 12px;
  margin-bottom: 8px;
  color: $text-secondary;
  border: 1px solid rgba(100, 108, 255, 0.14);
  border-radius: 8px;
  background: rgba(100, 108, 255, 0.04);
  cursor: pointer;
  text-align: left;

  &.active,
  &:hover {
    color: $text-primary;
    border-color: rgba(34, 211, 238, 0.62);
    background: rgba(34, 211, 238, 0.08);
  }

  small {
    color: $text-tertiary;
  }
}

.shot-panel {
  min-width: 0;
}

.workbench-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 20px;
  margin-bottom: 16px;
  border-radius: 8px;

  p:last-child {
    margin: 8px 0 0;
    color: $text-secondary;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px;
  margin-bottom: 16px;
  border-radius: 8px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.selected-count {
  color: $text-secondary;
  font-size: 13px;
}

.shot-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

@media (max-width: 1080px) {
  .shot-workbench-page {
    grid-template-columns: 1fr;
  }

  .scene-panel {
    position: static;
  }

  .toolbar,
  .workbench-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    flex-wrap: wrap;
  }
}
</style>

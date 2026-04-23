<template>
  <div class="shot-workbench-page">
    <aside class="scene-panel card-glass border-neon">
      <div class="panel-head">
        <p class="eyebrow">Scene Index</p>
        <h3>分集 / 分场</h3>
      </div>
      <div class="episode-list">
        <section v-for="episode in mockEpisodes" :key="episode.id" class="episode-block">
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
          <el-button :disabled="selectedShotIds.length === 0" @click="mockRegenerateSelected">
            重新生成
          </el-button>
        </div>
      </div>

      <div class="shot-list">
        <ShotCard
          v-for="shot in currentPageShots"
          :key="shot.id"
          :shot="shot"
          :selected="selectedShotIds.includes(shot.id)"
          :scene-title="viewMode === 'project' ? sceneTitleMap[shot.sceneId] : ''"
          @toggle-select="toggleSelect"
          @review="openSingleReview"
          @regenerate="mockRegenerate"
          @bind-assets="openBindPanel"
        />
      </div>

      <EmptyState
        v-if="currentPageShots.length === 0"
        type="shot"
        title="暂无分镜"
        description="当前筛选条件下没有分镜。"
      />

      <div v-if="filteredShots.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredShots.length"
          layout="total, prev, pager, next"
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
      :assets="mockAssets"
      @save="saveAssetBindings"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ShotCard from '@/components/Shot/ShotCard.vue'
import ShotReviewDialog from '@/components/Shot/ShotReviewDialog.vue'
import AssetBindPanel from '@/components/Shot/AssetBindPanel.vue'
import EmptyState from '@/components/Common/EmptyState.vue'
import { AiTaskStatus, AssetStatus, AssetType, ShotStatus } from '@/types'
import type { AssetVO, EpisodeVO, SceneVO, ShotAssetRef, ShotVO } from '@/types'

const pageSize = 20
const currentPage = ref(1)
const currentSceneId = ref(101)
const viewMode = ref<'scene' | 'project'>('scene')
const statusFilter = ref<'all' | ShotStatus>('all')
const selectedShotIds = ref<number[]>([])
const reviewDialogVisible = ref(false)
const reviewAction = ref<'approve' | 'reject'>('approve')
const reviewTargetIds = ref<number[]>([])
const bindPanelVisible = ref(false)
const bindingShot = ref<ShotVO | null>(null)

const mockEpisodes = ref<EpisodeVO[]>([
  {
    id: 1,
    projectId: 1,
    title: '第 1 集：雨夜来信',
    sortOrder: 1,
    content: null,
    status: 1,
    createTime: '2026-04-22 10:00:00',
    updateTime: '2026-04-22 10:00:00',
    sceneCount: 2,
    shotStats: { total: 18, approved: 10, rejected: 2 }
  },
  {
    id: 2,
    projectId: 1,
    title: '第 2 集：机械花园',
    sortOrder: 2,
    content: null,
    status: 1,
    createTime: '2026-04-22 10:00:00',
    updateTime: '2026-04-22 10:00:00',
    sceneCount: 2,
    shotStats: { total: 14, approved: 6, rejected: 1 }
  }
])

const mockScenes = ref<SceneVO[]>([
  { id: 101, episodeId: 1, title: '第 1 场 天台告白', sortOrder: 1, content: null, status: 1, createTime: '', updateTime: '', shotCount: 9, shotStats: { total: 9, approved: 6, rejected: 1 } },
  { id: 102, episodeId: 1, title: '第 2 场 地铁追逐', sortOrder: 2, content: null, status: 1, createTime: '', updateTime: '', shotCount: 9, shotStats: { total: 9, approved: 4, rejected: 1 } },
  { id: 201, episodeId: 2, title: '第 1 场 温室苏醒', sortOrder: 1, content: null, status: 1, createTime: '', updateTime: '', shotCount: 8, shotStats: { total: 8, approved: 4, rejected: 0 } },
  { id: 202, episodeId: 2, title: '第 2 场 核心机房', sortOrder: 2, content: null, status: 1, createTime: '', updateTime: '', shotCount: 6, shotStats: { total: 6, approved: 2, rejected: 1 } }
])

const mockAssets = ref<AssetVO[]>([
  { id: 1, projectId: 1, assetType: AssetType.Character, name: '林夏', description: '短发女主，黑色风衣，冷静但敏锐。', referenceImages: ['/assets/images/project-cover-japanese.png'], stylePreset: null, status: AssetStatus.Confirmed, createTime: '', updateTime: '' },
  { id: 2, projectId: 1, assetType: AssetType.Character, name: '祁野', description: '银发少年，机械义眼。', referenceImages: ['/assets/images/project-cover-scifi.png'], stylePreset: null, status: AssetStatus.Confirmed, createTime: '', updateTime: '' },
  { id: 3, projectId: 1, assetType: AssetType.Scene, name: '雨夜天台', description: '霓虹、积水、城市远景。', referenceImages: ['/assets/images/bg-texture.png'], stylePreset: null, status: AssetStatus.Confirmed, createTime: '', updateTime: '' },
  { id: 4, projectId: 1, assetType: AssetType.Prop, name: '蓝色芯片', description: '透明封装，内部有流动光点。', referenceImages: [], stylePreset: null, status: AssetStatus.Draft, createTime: '', updateTime: '' },
  { id: 5, projectId: 1, assetType: AssetType.Voice, name: '女主声线', description: '低声、干净、带轻微沙哑。', referenceImages: [], stylePreset: { audioUrl: '' }, status: AssetStatus.Confirmed, createTime: '', updateTime: '' }
])

const assetRef = (assetId: number): ShotAssetRef => {
  const asset = mockAssets.value.find(item => item.id === assetId)!
  return {
    assetId: asset.id,
    assetType: asset.assetType,
    assetName: asset.name,
    primaryImage: asset.referenceImages?.[0] || ''
  }
}

const mockShots = ref<ShotVO[]>(
  Array.from({ length: 32 }).map((_, index) => {
    const sceneIds = [101, 102, 201, 202]
    const sceneId = sceneIds[index % sceneIds.length]
    const statusCycle = [
      ShotStatus.WaitingReview,
      ShotStatus.Approved,
      ShotStatus.Generating,
      ShotStatus.Pending,
      ShotStatus.Rejected,
      ShotStatus.Completed
    ]
    const status = statusCycle[index % statusCycle.length]
    return {
      id: index + 1,
      sceneId,
      sortOrder: index + 1,
      prompt: `镜头${index + 1}，雨水划过玻璃，角色在霓虹反光中抬头，情绪克制。`,
      promptEn: `Shot ${index + 1}, rain streaks across glass, the character looks up under neon reflections with restrained emotion.`,
      generatedImageUrl: index % 4 === 0 ? null : ['/assets/images/project-cover-japanese.png', '/assets/images/project-cover-scifi.png', '/assets/images/project-cover-chinese.png'][index % 3],
      generatedVideoUrl: null,
      status,
      reviewComment: status === ShotStatus.Rejected ? '人物表情不够清晰，需要强化近景。' : null,
      version: (index % 3) + 1,
      generationAttempts: index % 4,
      assetRefs: [assetRef(1), assetRef(sceneId === 101 ? 3 : 2)].filter(Boolean),
      currentAiTask: status === ShotStatus.Generating
        ? {
            taskId: 9000 + index,
            taskType: index % 2 === 0 ? 'image_gen' : 'video_gen',
            status: index % 3 === 0 ? AiTaskStatus.Submitting : AiTaskStatus.Processing
          }
        : null,
      createTime: '',
      updateTime: ''
    }
  })
)

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待处理', value: ShotStatus.Pending },
  { label: '生成中', value: ShotStatus.Generating },
  { label: '待审核', value: ShotStatus.WaitingReview },
  { label: '已通过', value: ShotStatus.Approved },
  { label: '已打回', value: ShotStatus.Rejected }
]

const scenesByEpisode = computed(() => {
  return mockScenes.value.reduce<Record<number, SceneVO[]>>((result, scene) => {
    if (!result[scene.episodeId]) result[scene.episodeId] = []
    result[scene.episodeId].push(scene)
    return result
  }, {})
})

const sceneTitleMap = computed(() => {
  return mockScenes.value.reduce<Record<number, string>>((result, scene) => {
    result[scene.id] = scene.title
    return result
  }, {})
})

const currentSceneTitle = computed(() => {
  if (viewMode.value === 'project') return '全部镜头'
  return sceneTitleMap.value[currentSceneId.value] || '当前分场'
})

const filteredShots = computed(() => {
  const sceneShots = viewMode.value === 'scene'
    ? mockShots.value.filter(shot => shot.sceneId === currentSceneId.value)
    : mockShots.value
  if (statusFilter.value === 'all') return sceneShots
  return sceneShots.filter(shot => shot.status === statusFilter.value)
})

const currentPageShots = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredShots.value.slice(start, start + pageSize)
})

const resetPage = () => {
  currentPage.value = 1
  selectedShotIds.value = []
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

const submitReview = ({ action, comment }: { action: 'approve' | 'reject'; comment?: string }) => {
  mockShots.value = mockShots.value.map(shot => {
    if (!reviewTargetIds.value.includes(shot.id)) return shot
    return {
      ...shot,
      status: action === 'approve' ? ShotStatus.Approved : ShotStatus.Rejected,
      reviewComment: comment || null
    }
  })
  selectedShotIds.value = selectedShotIds.value.filter(id => !reviewTargetIds.value.includes(id))
  ElMessage.success(action === 'approve' ? '审核已通过' : '分镜已打回')
}

const mockRegenerate = (shot: ShotVO) => {
  mockShots.value = mockShots.value.map(item => item.id === shot.id
    ? {
        ...item,
        status: ShotStatus.Generating,
        generationAttempts: item.generationAttempts + 1,
        currentAiTask: { taskId: Date.now(), taskType: 'image_gen', status: AiTaskStatus.Submitting }
      }
    : item
  )
  ElMessage.info('已进入重新生成队列')
}

const mockRegenerateSelected = () => {
  selectedShotIds.value.forEach(id => {
    const shot = mockShots.value.find(item => item.id === id)
    if (shot) mockRegenerate(shot)
  })
  selectedShotIds.value = []
}

const openBindPanel = (shot: ShotVO) => {
  bindingShot.value = shot
  bindPanelVisible.value = true
}

const saveAssetBindings = (assetIds: number[]) => {
  if (!bindingShot.value) return
  const refs = assetIds.map(assetRef)
  mockShots.value = mockShots.value.map(shot => shot.id === bindingShot.value?.id
    ? { ...shot, assetRefs: refs }
    : shot
  )
  ElMessage.success('资产绑定已更新')
}
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

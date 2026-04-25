<template>
  <div class="shot-workbench-page">
    <aside class="episode-panel card-glass border-neon">
      <div class="panel-head">
        <p class="eyebrow">Episode Board</p>
        <h3>分集列表</h3>
      </div>

      <div v-for="episode in episodes" :key="episode.id" class="episode-card" :class="{ active: episode.id === currentEpisodeId }">
        <div class="episode-main" @click="selectEpisode(episode.id)">
          <div>
            <strong>{{ episode.title }}</strong>
            <p>{{ episode.content?.slice(0, 40) || '暂无概述' }}</p>
          </div>
          <el-button text @click.stop="toggleExpandEpisode(episode.id)">{{ expandedEpisodeIds.includes(episode.id) ? '收起' : '展开' }}</el-button>
        </div>

        <div class="episode-actions">
          <el-select v-model="splitRuleByEpisode[episode.id]" size="small" class="split-select">
            <el-option v-for="item in SHOT_SPLIT_TEMPLATE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button size="small" :loading="splittingEpisodeId === episode.id" @click="confirmSplitEpisode(episode)">
            {{ episodeHasShots(episode.id) ? '重新拆分' : '分镜拆分' }}
          </el-button>
        </div>

        <div v-if="expandedEpisodeIds.includes(episode.id)" class="episode-detail">
          <p class="content">{{ episode.content || '暂无完整剧本内容' }}</p>
          <div class="asset-pills">
            <span v-for="name in getEpisodeAssetNames(episode.id)" :key="name">{{ name }}</span>
            <span v-if="getEpisodeAssetNames(episode.id).length === 0">暂无关联资产</span>
          </div>
        </div>
      </div>
    </aside>

    <main class="workbench-main">
      <section class="toolbar card-glass border-neon">
        <div>
          <p class="eyebrow">Shot Workbench</p>
          <h3>{{ currentEpisode?.title || '请选择分集' }}</h3>
        </div>
        <div class="toolbar-actions">
          <el-select v-model="filterKind" size="small" style="width: 140px">
            <el-option label="全部状态" value="all" />
            <el-option label="提示词状态" value="prompt" />
            <el-option label="图片状态" value="image" />
            <el-option label="视频状态" value="video" />
          </el-select>
          <el-button @click="batchGenerate('prompt')" :disabled="selectedShotIds.length === 0">批量生成提示词</el-button>
          <el-button @click="batchGenerate('image')" :disabled="selectedShotIds.length === 0">批量生成图片</el-button>
          <el-button @click="batchGenerate('video')" :disabled="selectedShotIds.length === 0">批量生成视频</el-button>
          <el-button class="btn-gradient" @click="openCreateShotDialog" :disabled="!currentEpisodeId">新增分镜</el-button>
        </div>
      </section>

      <section class="shot-list" v-loading="loadingShots">
        <article v-for="shot in visibleShots" :key="shot.id" class="shot-card card-glass">
          <div class="shot-header">
            <div class="shot-title">
              <el-checkbox :model-value="selectedShotIds.includes(shot.id)" @change="toggleSelect(shot.id)" />
              <strong>分镜 {{ shot.sortOrder }}</strong>
              <el-switch
                :model-value="Boolean(shot.followLast)"
                inline-prompt
                active-text="承接"
                inactive-text="独立"
                @change="toggleFollowLast(shot, Boolean($event))"
              />
            </div>
            <div class="shot-actions">
              <el-button size="small" @click="openEditShotDialog(shot)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="deleteShot(shot)">删除</el-button>
            </div>
          </div>

          <div class="status-row">
            <el-tag :type="generationMeta(shot, 'prompt').type">{{ generationMeta(shot, 'prompt').label }}</el-tag>
            <el-tag :type="generationMeta(shot, 'image').type">{{ generationMeta(shot, 'image').label }}</el-tag>
            <el-tag :type="generationMeta(shot, 'video').type">{{ generationMeta(shot, 'video').label }}</el-tag>
          </div>

          <div class="shot-body">
            <div class="field"><span>中文提示词</span><p>{{ shot.prompt || '未生成' }}</p></div>
            <div class="field"><span>英文提示词</span><p>{{ shot.promptEn || '未生成' }}</p></div>
            <div class="meta-row">
              <span>景别：{{ shot.sceneType || '未设置' }}</span>
              <span>运镜：{{ shot.cameraMove || '未设置' }}</span>
              <span>时长：{{ shot.duration || 15 }}s</span>
            </div>
            <div class="field"><span>台词</span><p>{{ shot.lines || '未填写' }}</p></div>
            <div class="field"><span>绑定资产</span><p>{{ shot.assetRefs.map(item => item.assetName).join(' / ') || '未绑定' }}</p></div>
          </div>

          <div class="preview-row">
            <img v-if="shot.generatedImageUrl" :src="shot.generatedImageUrl" alt="图片预览" />
            <div v-else class="empty-preview">待生图</div>
            <video v-if="shot.generatedVideoUrl" :src="shot.generatedVideoUrl" controls preload="metadata" />
            <div v-else class="empty-preview">待生视频</div>
          </div>

          <div class="generate-row">
            <el-button
              size="small"
              :loading="generationMeta(shot, 'prompt').loading"
              :disabled="generationMeta(shot, 'prompt').loading"
              @click="runGenerate(shot, 'prompt')"
            >
              {{ generationMeta(shot, 'prompt').actionText }}
            </el-button>
            <el-button
              size="small"
              :loading="generationMeta(shot, 'image').loading"
              :disabled="generationMeta(shot, 'image').loading"
              @click="runGenerate(shot, 'image')"
            >
              {{ generationMeta(shot, 'image').actionText }}
            </el-button>
            <el-button
              size="small"
              :loading="generationMeta(shot, 'video').loading"
              :disabled="generationMeta(shot, 'video').loading"
              @click="runGenerate(shot, 'video')"
            >
              {{ generationMeta(shot, 'video').actionText }}
            </el-button>
          </div>

          <div v-if="shot.errorMsg" class="error-bar">
            {{ shot.errorMsg }}
          </div>
        </article>

        <el-empty v-if="!loadingShots && visibleShots.length === 0" description="当前分集暂无分镜" />
      </section>
    </main>

    <el-dialog v-model="shotDialogVisible" :title="editingShot ? '编辑分镜' : '新增分镜'" width="680px">
      <el-form label-width="96px">
        <el-form-item label="中文提示词">
          <el-input v-model="shotForm.prompt" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="英文提示词">
          <el-input v-model="shotForm.promptEn" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="景别">
          <el-select v-model="shotForm.sceneType" class="full-width">
            <el-option v-for="item in sceneTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="运镜">
          <el-select v-model="shotForm.cameraMove" class="full-width">
            <el-option v-for="item in cameraMoveOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长">
          <el-select v-model="shotForm.duration" class="full-width">
            <el-option v-for="item in SHOT_DURATION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="台词">
          <el-input v-model="shotForm.lines" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="绑定资产">
          <el-select v-model="shotForm.assetIds" class="full-width" multiple collapse-tags>
            <el-option
              v-for="asset in bindableAssets"
              :key="asset.id"
              :label="asset.name"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="承接上一镜">
          <el-switch v-model="shotForm.followLast" />
        </el-form-item>
        <el-form-item label="草稿内容">
          <el-input v-model="shotForm.draftContent" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shotDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="shotSaving" @click="submitShot">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assetApi } from '@/api/asset'
import { contentApi } from '@/api/content'
import { projectApi } from '@/api/project'
import { taskApi } from '@/api/task'
import { SHOT_DURATION_OPTIONS, SHOT_SPLIT_TEMPLATE_OPTIONS } from '@/constants/options'
import { GENERATION_STATUS_MAP } from '@/constants/status'
import { AiTaskStatus, AssetStatus, GenerationStatus } from '@/types'
import type { AssetVO, BatchGenerateRequest, EpisodeVO, ShotVO } from '@/types'

type GenerateKind = 'prompt' | 'image' | 'video'

const route = useRoute()
const projectId = Number(route.params.id)

const loadingShots = ref(false)
const shotSaving = ref(false)
const splittingEpisodeId = ref<number | null>(null)
const currentEpisodeId = ref<number | null>(null)
const filterKind = ref<'all' | GenerateKind>('all')
const shotDialogVisible = ref(false)
const editingShot = ref<ShotVO | null>(null)
const selectedShotIds = ref<number[]>([])
const episodes = ref<EpisodeVO[]>([])
const shotsByEpisode = ref<Record<number, ShotVO[]>>({})
const assets = ref<AssetVO[]>([])
const expandedEpisodeIds = ref<number[]>([])
const splitRuleByEpisode = ref<Record<number, string>>({})
const taskingMap = ref<Record<string, boolean>>({})
const projectDefaults = ref({ ratio: '16:9', definition: '1080P' })

const sceneTypeOptions = ['远景', '全景', '中景', '近景', '特写']
const cameraMoveOptions = ['固定镜头', '推镜', '拉镜', '摇镜', '跟拍']

const shotForm = reactive({
  prompt: '',
  promptEn: '',
  sceneType: '中景',
  cameraMove: '固定镜头',
  lines: '',
  duration: 15,
  assetIds: [] as number[],
  followLast: true,
  draftContent: ''
})

const currentEpisode = computed(() => episodes.value.find(item => item.id === currentEpisodeId.value) || null)
const currentShots = computed(() => shotsByEpisode.value[currentEpisodeId.value || 0] || [])
const visibleShots = computed(() => {
  if (filterKind.value === 'all') return currentShots.value
  const key = `${filterKind.value}Status` as 'promptStatus' | 'imageStatus' | 'videoStatus'
  return currentShots.value.filter(shot => (shot[key] || GenerationStatus.Pending) !== GenerationStatus.Pending)
})
const bindableAssets = computed(() => assets.value.filter(asset => asset.status === AssetStatus.Confirmed))

const taskKey = (shotId: number, kind: GenerateKind) => `${kind}:${shotId}`

const getDurationSeconds = (templateKey: string): 10 | 12 | 15 => {
  if (templateKey.includes('10')) return 10
  if (templateKey.includes('12')) return 12
  return 15
}

const fetchAssets = async () => {
  const res = await assetApi.list(projectId)
  assets.value = (res.data.list || res.data.records || []) as AssetVO[]
}

const fetchEpisodes = async () => {
  const episodeRes = await contentApi.listEpisodes(projectId)
  episodes.value = episodeRes.data || []
  if (!currentEpisodeId.value && episodes.value.length > 0) {
    currentEpisodeId.value = episodes.value[0].id
  }
  episodes.value.forEach((episode) => {
    splitRuleByEpisode.value[episode.id] ||= SHOT_SPLIT_TEMPLATE_OPTIONS[2].value
  })
}

const fetchShots = async (episodeId: number) => {
  loadingShots.value = true
  try {
    const res = await contentApi.listShots(episodeId)
    shotsByEpisode.value[episodeId] = (res.data || []).sort((a, b) => a.sortOrder - b.sortOrder)
  } finally {
    loadingShots.value = false
  }
}

const fetchProjectDefaults = async () => {
  const res = await projectApi.getDetail(projectId)
  projectDefaults.value = {
    ratio: res.data.ratio || '16:9',
    definition: res.data.definition || '1080P'
  }
}

const selectEpisode = async (episodeId: number) => {
  currentEpisodeId.value = episodeId
  selectedShotIds.value = []
  await fetchShots(episodeId)
}

const toggleExpandEpisode = (episodeId: number) => {
  expandedEpisodeIds.value = expandedEpisodeIds.value.includes(episodeId)
    ? expandedEpisodeIds.value.filter(id => id !== episodeId)
    : [...expandedEpisodeIds.value, episodeId]
}

const getEpisodeAssetNames = (episodeId: number) => {
  const names = new Set<string>()
  const assetIds = episodes.value.find(item => item.id === episodeId)?.assetIds || []
  assetIds.forEach((id) => {
    const asset = assets.value.find(item => item.id === id)
    if (asset) names.add(asset.name)
  })
  ;(shotsByEpisode.value[episodeId] || []).forEach((shot) => {
    shot.assetRefs.forEach((asset) => names.add(asset.assetName))
  })
  return [...names]
}

const episodeHasShots = (episodeId: number) => (shotsByEpisode.value[episodeId] || []).length > 0

const waitForTask = async (taskId: number) => {
  const maxPolls = 240
  for (let count = 0; count < maxPolls; count += 1) {
    const res = await taskApi.getStatus(taskId)
    const task = res.data
    if (task.status === AiTaskStatus.Success || task.status === AiTaskStatus.Failed) {
      return task
    }
    await new Promise(resolve => window.setTimeout(resolve, count < 3 ? 3000 : 5000))
  }
  throw new Error('任务轮询超时')
}

const waitForBatch = async (batchId: string, taskIds: number[]) => {
  const maxPolls = 240
  for (let count = 0; count < maxPolls; count += 1) {
    const res = await taskApi.getBatchStatus({ batchId, taskIds })
    const tasks = res.data.tasks || []
    if (tasks.length > 0 && tasks.every(task => task.status === AiTaskStatus.Success || task.status === AiTaskStatus.Failed)) {
      return tasks
    }
    await new Promise(resolve => window.setTimeout(resolve, count < 3 ? 3000 : 5000))
  }
  throw new Error('批量任务轮询超时')
}

const confirmSplitEpisode = async (episode: EpisodeVO) => {
  const overwrite = episodeHasShots(episode.id)
  if (overwrite) {
    await ElMessageBox.confirm('重新拆分将覆盖现有分镜，是否确认？', '重新拆分确认', { type: 'warning' })
  }
  splittingEpisodeId.value = episode.id
  try {
    const templateKey = splitRuleByEpisode.value[episode.id] || SHOT_SPLIT_TEMPLATE_OPTIONS[2].value
    const res = await contentApi.splitShots(episode.id, {
      templateKey,
      durationSeconds: getDurationSeconds(templateKey),
      overwrite
    })
    const task = await waitForTask(res.data.taskId)
    if (task.status === AiTaskStatus.Failed) {
      throw new Error(task.errorMsg || '分镜拆分失败')
    }
    await fetchShots(episode.id)
    ElMessage.success('分镜拆分完成')
  } catch (error: any) {
    ElMessage.error(error.message || '分镜拆分失败')
  } finally {
    splittingEpisodeId.value = null
  }
}

const toggleSelect = (shotId: number) => {
  selectedShotIds.value = selectedShotIds.value.includes(shotId)
    ? selectedShotIds.value.filter(id => id !== shotId)
    : [...selectedShotIds.value, shotId]
}

const generationMeta = (shot: ShotVO, kind: GenerateKind) => {
  const status = (shot[`${kind}Status` as const] || GenerationStatus.Pending) as GenerationStatus
  const statusMeta = GENERATION_STATUS_MAP[status]
  return {
    ...statusMeta,
    loading: Boolean(taskingMap.value[taskKey(shot.id, kind)])
  }
}

const ensureGenerateConfirm = async (kind: GenerateKind) => {
  if (kind === 'prompt') return
  await ElMessageBox.confirm('当前参数与全局配置不一致是否确定生成', '参数确认', { type: 'warning' })
}

const runGenerate = async (shot: ShotVO, kind: GenerateKind) => {
  try {
    if (kind !== 'prompt') {
      await ensureGenerateConfirm(kind)
    }
    taskingMap.value[taskKey(shot.id, kind)] = true
    const payload = {
      ratio: projectDefaults.value.ratio as BatchGenerateRequest['ratio'],
      definition: projectDefaults.value.definition as BatchGenerateRequest['definition']
    }
    const res = kind === 'prompt'
      ? await contentApi.generatePrompt(shot.id)
      : kind === 'image'
        ? await contentApi.generateImage(shot.id, payload)
        : await contentApi.generateVideo(shot.id, payload)
    const task = await waitForTask(res.data.taskId)
    if (task.status === AiTaskStatus.Failed) {
      throw new Error(task.errorMsg || '生成失败')
    }
    if (currentEpisodeId.value) {
      await fetchShots(currentEpisodeId.value)
    }
    ElMessage.success(`${kind === 'prompt' ? '提示词' : kind === 'image' ? '图片' : '视频'}生成完成`)
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    delete taskingMap.value[taskKey(shot.id, kind)]
  }
}

const batchGenerate = async (kind: GenerateKind) => {
  if (!currentEpisodeId.value || selectedShotIds.value.length === 0) return
  try {
    if (kind !== 'prompt') {
      await ensureGenerateConfirm(kind)
    }
    selectedShotIds.value.forEach((shotId) => {
      taskingMap.value[taskKey(shotId, kind)] = true
    })
    const payload: BatchGenerateRequest = {
      shotIds: selectedShotIds.value,
      ratio: projectDefaults.value.ratio as BatchGenerateRequest['ratio'],
      definition: projectDefaults.value.definition as BatchGenerateRequest['definition']
    }
    const res = kind === 'prompt'
      ? await contentApi.batchGeneratePrompt(currentEpisodeId.value, payload)
      : kind === 'image'
        ? await contentApi.batchGenerateImage(currentEpisodeId.value, payload)
        : await contentApi.batchGenerateVideo(currentEpisodeId.value, payload)
    const tasks = await waitForBatch(res.data.batchId, res.data.taskIds)
    if (tasks.some(task => task.status === AiTaskStatus.Failed)) {
      ElMessage.warning('批量任务已完成，部分分镜生成失败')
    } else {
      ElMessage.success('批量生成完成')
    }
    await fetchShots(currentEpisodeId.value)
  } catch (error: any) {
    ElMessage.error(error.message || '批量生成失败')
  } finally {
    selectedShotIds.value.forEach((shotId) => {
      delete taskingMap.value[taskKey(shotId, kind)]
    })
  }
}

const openCreateShotDialog = () => {
  editingShot.value = null
  shotForm.prompt = ''
  shotForm.promptEn = ''
  shotForm.sceneType = '中景'
  shotForm.cameraMove = '固定镜头'
  shotForm.lines = ''
  shotForm.duration = 15
  shotForm.assetIds = []
  shotForm.followLast = true
  shotForm.draftContent = ''
  shotDialogVisible.value = true
}

const openEditShotDialog = (shot: ShotVO) => {
  editingShot.value = shot
  shotForm.prompt = shot.prompt || ''
  shotForm.promptEn = shot.promptEn || ''
  shotForm.sceneType = shot.sceneType || '中景'
  shotForm.cameraMove = shot.cameraMove || '固定镜头'
  shotForm.lines = shot.lines || ''
  shotForm.duration = shot.duration || 15
  shotForm.assetIds = shot.assetRefs.map(item => item.assetId)
  shotForm.followLast = Boolean(shot.followLast)
  shotForm.draftContent = shot.draftContent || ''
  shotDialogVisible.value = true
}

const saveShotDraft = async (shotId: number) => {
  await contentApi.saveDraft(shotId, {
    draftContent: shotForm.draftContent.trim()
  })
}

const submitShot = async () => {
  if (!currentEpisodeId.value) return
  shotSaving.value = true
  try {
    const payload = {
      prompt: shotForm.prompt,
      promptEn: shotForm.promptEn,
      sceneType: shotForm.sceneType,
      cameraMove: shotForm.cameraMove,
      lines: shotForm.lines,
      duration: shotForm.duration,
      followLast: shotForm.followLast,
      assetIds: shotForm.assetIds
    }
    if (editingShot.value) {
      await contentApi.updateShot(editingShot.value.id, payload)
      if (shotForm.draftContent.trim()) {
        await saveShotDraft(editingShot.value.id)
      }
      ElMessage.success('分镜已更新')
    } else {
      const createRes = await contentApi.createShot(currentEpisodeId.value, payload)
      if (shotForm.draftContent.trim()) {
        await contentApi.saveDraft(createRes.data, {
          draftContent: shotForm.draftContent.trim()
        })
      }
      ElMessage.success('分镜已创建')
    }
    shotDialogVisible.value = false
    await fetchShots(currentEpisodeId.value)
  } finally {
    shotSaving.value = false
  }
}

const toggleFollowLast = async (shot: ShotVO, followLast: boolean) => {
  await contentApi.updateShot(shot.id, { followLast })
  shot.followLast = followLast
}

const deleteShot = async (shot: ShotVO) => {
  await ElMessageBox.confirm(`确认删除分镜 ${shot.sortOrder} 吗？`, '删除分镜', { type: 'warning' })
  await contentApi.deleteShot(shot.id)
  ElMessage.success('分镜已删除')
  if (currentEpisodeId.value) {
    await fetchShots(currentEpisodeId.value)
  }
}

watch(currentEpisodeId, async (episodeId) => {
  if (episodeId) {
    await fetchShots(episodeId)
  }
})

onMounted(async () => {
  await Promise.all([fetchProjectDefaults(), fetchAssets(), fetchEpisodes()])
  if (currentEpisodeId.value) {
    await fetchShots(currentEpisodeId.value)
  }
})
</script>

<style scoped lang="scss">
.shot-workbench-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
}

.episode-panel,
.toolbar {
  padding: 20px;
}

.eyebrow {
  margin: 0 0 6px;
  color: $accent-green;
  text-transform: uppercase;
  font-size: 12px;
}

.panel-head h3,
.toolbar h3 {
  margin: 0;
  color: $text-primary;
}

.episode-card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(100, 108, 255, 0.12);
  border-radius: 16px;

  &.active {
    border-color: rgba(100, 108, 255, 0.4);
    background: rgba(100, 108, 255, 0.06);
  }
}

.episode-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;

  strong {
    color: $text-primary;
  }

  p {
    margin: 6px 0 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.episode-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.split-select {
  width: 130px;
}

.episode-detail {
  margin-top: 12px;

  .content {
    margin: 0;
    color: $text-secondary;
    line-height: 1.6;
  }
}

.asset-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.05);
    color: $text-secondary;
    font-size: 12px;
  }
}

.workbench-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.shot-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shot-card {
  padding: 18px;
}

.shot-header,
.generate-row,
.preview-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.shot-title,
.shot-actions,
.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-row {
  margin: 14px 0;
}

.field {
  margin-top: 12px;

  span {
    display: block;
    margin-bottom: 4px;
    color: $text-tertiary;
    font-size: 12px;
  }

  p {
    margin: 0;
    color: $text-secondary;
    line-height: 1.6;
  }
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  color: $text-secondary;
  font-size: 13px;
}

.preview-row {
  margin-top: 14px;

  img,
  video,
  .empty-preview {
    width: calc(50% - 6px);
    height: 160px;
    border-radius: 12px;
    object-fit: cover;
    background: rgba(255, 255, 255, 0.04);
  }
}

.empty-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
}

.generate-row {
  margin-top: 14px;
}

.error-bar {
  margin-top: 12px;
  color: #f56c6c;
  font-size: 13px;
}

.full-width {
  width: 100%;
}

@media (max-width: 1100px) {
  .shot-workbench-page {
    grid-template-columns: 1fr;
  }
}
</style>

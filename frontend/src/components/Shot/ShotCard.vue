<template>
  <div class="shot-card card-glass border-neon" :class="{ selected }">
    <div class="shot-card-head">
      <el-checkbox :model-value="selected" @change="emit('toggle-select', shot.id)" />
      <div class="shot-title">
        <span>#{{ shot.sortOrder }}</span>
        <strong>{{ sceneTitle ? `[${sceneTitle}] ` : '' }}{{ promptTitle }}</strong>
      </div>
      <el-tag :type="statusMeta.type" size="small">{{ statusMeta.label }}</el-tag>
    </div>

    <div class="shot-main">
      <ShotPreview :shot="shot" />
      <div class="shot-body">
        <p class="prompt">{{ shot.prompt || '暂无中文提示词' }}</p>
        <p class="prompt-en">{{ shot.promptEn || 'No English prompt yet.' }}</p>
        <div class="shot-meta">
          <span>版本 v{{ shot.version }}</span>
          <span>尝试 {{ shot.generationAttempts }} 次</span>
          <span v-if="shot.reviewComment">意见：{{ shot.reviewComment }}</span>
        </div>
      </div>
    </div>

    <div class="asset-strip">
      <span class="asset-label">绑定资产</span>
      <el-tag
        v-for="asset in shot.assetRefs"
        :key="asset.assetId"
        size="small"
        effect="plain"
      >
        {{ asset.assetName }}
      </el-tag>
      <el-button size="small" text @click="emit('bind-assets', shot)">
        <el-icon><Plus /></el-icon>
        添加
      </el-button>
    </div>

    <div class="shot-actions">
      <el-button size="small" @click="emit('regenerate', shot)">重新生成</el-button>
      <el-button size="small" type="success" :disabled="!canReview" @click="emit('review', shot, 'approve')">通过</el-button>
      <el-button size="small" type="danger" :disabled="!canReview" @click="emit('review', shot, 'reject')">打回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import ShotPreview from './ShotPreview.vue'
import { SHOT_STATUS_MAP } from '@/constants/status'
import { ShotStatus } from '@/types'
import type { ShotVO } from '@/types'

const props = defineProps<{
  shot: ShotVO
  selected?: boolean
  sceneTitle?: string
}>()

const emit = defineEmits<{
  'toggle-select': [shotId: number]
  review: [shot: ShotVO, action: 'approve' | 'reject']
  regenerate: [shot: ShotVO]
  'bind-assets': [shot: ShotVO]
}>()

const statusMeta = computed(() => SHOT_STATUS_MAP[props.shot.status])
const canReview = computed(() => props.shot.status === ShotStatus.WaitingReview || props.shot.status === ShotStatus.Rejected)
const promptTitle = computed(() => props.shot.prompt?.slice(0, 22) || '未命名分镜')
</script>

<style scoped lang="scss">
.shot-card {
  height: 280px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: 8px;
  overflow: hidden;

  &.selected {
    border-color: rgba(34, 211, 238, 0.72);
    box-shadow: 0 0 18px rgba(34, 211, 238, 0.16);
  }
}

.shot-card-head {
  display: grid;
  grid-template-columns: 26px 1fr auto;
  align-items: center;
  gap: 10px;
}

.shot-title {
  min-width: 0;
  display: flex;
  gap: 8px;
  align-items: center;

  span {
    color: $accent-green;
    font-size: 13px;
  }

  strong {
    min-width: 0;
    color: $text-primary;
    font-size: 15px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.shot-main {
  display: flex;
  gap: 14px;
  min-height: 128px;
}

.shot-body {
  min-width: 0;
  flex: 1;
}

.prompt,
.prompt-en {
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.prompt {
  color: $text-primary;
  line-height: 1.5;
  -webkit-line-clamp: 2;
}

.prompt-en {
  margin-top: 8px;
  color: $text-secondary;
  font-size: 12px;
  line-height: 1.45;
  -webkit-line-clamp: 2;
}

.shot-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  color: $text-tertiary;
  font-size: 12px;
}

.asset-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  overflow: hidden;
}

.asset-label {
  color: $text-secondary;
  font-size: 12px;
  flex: 0 0 auto;
}

.shot-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 768px) {
  .shot-card {
    height: auto;
    min-height: 420px;
  }

  .shot-main {
    flex-direction: column;
  }
}
</style>

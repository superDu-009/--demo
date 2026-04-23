<template>
  <div class="shot-preview">
    <video
      v-if="shot.generatedVideoUrl"
      class="shot-video"
      :src="shot.generatedVideoUrl"
      controls
      preload="none"
    />
    <el-image
      v-else-if="shot.generatedImageUrl"
      class="shot-image"
      :src="shot.generatedImageUrl"
      fit="cover"
      lazy
    />
    <div v-else class="preview-empty">
      <el-icon :size="34"><Picture /></el-icon>
      <span>待生成</span>
    </div>

    <div v-if="overlayText" class="preview-overlay">
      {{ overlayText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import { AiTaskStatus, ShotStatus } from '@/types'
import type { ShotVO } from '@/types'

const props = defineProps<{
  shot: ShotVO
}>()

const overlayText = computed(() => {
  if (props.shot.status !== ShotStatus.Generating) return ''
  const task = props.shot.currentAiTask
  if (!task || task.status === AiTaskStatus.Submitting) return 'AI模型排队中，请耐心等待...'
  if (task.status === AiTaskStatus.Processing && task.taskType === 'image_gen') return '图片生成中...'
  if (task.status === AiTaskStatus.Processing && task.taskType === 'video_gen') return '视频生成中...'
  return ''
})
</script>

<style scoped lang="scss">
.shot-preview {
  position: relative;
  width: 178px;
  height: 128px;
  flex: 0 0 178px;
  overflow: hidden;
  border: 1px solid rgba(100, 108, 255, 0.22);
  border-radius: 8px;
  background: rgba(100, 108, 255, 0.06);
}

.shot-image,
.shot-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.preview-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: $text-tertiary;
  font-size: 13px;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: #fff;
  font-size: 13px;
  text-align: center;
  background: rgba(5, 8, 18, 0.72);
  backdrop-filter: blur(6px);
}

@media (max-width: 768px) {
  .shot-preview {
    width: 100%;
    height: 180px;
    flex-basis: auto;
  }
}
</style>

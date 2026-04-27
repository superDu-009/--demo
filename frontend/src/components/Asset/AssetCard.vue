<template>
  <article class="asset-card card-glass border-neon hud-corner">
    <div class="cover scanline">
      <img v-if="coverUrl" :src="coverUrl" alt="资产缩略图" />
      <div v-else class="empty-cover">待生成</div>
      <div v-if="safeChildPreviewList.length > 0" class="child-strip">
        <img v-for="url in safeChildPreviewList" :key="url" :src="url" alt="子资产缩略图" />
      </div>
    </div>

    <div class="body">
      <div class="header">
        <div>
          <h3>{{ asset.name }}</h3>
          <p>{{ typeLabel }}</p>
        </div>
        <el-tag :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
      </div>

      <p class="description">{{ asset.description || '未填写资产描述。' }}</p>

      <div class="tree-line" v-if="safeParentAssetNames.length > 0">
        <span>父资产：</span>{{ safeParentAssetNames.join(' / ') }}
      </div>

      <div class="actions">
        <el-button
          class="btn-gradient"
          size="small"
          :loading="generating"
          :disabled="generating"
          @click="$emit('generate', asset)"
        >
          {{ generateText }}
        </el-button>
        <el-button size="small" @click="$emit('edit', asset)">编辑</el-button>
        <el-button size="small" type="danger" plain @click="$emit('delete', asset)">删除</el-button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { AssetType, GenerationStatus } from '@/types'
import { ASSET_STATUS_MAP, GENERATION_STATUS_MAP } from '@/constants/status'
import { normalizeMediaUrl } from '@/utils/media'
import type { AssetVO } from '@/types'

const props = defineProps<{
  asset: AssetVO
  generatingStatus?: GenerationStatus
  parentAssetNames?: string[]
  childPreviewList?: string[]
}>()

defineEmits<{
  edit: [asset: AssetVO]
  delete: [asset: AssetVO]
  generate: [asset: AssetVO]
}>()

const statusMeta = computed(() => ASSET_STATUS_MAP[props.asset.status])
const safeParentAssetNames = computed(() => props.parentAssetNames || [])
const safeChildPreviewList = computed(() => (props.childPreviewList || []).map(normalizeMediaUrl).filter(Boolean))
const typeLabel = computed(() => {
  const map: Record<AssetType, string> = {
    character: '角色',
    scene: '场景',
    prop: '道具',
    voice: '声音'
  }
  return map[props.asset.assetType]
})
const coverUrl = computed(() => normalizeMediaUrl(props.asset.referenceImages?.[0]))
const generating = computed(() => props.generatingStatus === GenerationStatus.Processing)
const generateText = computed(() => GENERATION_STATUS_MAP[props.generatingStatus ?? GenerationStatus.Pending].actionText)
</script>

<style scoped lang="scss">
.asset-card {
  overflow: hidden;
}

.cover {
  position: relative;
  height: 180px;
  background:
    linear-gradient(135deg, rgba(92, 241, 255, 0.1), rgba(255, 204, 102, 0.04)),
    rgba(255, 255, 255, 0.04);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.empty-cover {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
  font-family: $font-display;
  letter-spacing: 0.14em;
}

.child-strip {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: flex;
  gap: 6px;

  img {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    border: 2px solid rgba(255, 255, 255, 0.72);
  }
}

.body {
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 12px;

  h3 {
    margin: 0;
    color: $text-primary;
    font-family: $font-display;
    font-size: 22px;
  }

  p {
    margin: 6px 0 0;
    color: $text-secondary;
    font-size: 12px;
  }
}

.description,
.tree-line {
  margin: 12px 0 0;
  color: $text-secondary;
  line-height: 1.6;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
</style>

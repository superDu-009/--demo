<!-- components/Asset/AssetCard.vue — 资产卡片组件 -->
<template>
  <div class="asset-card card-glass border-neon">
    <!-- 资产封面 -->
    <div class="asset-cover">
      <img 
        v-if="asset.referenceImages && asset.referenceImages.length > 0" 
        :src="asset.referenceImages[0]" 
        alt="资产封面"
        class="cover-img"
      >
      <div v-else class="empty-cover">
        <el-icon :size="48" class="empty-icon"><Picture /></el-icon>
      </div>
      <!-- 状态标签 -->
      <div class="status-tag" :class="`status-${asset.status}`">
        {{ statusLabel }}
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="card-content">
      <h3 class="asset-name">{{ asset.name }}</h3>
      <div class="asset-type-tag">
        <el-tag :type="tagType" size="small">{{ typeLabel }}</el-tag>
      </div>
      <p class="asset-desc" v-if="asset.description">{{ asset.description }}</p>

      <!-- 操作按钮 -->
      <div class="card-actions">
        <el-button size="small" @click="$emit('edit', asset)">编辑</el-button>
        <el-button 
          size="small" 
          type="success" 
          v-if="asset.status === 0"
          @click="$emit('confirm', asset)"
        >确认</el-button>
        <el-button 
          size="small" 
          type="danger" 
          @click="$emit('delete', asset)"
        >删除</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import type { AssetVO } from '@/types'
import { ASSET_STATUS_MAP } from '@/constants/status'

const props = defineProps<{
  asset: AssetVO
}>()

const emit = defineEmits<{
  edit: [asset: AssetVO]
  delete: [asset: AssetVO]
  confirm: [asset: AssetVO]
}>()

const statusLabel = computed(() => ASSET_STATUS_MAP[props.asset.status]?.label || '未知')

// 资产类型标签文本
const typeLabel = computed(() => {
  const map: Record<string, string> = {
    character: '角色',
    scene: '场景',
    prop: '物品',
    voice: '声音'
  }
  return map[props.asset.assetType] || props.asset.assetType
})

// 标签类型
const tagType = computed(() => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
    character: 'primary',
    scene: 'success',
    prop: 'warning',
    voice: 'info'
  }
  return map[props.asset.assetType] || 'primary'
})
</script>

<style scoped lang="scss">
.asset-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.asset-cover {
  position: relative;
  height: 160px;
  width: 100%;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
  background: rgba(100, 108, 255, 0.05);

  .cover-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }

  .empty-cover {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $text-tertiary;
  }

  .status-tag {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 12px;
    color: #fff;
    backdrop-filter: blur(8px);
    background: rgba(144, 147, 153, 0.8);

    &.status-1 {
      background: rgba(103, 194, 58, 0.8);
    }

    &.status-2 {
      background: rgba(245, 108, 108, 0.8);
    }
  }
}

.asset-card:hover .cover-img {
  transform: scale(1.05);
}

.card-content {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.asset-name {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-type-tag {
  margin-bottom: 12px;
}

.asset-desc {
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.4;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  flex: 1;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}
</style>

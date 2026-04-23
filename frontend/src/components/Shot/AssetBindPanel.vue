<template>
  <el-drawer
    v-model="visible"
    title="绑定资产"
    size="420px"
    destroy-on-close
  >
    <div v-if="shot" class="bind-panel">
      <div class="shot-line">
        <span>#{{ shot.sortOrder }}</span>
        <strong>{{ shot.prompt || '未填写提示词' }}</strong>
      </div>

      <el-segmented v-model="activeType" :options="typeOptions" class="type-tabs" />

      <div class="asset-list">
        <label
          v-for="asset in currentAssets"
          :key="asset.id"
          class="asset-row"
          :class="{ selected: selectedIds.includes(asset.id) }"
        >
          <el-checkbox :model-value="selectedIds.includes(asset.id)" @change="toggleAsset(asset.id)" />
          <img v-if="asset.referenceImages?.[0]" :src="asset.referenceImages[0]" alt="" class="asset-thumb">
          <div v-else class="asset-thumb asset-thumb-empty">
            <el-icon><Picture /></el-icon>
          </div>
          <div class="asset-info">
            <strong>{{ asset.name }}</strong>
            <span>{{ asset.description || '暂无描述' }}</span>
          </div>
        </label>
      </div>

      <EmptyState
        v-if="currentAssets.length === 0"
        type="asset"
        title="暂无可绑定资产"
        description="当前类型下没有 mock 资产。"
      />
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="btn-gradient" @click="submit">保存绑定</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import EmptyState from '@/components/Common/EmptyState.vue'
import { AssetType } from '@/types'
import type { AssetVO, ShotVO } from '@/types'

const props = defineProps<{
  modelValue: boolean
  shot: ShotVO | null
  assets: AssetVO[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [assetIds: number[]]
}>()

const activeType = ref<AssetType>(AssetType.Character)
const selectedIds = ref<number[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const typeOptions = [
  { value: AssetType.Character, label: '角色' },
  { value: AssetType.Scene, label: '场景' },
  { value: AssetType.Prop, label: '物品' },
  { value: AssetType.Voice, label: '声音' }
]

const currentAssets = computed(() => props.assets.filter(asset => asset.assetType === activeType.value))

watch(() => props.shot, (shot) => {
  selectedIds.value = shot?.assetRefs.map(item => item.assetId) || []
}, { immediate: true })

const toggleAsset = (assetId: number) => {
  selectedIds.value = selectedIds.value.includes(assetId)
    ? selectedIds.value.filter(id => id !== assetId)
    : [...selectedIds.value, assetId]
}

const submit = () => {
  emit('save', selectedIds.value)
  visible.value = false
}
</script>

<style scoped lang="scss">
.bind-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shot-line {
  padding: 14px;
  border: 1px solid rgba(100, 108, 255, 0.18);
  border-radius: 8px;
  background: rgba(100, 108, 255, 0.05);

  span {
    color: $accent-green;
    margin-right: 8px;
  }

  strong {
    color: $text-primary;
    font-size: 14px;
  }
}

.type-tabs {
  width: 100%;
}

.asset-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.asset-row {
  display: grid;
  grid-template-columns: 28px 52px 1fr;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border: 1px solid rgba(100, 108, 255, 0.16);
  border-radius: 8px;
  background: rgba(100, 108, 255, 0.04);
  cursor: pointer;

  &.selected {
    border-color: rgba(34, 211, 238, 0.72);
    background: rgba(34, 211, 238, 0.08);
  }
}

.asset-thumb {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  background: rgba(100, 108, 255, 0.08);
}

.asset-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-tertiary;
}

.asset-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: $text-primary;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }
}
</style>

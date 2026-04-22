<!-- components/Common/EmptyState.vue — 通用空状态组件 -->
<template>
  <div class="empty-state">
    <div class="empty-icon">
      <component :is="iconComponent" :size="80" class="icon" />
    </div>
    <p class="empty-title text-neon">{{ title }}</p>
    <p class="empty-desc">{{ description }}</p>
    <slot>
      <el-button class="btn-gradient" v-if="showAction" @click="$emit('action')">
        {{ actionText }}
      </el-button>
    </slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Folder, Picture, Film, DataAnalysis, Plus } from '@element-plus/icons-vue'

const props = defineProps<{
  type?: 'default' | 'project' | 'asset' | 'shot' | 'stat'
  title?: string
  description?: string
  showAction?: boolean
  actionText?: string
}>()

const emit = defineEmits<{
  action: []
}>()

const defaultProps = withDefaults(props, {
  type: 'default',
  title: '暂无数据',
  description: '快去创建第一条内容吧~',
  showAction: false,
  actionText: '立即创建'
})

// 图标映射
const iconComponent = computed(() => {
  const map: Record<string, any> = {
    project: Folder,
    asset: Picture,
    shot: Film,
    stat: DataAnalysis,
    default: Folder
  }
  return map[props.type!] || Folder
})
</script>

<style scoped lang="scss">
.empty-state {
  padding: 60px 20px;
  text-align: center;

  .empty-icon {
    margin-bottom: 24px;
    opacity: 0.6;
    animation: float 3s ease-in-out infinite;
    .icon {
      background: $primary-gradient;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .empty-title {
    font-size: 20px;
    font-weight: 600;
    margin: 0 0 12px 0;
  }

  .empty-desc {
    font-size: 14px;
    color: $text-secondary;
    margin: 0 0 24px 0;
    max-width: 400px;
    margin-left: auto;
    margin-right: auto;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}
</style>
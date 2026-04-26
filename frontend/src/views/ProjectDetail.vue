<template>
  <div class="project-detail-page">
    <section class="project-overview card-glass border-neon hud-panel hud-corner" v-loading="loading">
      <div>
        <p class="eyebrow">Project Detail</p>
        <h2 class="hud-title">{{ projectInfo?.name || '项目详情' }}</h2>
        <p class="desc">{{ projectInfo?.description || '未填写项目描述。' }}</p>
      </div>
      <div class="meta-grid">
        <div class="meta-card">
          <span>默认比例</span>
          <strong>{{ projectInfo?.ratio || '16:9' }}</strong>
        </div>
        <div class="meta-card">
          <span>默认清晰度</span>
          <strong>{{ projectInfo?.definition || '1080P' }}</strong>
        </div>
        <div class="meta-card">
          <span>风格</span>
          <strong>{{ projectInfo?.style || '未设置' }}</strong>
        </div>
      </div>
    </section>

    <section class="tab-nav">
      <button
        v-for="tab in tabList"
        :key="tab.name"
        class="tab-item hud-corner"
        :class="{ active: route.name === tab.name }"
        @click="router.push({ name: tab.name, params: route.params })"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </button>
    </section>

    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Film, Notebook } from '@element-plus/icons-vue'
import { projectApi } from '@/api/project'
import type { ProjectVO } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const projectInfo = ref<ProjectVO | null>(null)

const tabList = [
  { name: 'ScriptPreview', label: '剧本预览', icon: Notebook },
  { name: 'ShotWorkbench', label: '分镜工作台', icon: Film },
  { name: 'AssetLibrary', label: '资产库', icon: Collection }
]

const fetchProjectDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await projectApi.getDetail(id)
    projectInfo.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const id = Number(route.params.id)
  if (id) fetchProjectDetail(id)
})

watch(() => route.params.id, (id) => {
  if (id) fetchProjectDetail(Number(id))
})
</script>

<style scoped lang="scss">
.project-detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.project-overview {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;

  .eyebrow {
    margin: 0 0 6px;
    color: $accent-yellow;
    font-size: 12px;
  }

  h2 {
    margin: 0;
    color: $text-primary;
    font-size: 38px;
  }

  .desc {
    margin: 10px 0 0;
    max-width: 680px;
    color: $text-secondary;
    line-height: 1.7;
  }
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  min-width: 360px;
}

.meta-card {
  position: relative;
  padding: 14px 16px;
  border: 1px solid rgba(92, 241, 255, 0.18);
  border-radius: 16px;
  background: rgba(92, 241, 255, 0.055);

  span,
  strong {
    display: block;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }

  strong {
    margin-top: 6px;
    color: $text-primary;
    font-family: $font-display;
    font-size: 18px;
  }
}

.tab-nav {
  display: flex;
  gap: 12px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(92, 241, 255, 0.18);
  border-radius: 14px;
  background: rgba(92, 241, 255, 0.045);
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;

  &.active {
    color: #031018;
    background: $primary-gradient;
    border-color: transparent;
    box-shadow: 0 0 22px rgba(92, 241, 255, 0.22);
  }

  &:hover {
    border-color: rgba(92, 241, 255, 0.42);
    transform: translateY(-1px);
  }
}

@media (max-width: 960px) {
  .project-overview {
    flex-direction: column;
  }

  .meta-grid {
    min-width: 0;
    grid-template-columns: repeat(3, 1fr);
  }

  .tab-nav {
    overflow: auto;
  }
}
</style>

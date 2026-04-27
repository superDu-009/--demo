<template>
  <div class="script-preview-page">
    <section class="toolbar card-glass border-neon hud-panel hud-corner">
      <div>
        <p class="eyebrow">Script Preview</p>
        <h3 class="hud-title">剧本预览</h3>
      </div>
      <div class="toolbar-actions">
        <el-tag :type="parseMeta.type">{{ parseMeta.label }}</el-tag>
        <el-button class="btn-gradient" :loading="analysisSubmitting" @click="analysisDialogVisible = true">
          剧本分析
        </el-button>
      </div>
    </section>

    <section class="reader card-glass border-neon hud-corner scanline" v-loading="textLoading">
      <div class="reader-body">
        <pre v-if="sourceText" class="page-text">{{ sourceText }}</pre>
        <el-empty v-else description="当前没有可展示的剧本文本" />
      </div>
    </section>

    <el-dialog v-model="analysisDialogVisible" title="剧本分析" width="460px">
      <el-form label-width="88px">
        <el-form-item label="拆分规则">
          <el-select v-model="selectedTemplateKey" class="full-width">
            <el-option
              v-for="item in SCRIPT_ANALYSIS_TEMPLATE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="parseStatus === ParseStatus.Failed"
        :title="parseError || '解析失败'"
        type="error"
        :closable="false"
        show-icon
      />
      <template #footer>
        <el-button @click="analysisDialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="analysisSubmitting" @click="runAnalysis">
          {{ parseStatus === ParseStatus.Failed ? '手动重试' : '开始分析' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SCRIPT_ANALYSIS_TEMPLATE_OPTIONS } from '@/constants/options'
import { PARSE_STATUS_MAP } from '@/constants/status'
import { contentApi } from '@/api/content'
import { projectApi } from '@/api/project'
import { ParseStatus } from '@/types'
import type { ProjectVO } from '@/types'

const route = useRoute()

const analysisDialogVisible = ref(false)
const analysisSubmitting = ref(false)
const textLoading = ref(false)
const projectInfo = ref<ProjectVO | null>(null)
const sourceText = ref('')
const parseStatus = ref<ParseStatus>(ParseStatus.Pending)
const parseError = ref('')
const analyzeTaskId = ref<number | null>(null)
const selectedTemplateKey = ref(SCRIPT_ANALYSIS_TEMPLATE_OPTIONS[0].value)
let analyzeTimer: number | null = null

const parseMeta = computed(() => PARSE_STATUS_MAP[parseStatus.value])

const stopAnalyzePolling = () => {
  if (analyzeTimer) {
    window.clearTimeout(analyzeTimer)
    analyzeTimer = null
  }
}

const scheduleAnalyzeStatus = () => {
  stopAnalyzePolling()
  if (parseStatus.value !== ParseStatus.Processing) return
  analyzeTimer = window.setTimeout(async () => {
    await fetchAnalyzeStatus()
    if (parseStatus.value === ParseStatus.Processing) {
      scheduleAnalyzeStatus()
    }
  }, 3000)
}

const fetchAnalyzeStatus = async () => {
  const res = await contentApi.getAnalyzeStatus(Number(route.params.id))
  parseStatus.value = res.data.parseStatus || ParseStatus.Pending
  parseError.value = res.data.parseError || ''
  analyzeTaskId.value = res.data.taskId || null
}

const loadNovelText = async (project: ProjectVO) => {
  if (!project.novelTosPath) return
  textLoading.value = true
  try {
    const response = await fetch(project.novelTosPath)
    const text = await response.text()
    sourceText.value = text
  } catch {
    parseError.value = '剧本文本加载失败，请确认 TOS 文件可直读。'
  } finally {
    textLoading.value = false
  }
}

const runAnalysis = async () => {
  analysisSubmitting.value = true
  try {
    const res = await contentApi.analyzeEpisodes(Number(route.params.id))
    analyzeTaskId.value = res.data
    parseStatus.value = ParseStatus.Processing
    parseError.value = ''
    analysisDialogVisible.value = false
    scheduleAnalyzeStatus()
    ElMessage.success('剧本分析任务已提交')
  } finally {
    analysisSubmitting.value = false
  }
}

const fetchData = async () => {
  const projectId = Number(route.params.id)
  const [projectRes] = await Promise.all([
    projectApi.getDetail(projectId),
    fetchAnalyzeStatus().catch(() => undefined)
  ])
  projectInfo.value = projectRes.data
  await loadNovelText(projectRes.data)
  if (parseStatus.value === ParseStatus.Processing) {
    scheduleAnalyzeStatus()
  }
}

onMounted(async () => {
  await fetchData()
})

watch(parseStatus, (status) => {
  if (status !== ParseStatus.Processing) {
    stopAnalyzePolling()
  }
})

onBeforeUnmount(stopAnalyzePolling)
</script>

<style scoped lang="scss">
.script-preview-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar,
.reader {
  padding: 22px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 6px;
  color: $accent-green;
  color: $accent-yellow;
  font-size: 12px;
}

.toolbar h3 {
  margin: 0;
  color: $text-primary;
  font-size: 28px;
}

.toolbar p {
  margin: 8px 0 0;
  color: $text-secondary;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reader {
  height: clamp(520px, 68vh, 760px);
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(90deg, rgba(92, 241, 255, 0.06), transparent 18%),
    rgba(7, 18, 30, 0.72);
}

.reader-body {
  flex: 1;
  padding: 28px;
  border: 1px solid rgba(92, 241, 255, 0.12);
  border-radius: 16px;
  background: rgba(3, 7, 13, 0.38);
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.page-text {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.86;
  font-size: 14px;
  color: $text-primary;
  font-family: $font-body;
}

.full-width {
  width: 100%;
}
</style>

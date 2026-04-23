<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="92px"
    class="asset-form"
  >
    <el-form-item label="资产类型" prop="assetType">
      <el-segmented
        v-model="form.assetType"
        :options="assetTypeOptions"
        :disabled="mode === 'edit'"
      />
    </el-form-item>

    <el-form-item label="资产名称" prop="name">
      <el-input
        v-model="form.name"
        placeholder="例如：女主角林夏 / 雨夜天台 / 古铜钥匙"
        maxlength="80"
        show-word-limit
      />
    </el-form-item>

    <el-form-item label="描述文本" prop="description">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="4"
        placeholder="记录外观、性格、服饰、场景氛围或声音特征，供后续 AI 生成复用。"
        maxlength="1000"
        show-word-limit
      />
    </el-form-item>

    <el-form-item v-if="form.assetType !== AssetType.Voice" label="参考图">
      <div class="reference-list">
        <div
          v-for="(url, index) in form.referenceImages"
          :key="`${url}-${index}`"
          class="reference-item"
        >
          <el-image :src="url" fit="cover" class="reference-image" />
          <el-tag v-if="index === 0" size="small" type="success" class="main-tag">主图</el-tag>
          <el-button
            class="remove-reference"
            type="danger"
            size="small"
            circle
            @click="removeReference(index)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <TosUpload
          class="reference-uploader"
          :model-value="uploadTempUrl"
          :project-id="projectId"
          file-type="asset"
          :project-dir="`projects/${projectId}/assets`"
          button-text="上传参考图"
          tip-text="支持 png/jpg/webp，单张最大10MB"
          accept=".png,.jpg,.jpeg,.webp"
          :allowed-types="['image/png', 'image/jpeg', 'image/webp']"
          :max-file-size="10 * 1024 * 1024"
          :show-preview="false"
          @success="addUploadedReference"
          @update:model-value="uploadTempUrl = $event"
        />
      </div>
    </el-form-item>

    <el-form-item v-else label="声音文件">
      <div class="audio-field">
        <TosUpload
          v-model="audioUrl"
          :project-id="projectId"
          file-type="asset"
          :project-dir="`projects/${projectId}/assets/voice`"
          button-text="上传声音资产"
          tip-text="支持 mp3/wav/m4a，单文件最大30MB"
          accept=".mp3,.wav,.m4a,.aac"
          :allowed-types="['audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/mp4', 'audio/aac']"
          :max-file-size="30 * 1024 * 1024"
          :show-preview="false"
        />
        <audio
          v-if="audioUrl"
          class="audio-preview"
          :src="audioUrl"
          controls
          preload="none"
        />
        <el-button v-if="audioUrl" type="danger" plain @click="audioUrl = ''">
          清除声音
        </el-button>
      </div>
    </el-form-item>

    <el-form-item label="备用链接">
      <div class="reference-input">
        <el-input
          v-model="manualUrl"
          :placeholder="form.assetType === AssetType.Voice ? '粘贴音频 URL，回车添加' : '粘贴参考图 URL，回车添加'"
          clearable
          @keyup.enter="addManualUrl"
        />
        <el-button @click="addManualUrl">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>
    </el-form-item>

    <div class="style-grid">
      <el-form-item label="画风">
        <el-input v-model="form.stylePreset.artStyle" placeholder="赛璐璐 / 水墨 / 厚涂" />
      </el-form-item>
      <el-form-item label="色调">
        <el-input v-model="form.stylePreset.colorTone" placeholder="冷色 / 暖光 / 高对比" />
      </el-form-item>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Close, Plus } from '@element-plus/icons-vue'
import TosUpload from '@/components/Common/TosUpload.vue'
import { AssetType } from '@/types'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO } from '@/types'

interface AssetFormState {
  assetType: AssetType
  name: string
  description: string
  referenceImages: string[]
  stylePreset: Record<string, any>
}

const props = withDefaults(defineProps<{
  mode?: 'create' | 'edit'
  asset?: AssetVO | null
  defaultType?: AssetType | 'all'
  projectId?: number
}>(), {
  mode: 'create',
  asset: null,
  defaultType: AssetType.Character,
  projectId: 0
})

const formRef = ref<FormInstance>()
const manualUrl = ref('')
const uploadTempUrl = ref('')
const audioUrl = ref('')

const assetTypeOptions = [
  { label: '角色', value: AssetType.Character },
  { label: '场景', value: AssetType.Scene },
  { label: '物品', value: AssetType.Prop },
  { label: '声音', value: AssetType.Voice }
]

const form = reactive<AssetFormState>({
  assetType: AssetType.Character,
  name: '',
  description: '',
  referenceImages: [],
  stylePreset: {
    artStyle: '',
    colorTone: '',
    audioUrl: ''
  }
})

const rules: FormRules = {
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  name: [
    { required: true, message: '请输入资产名称', trigger: 'blur' },
    { min: 1, max: 80, message: '长度在 1 到 80 个字符', trigger: 'blur' }
  ]
}

const resetForm = () => {
  const fallbackType = props.defaultType === 'all' ? AssetType.Character : props.defaultType
  form.assetType = fallbackType
  form.name = ''
  form.description = ''
  form.referenceImages = []
  form.stylePreset = { artStyle: '', colorTone: '', audioUrl: '' }
  manualUrl.value = ''
  uploadTempUrl.value = ''
  audioUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (asset: AssetVO) => {
  form.assetType = asset.assetType
  form.name = asset.name
  form.description = asset.description || ''
  form.referenceImages = [...(asset.referenceImages || [])]
  form.stylePreset = {
    artStyle: asset.stylePreset?.artStyle || '',
    colorTone: asset.stylePreset?.colorTone || '',
    audioUrl: asset.stylePreset?.audioUrl || '',
    ...asset.stylePreset
  }
  audioUrl.value = String(asset.stylePreset?.audioUrl || '')
  manualUrl.value = ''
  uploadTempUrl.value = ''
  formRef.value?.clearValidate()
}

watch(
  () => [props.asset, props.mode, props.defaultType] as const,
  () => {
    if (props.mode === 'edit' && props.asset) {
      fillForm(props.asset)
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

const addUploadedReference = (url: string) => {
  if (!url) return
  if (!form.referenceImages) form.referenceImages = []
  form.referenceImages.push(url)
  uploadTempUrl.value = ''
}

const addManualUrl = () => {
  const url = manualUrl.value.trim()
  if (!url) return
  if (!/^https?:\/\//i.test(url) && !url.startsWith('/')) {
    ElMessage.warning('请填写有效的资源 URL')
    return
  }
  if (form.assetType === AssetType.Voice) {
    audioUrl.value = url
    manualUrl.value = ''
    return
  }
  if (!form.referenceImages) form.referenceImages = []
  form.referenceImages.push(url)
  manualUrl.value = ''
}

const removeReference = (index: number) => {
  form.referenceImages?.splice(index, 1)
}

const normalizeStylePreset = () => {
  const stylePreset = { ...(form.stylePreset || {}) }
  if (form.assetType === AssetType.Voice && audioUrl.value) {
    stylePreset.audioUrl = audioUrl.value
  }
  Object.keys(stylePreset).forEach((key) => {
    if (stylePreset[key] === '') delete stylePreset[key]
  })
  return Object.keys(stylePreset).length > 0 ? stylePreset : undefined
}

const validate = async (): Promise<AssetCreateRequest | AssetUpdateRequest | false> => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return false

  const payload: AssetCreateRequest = {
    assetType: form.assetType,
    name: form.name.trim(),
    description: form.description?.trim() || undefined,
    referenceImages: form.assetType === AssetType.Voice ? [] : form.referenceImages?.filter(Boolean),
    stylePreset: normalizeStylePreset()
  }

  if (props.mode === 'edit') {
    const { assetType, ...updatePayload } = payload
    return updatePayload
  }
  return payload
}

defineExpose({ validate, resetForm })
</script>

<style scoped lang="scss">
.asset-form {
  :deep(.el-segmented) {
    background: rgba(100, 108, 255, 0.08);
    border: 1px solid rgba(100, 108, 255, 0.18);
  }
}

.reference-list {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 12px;
}

.reference-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(100, 108, 255, 0.06);
  border: 1px solid rgba(100, 108, 255, 0.2);
}

.reference-image {
  width: 100%;
  height: 100%;
}

.main-tag {
  position: absolute;
  left: 8px;
  top: 8px;
}

.remove-reference {
  position: absolute;
  right: 8px;
  top: 8px;
}

.reference-input {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
}

.reference-uploader {
  grid-column: 1 / -1;
}

.audio-field {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audio-preview {
  width: 100%;
}

.style-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 768px) {
  .style-grid,
  .reference-input {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>

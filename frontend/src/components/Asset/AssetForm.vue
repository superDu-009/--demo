<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
    <el-form-item label="资产类型" prop="assetType">
      <el-select v-model="form.assetType" class="full-width" :disabled="mode === 'edit'">
        <el-option v-for="item in ASSET_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
    </el-form-item>
    <el-form-item label="资产名称" prop="name">
      <el-input v-model="form.name" maxlength="80" show-word-limit />
    </el-form-item>
    <el-form-item label="资产描述">
      <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
    </el-form-item>
    <el-form-item label="参考图">
      <div class="upload-box">
        <div class="preview-list">
          <div v-for="(url, index) in form.referenceImages" :key="`${url}-${index}`" class="preview-item">
            <img :src="url" alt="参考图" />
            <el-button circle size="small" type="danger" class="delete-btn" @click="removeReference(index)">×</el-button>
          </div>
        </div>
        <TosUpload
          v-model="uploadTemp"
          :project-id="projectId"
          file-type="asset"
          button-text="上传参考图"
          tip-text="最多 3 张"
          accept=".png,.jpg,.jpeg,.webp"
          :allowed-types="['image/png', 'image/jpeg', 'image/webp']"
          :max-file-size="10 * 1024 * 1024"
          :show-preview="false"
          @success="addReference"
        />
      </div>
    </el-form-item>
    <el-form-item label="是否子资产">
      <el-switch v-model="isSubAsset" />
    </el-form-item>
    <el-form-item v-if="isSubAsset" label="父资产">
      <el-select v-model="form.parentIds" class="full-width" multiple collapse-tags>
        <el-option
          v-for="item in parentCandidates"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="草稿内容">
      <el-input v-model="form.draftContent" type="textarea" :rows="3" maxlength="800" show-word-limit />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import TosUpload from '@/components/Common/TosUpload.vue'
import { ASSET_TYPE_OPTIONS } from '@/constants/options'
import { AssetType } from '@/types'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO } from '@/types'

const props = withDefaults(defineProps<{
  mode?: 'create' | 'edit'
  asset?: AssetVO | null
  projectId?: number
  assets?: AssetVO[]
}>(), {
  mode: 'create',
  asset: null,
  projectId: 0,
  assets: () => []
})

const formRef = ref<FormInstance>()
const uploadTemp = ref('')
const isSubAsset = ref(false)

const form = reactive({
  assetType: AssetType.Character,
  name: '',
  description: '',
  referenceImages: [] as string[],
  parentIds: [] as number[],
  draftContent: ''
})

const rules: FormRules = {
  assetType: [{ required: true, message: '请选择资产类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }]
}

const parentCandidates = computed(() => {
  return props.assets.filter(item => item.id !== props.asset?.id)
})

const fillForm = () => {
  form.assetType = props.asset?.assetType || AssetType.Character
  form.name = props.asset?.name || ''
  form.description = props.asset?.description || ''
  form.referenceImages = [...(props.asset?.referenceImages || [])]
  form.parentIds = [...(props.asset?.parentIds || [])]
  form.draftContent = props.asset?.draftContent || ''
  isSubAsset.value = form.parentIds.length > 0
}

watch(() => props.asset, fillForm, { immediate: true })

const addReference = (url: string) => {
  if (!url || form.referenceImages.length >= 3) return
  form.referenceImages.push(url)
  uploadTemp.value = ''
}

const removeReference = (index: number) => {
  form.referenceImages.splice(index, 1)
}

const validate = async (): Promise<AssetCreateRequest | AssetUpdateRequest | false> => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return false
  const payload: AssetCreateRequest = {
    assetType: form.assetType,
    name: form.name.trim(),
    description: form.description?.trim() || undefined,
    referenceImages: form.referenceImages,
    parentIds: isSubAsset.value ? form.parentIds : [],
    draftContent: form.draftContent?.trim() || undefined
  }
  if (props.mode === 'edit') {
    const { assetType, ...updatePayload } = payload
    return updatePayload
  }
  return payload
}

defineExpose({ validate })
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}

.upload-box {
  width: 100%;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.preview-item {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.delete-btn {
  position: absolute;
  top: 6px;
  right: 6px;
}
</style>

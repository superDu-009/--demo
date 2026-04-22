<!-- components/Common/TosUpload.vue — 通用TOS上传组件 -->
<template>
  <div class="tos-upload">
    <!-- 拖拽上传区域 -->
    <div
      class="upload-area upload-area-neon card-glass border-neon"
      :class="{
        'dragover': isDragover,
        'upload-area-disabled': disabled || uploading
      }"
      @click="handleClick"
      @drop.prevent="handleDrop"
      @dragover.prevent="handleDragover"
      @dragleave.prevent="handleDragleave"
    >
      <el-icon v-if="!uploading" class="upload-icon text-neon" :size="48"><UploadFilled /></el-icon>
      <el-progress v-else type="circle" :percentage="progress" :width="48" color="#646cff"/>
      
      <p class="upload-text" v-if="!uploading">
        <span class="text-neon">{{ buttonText }}</span><br>
        <span class="upload-tip">支持拖放上传，{{ tipText }}</span>
      </p>
      <p class="upload-text" v-else><span class="text-neon">上传中 {{ progress }}%</span></p>
      
      <input
        ref="fileInputRef"
        type="file"
        class="file-input"
        :accept="accept"
        :multiple="false"
        @change="handleFileChange"
      >
    </div>

    <!-- 上传成功展示 -->
    <div v-if="showPreview && fileUrl" class="preview-area card-glass">
      <!-- 图片预览 -->
      <el-image v-if="isImageType(fileUrl)" :src="fileUrl" class="preview-image" fit="cover" />
      <!-- 文件预览 -->
      <div v-else class="preview-file">
        <el-icon :size="32" class="text-neon"><Document /></el-icon>
        <span class="file-name">{{ fileName }}</span>
      </div>
      <el-button type="danger" size="small" icon="Delete" @click="handleDelete" class="delete-btn">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { UploadFilled, Document, Delete } from '@element-plus/icons-vue'
import { useTosUpload } from '@/composables/useTosUpload'
import type { UploadOptions } from '@/composables/useTosUpload'

// 组件属性
const props = defineProps<{
  // 按钮文字
  buttonText?: string
  // 提示文字
  tipText?: string
  // 接受的文件类型
  accept?: string
  // 最大文件大小，单位字节
  maxFileSize?: number
  // 允许的文件类型数组
  allowedTypes?: string[]
  // 项目ID
  projectId: number
  // 文件类型
  fileType: 'novel' | 'asset' | 'other'
  // 项目存储目录
  projectDir?: string
  // 是否禁用
  disabled?: boolean
  // 是否显示预览
  showPreview?: boolean
  // 已上传的文件地址
  modelValue?: string
}>()

// 默认值
const defaultProps = withDefaults(props, {
  buttonText: '点击上传文件',
  tipText: '单文件最大50MB',
  accept: '*',
  maxFileSize: 50 * 1024 * 1024,
  allowedTypes: () => ['image/png', 'image/jpeg', 'video/mp4', 'text/plain'],
  disabled: false,
  showPreview: true,
  modelValue: ''
})

// 事件
const emit = defineEmits<{
  'update:modelValue': [url: string]
  'success': [url: string]
  'error': [error: Error]
}>()

const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragover = ref(false)
const fileUrl = ref(props.modelValue)
const fileName = ref('')

// 使用上传composable
const { uploading, progress, upload } = useTosUpload()

// 计算属性
const acceptStr = computed(() => {
  return props.allowedTypes.join(',')
})

// 监听外部传入的modelValue变化
watch(() => props.modelValue, (val) => {
  fileUrl.value = val
})

// 处理点击上传区域
const handleClick = () => {
  if (props.disabled || uploading.value) return
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  await doUpload(file)
  
  // 重置input，允许重复选择同一个文件
  target.value = ''
}

// 处理拖拽
const handleDrop = (e: DragEvent) => {
  isDragover.value = false
  if (props.disabled || uploading.value) return

  const file = e.dataTransfer?.files?.[0]
  if (!file) return

  doUpload(file)
}

const handleDragover = () => {
  if (props.disabled || uploading.value) return
  isDragover.value = true
}

const handleDragleave = () => {
  isDragover.value = false
}

// 执行上传
const doUpload = async (file: File) => {
  try {
    fileName.value = file.name
    const options: UploadOptions = {
      projectId: props.projectId,
      fileType: props.fileType,
      projectDir: props.projectDir,
      maxFileSize: props.maxFileSize,
      allowedTypes: props.allowedTypes,
      onSuccess: (url) => {
        fileUrl.value = url
        emit('update:modelValue', url)
        emit('success', url)
      },
      onError: (error) => {
        emit('error', error)
      }
    }

    await upload(file, options)
  } catch (error) {
    console.error('上传失败', error)
  }
}

// 删除文件
const handleDelete = () => {
  fileUrl.value = ''
  fileName.value = ''
  emit('update:modelValue', '')
}

// 判断是否是图片类型
const isImageType = (url: string) => {
  return /\.(png|jpe?g|gif|webp)$/i.test(url)
}
</script>

<style scoped lang="scss">
.tos-upload {
  width: 100%;
}

.upload-area {
  position: relative;
  width: 100%;
  min-height: 150px;
  border: 2px dashed rgba(100, 108, 255, 0.3);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;

  &.dragover {
    border-color: $border-glow-color;
  }

  &-disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  .upload-icon {
    margin-bottom: 16px;
  }

  .upload-text {
    text-align: center;
    color: $text-primary;
    font-size: 14px;
    line-height: 22px;

    .upload-tip {
      color: $text-secondary;
      font-size: 12px;
    }
  }

  .file-input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    cursor: pointer;
  }
}

.preview-area {
  margin-top: 12px;
  position: relative;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;

  .preview-image {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  .preview-file {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;

    .file-name {
      font-size: 14px;
      color: $text-primary;
    }
  }

  .delete-btn {
    margin-left: auto;
  }
}
</style>

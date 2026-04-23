<template>
  <el-dialog
    v-model="visible"
    :title="action === 'approve' ? '审核通过' : '打回分镜'"
    width="460px"
    destroy-on-close
  >
    <div class="review-summary">
      <span>已选择</span>
      <strong>{{ count }}</strong>
      <span>个分镜</span>
    </div>
    <el-input
      v-if="action === 'reject'"
      v-model="comment"
      type="textarea"
      :rows="4"
      maxlength="300"
      show-word-limit
      placeholder="请输入打回原因，方便后续重新生成。"
    />
    <p v-else class="approve-tip">确认后这些分镜会进入已通过状态。</p>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :type="action === 'approve' ? 'success' : 'danger'" @click="submit">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  action: 'approve' | 'reject'
  count: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { action: 'approve' | 'reject'; comment?: string }]
}>()

const comment = ref('')
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

watch(() => props.modelValue, (value) => {
  if (!value) comment.value = ''
})

const submit = () => {
  if (props.action === 'reject' && !comment.value.trim()) {
    ElMessage.warning('请输入打回原因')
    return
  }
  emit('submit', {
    action: props.action,
    comment: props.action === 'reject' ? comment.value.trim() : undefined
  })
  visible.value = false
}
</script>

<style scoped lang="scss">
.review-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: $text-secondary;

  strong {
    color: $accent-green;
    font-size: 22px;
  }
}

.approve-tip {
  margin: 0;
  color: $text-secondary;
}
</style>

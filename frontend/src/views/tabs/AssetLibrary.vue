<template>
  <div class="asset-library-page">
    <div class="asset-hero">
      <div>
        <p class="eyebrow">Asset Matrix</p>
        <h3>资产库</h3>
        <p class="hero-desc">管理角色、场景、物品与声音资产，供工作流和分镜生成复用。</p>
      </div>
      <el-button class="btn-gradient" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建资产
      </el-button>
    </div>

    <div class="asset-toolbar card-glass border-neon">
      <el-segmented
        v-model="assetType"
        :options="assetTypeList"
        @change="handleTypeChange"
      />
      <el-input
        v-model="searchKeyword"
        class="search-input"
        placeholder="搜索资产名称或描述"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="asset-grid" v-loading="loading">
      <el-row v-if="assetList.length > 0" :gutter="16">
        <el-col
          v-for="asset in assetList"
          :key="asset.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <AssetCard
            :asset="asset"
            @edit="openEditDialog"
            @delete="handleDelete"
            @confirm="handleConfirm"
          />
        </el-col>
      </el-row>

      <EmptyState
        v-else-if="!loading"
        type="asset"
        title="暂无资产"
        description="先创建一个角色、场景或物品资产，后续分镜生成会从这里读取参考信息。"
        show-action
        action-text="新建资产"
        @action="openCreateDialog"
      />
    </div>

    <div v-if="total > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="pageParams.page"
        v-model:page-size="pageParams.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchAssetList"
        @current-change="fetchAssetList"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑资产' : '新建资产'"
      width="640px"
      destroy-on-close
      class="asset-form-dialog"
    >
      <AssetForm
        ref="assetFormRef"
        :mode="isEdit ? 'edit' : 'create'"
        :asset="currentEditAsset"
        :default-type="assetType"
        :project-id="projectId"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="btn-gradient" :loading="submitLoading" @click="handleFormSubmit">
          {{ isEdit ? '保存修改' : '创建资产' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除资产"
      width="440px"
    >
      <p class="delete-text">
        确定要删除「{{ currentDeleteAsset?.name }}」吗？此操作不可恢复。
      </p>
      <el-alert
        v-if="deleteWarning"
        :title="deleteWarning"
        type="warning"
        show-icon
        :closable="false"
      />
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { assetApi } from '@/api/asset'
import AssetCard from '@/components/Asset/AssetCard.vue'
import AssetForm from '@/components/Asset/AssetForm.vue'
import EmptyState from '@/components/Common/EmptyState.vue'
import { AssetType } from '@/types'
import type { AssetCreateRequest, AssetUpdateRequest, AssetVO, PageResult } from '@/types'

const route = useRoute()

// 状态变量
const loading = ref(false)
const submitLoading = ref(false)
const deleteLoading = ref(false)
const dialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const searchKeyword = ref('')
const assetList = ref<AssetVO[]>([])
const total = ref(0)
const isEdit = ref(false)
const currentEditAsset = ref<AssetVO | null>(null)
const currentDeleteAsset = ref<AssetVO | null>(null)
const deleteWarning = ref('')
const assetFormRef = ref<InstanceType<typeof AssetForm>>()

// 当前选中的资产类型
const assetType = ref<'all' | AssetType>('all')

// 资产类型列表
const assetTypeList = [
  { value: 'all', label: '全部' },
  { value: AssetType.Character, label: '角色' },
  { value: AssetType.Scene, label: '场景' },
  { value: AssetType.Prop, label: '物品' },
  { value: AssetType.Voice, label: '声音' }
]

// 分页参数
const pageParams = reactive({
  page: 1,
  size: 12
})

// 项目ID（从路由获取）
const projectId = computed(() => Number(route.params.id))

// 获取资产列表
const fetchAssetList = async () => {
  if (!projectId.value) return
  loading.value = true
  try {
    const params = {
      ...pageParams,
      type: assetType.value === 'all' ? undefined : assetType.value,
      keyword: searchKeyword.value || undefined
    }
    const res = await assetApi.list(projectId.value, params)
    if (res.code === 0) {
      const pageData = res.data as PageResult<AssetVO>
      assetList.value = pageData.records || pageData.list || []
      total.value = pageData.total
    }
  } catch (err) {
    ElMessage.error('获取资产列表失败')
  } finally {
    loading.value = false
  }
}

// 类型切换
const handleTypeChange = () => {
  pageParams.page = 1
  fetchAssetList()
}

// 搜索
const handleSearch = () => {
  pageParams.page = 1
  fetchAssetList()
}

// 打开新建弹窗
const openCreateDialog = () => {
  isEdit.value = false
  currentEditAsset.value = null
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (asset: AssetVO) => {
  isEdit.value = true
  currentEditAsset.value = asset
  dialogVisible.value = true
}

// 表单提交
const handleFormSubmit = async () => {
  const formData = await assetFormRef.value?.validate()
  if (!formData) return

  submitLoading.value = true
  try {
    if (isEdit.value && currentEditAsset.value) {
      // 编辑资产
      await assetApi.update(currentEditAsset.value.id, formData as AssetUpdateRequest)
      ElMessage.success('编辑成功')
    } else {
      // 新建资产
      await assetApi.create(projectId.value, formData as AssetCreateRequest)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchAssetList()
  } catch (err) {
    ElMessage.error(isEdit.value ? '编辑失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 处理删除
const handleDelete = async (asset: AssetVO) => {
  currentDeleteAsset.value = asset
  deleteWarning.value = ''
  try {
    // 检查资产引用
    const res = await assetApi.checkReferences(asset.id)
    if (res.data && res.data.count > 0) {
      deleteWarning.value = `该资产已被 ${res.data.count} 个分镜引用，删除后会影响分镜生成！`
    }
  } catch (err) {
    // 引用检查失败，继续删除流程
  }
  deleteDialogVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!currentDeleteAsset.value || !projectId.value) return
  deleteLoading.value = true
  try {
    await assetApi.delete(currentDeleteAsset.value.id)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    fetchAssetList()
  } catch (err: any) {
    if (err.response?.data.code === 40901) {
      ElMessage.error('资产已被分镜引用，不可删除')
    } else {
      ElMessage.error('删除失败')
    }
  } finally {
    deleteLoading.value = false
  }
}

// 确认资产
const handleConfirm = async (asset: AssetVO) => {
  if (!projectId.value) return
  try {
    await assetApi.confirm(asset.id)
    ElMessage.success('资产已确认')
    fetchAssetList()
  } catch (err) {
    ElMessage.error('确认失败')
  }
}

// 页面加载时获取资产列表
onMounted(() => {
  fetchAssetList()
})
</script>

<style scoped lang="scss">
.asset-library-page {
  width: 100%;
}

.asset-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 18px;

  .eyebrow {
    margin: 0 0 6px;
    color: $accent-green;
    font-size: 12px;
    letter-spacing: 0;
    text-transform: uppercase;
  }

  h3 {
    margin: 0;
    color: $text-primary;
    font-size: 26px;
    font-weight: 700;
  }

  .hero-desc {
    margin: 8px 0 0;
    color: $text-secondary;
    font-size: 14px;
  }
}

.asset-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px;
  margin-bottom: 18px;
  border-radius: 8px;

  .search-input {
    max-width: 420px;
  }
}

.asset-grid {
  min-height: 360px;
}

:deep(.asset-card) {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

.delete-text {
  margin: 0 0 14px;
  color: $text-secondary;
  line-height: 1.6;
}

:deep(.asset-form-dialog .el-dialog) {
  background: rgba(16, 18, 32, 0.96);
}

@media (max-width: 768px) {
  .asset-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .asset-toolbar {
    align-items: stretch;
    flex-direction: column;

    .search-input {
      max-width: none;
    }
  }
}
</style>

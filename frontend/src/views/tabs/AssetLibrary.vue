<!-- views/tabs/AssetLibrary.vue — 资产库页面
     系分第 4.4 节：资产类型 Tab + 搜索 + 卡片网格 -->
<template>
  <div class="asset-library-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 资产类型 Tab -->
        <el-radio-group v-model="assetType" class="asset-type-tabs" @change="handleTypeChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="character">角色</el-radio-button>
          <el-radio-button label="scene">场景</el-radio-button>
          <el-radio-button label="prop">物品</el-radio-button>
          <el-radio-button label="voice">声音</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-right">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索资产名称"
          style="width: 240px; margin-right: 12px"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <!-- 新建资产按钮 -->
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建资产
        </el-button>
      </div>
    </div>

    <!-- 资产卡片列表 -->
    <div class="asset-card-list" v-loading="loading">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="asset in assetList" :key="asset.id">
          <AssetCard :asset="asset" @edit="openEditDialog" @delete="handleDelete" @confirm="handleConfirm" />
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <div v-if="assetList.length === 0 && !loading" class="empty-container">
        <el-empty description="暂无资产，点击「新建资产」开始创建" />
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
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

    <!-- 新建/编辑资产弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑资产' : '新建资产'"
      width="600px"
      destroy-on-close
    >
      <AssetForm
        ref="formRef"
        :asset-type="assetType === 'all' ? 'character' : assetType"
        :edit-data="isEdit ? currentEditAsset : null"
        @submit="handleFormSubmit"
      />
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="确认删除"
      width="400px"
    >
      <p>确定要删除资产「{{ currentDeleteAsset?.name }}」吗？</p>
      <p v-if="deleteWarning" class="warning-text">{{ deleteWarning }}</p>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete" :loading="deleteLoading">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { assetApi } from '@/api/asset'
import type { AssetVO } from '@/types'
// 缺失组件，临时注释
// import AssetCard from '@/components/Asset/AssetCard.vue'
// import AssetForm from '@/components/Asset/AssetForm.vue'

const route = useRoute()
const formRef = ref<any>()

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

// 当前选中的资产类型
const assetType = ref<'all' | 'character' | 'scene' | 'prop' | 'voice'>('all')

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
      assetList.value = res.data.records
      total.value = res.data.total
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
const handleFormSubmit = async (formData: any) => {
  submitLoading.value = true
  try {
    if (isEdit.value && currentEditAsset.value) {
      // 编辑资产
      await assetApi.update(projectId.value, currentEditAsset.value.id, formData)
      ElMessage.success('编辑成功')
    } else {
      // 新建资产
      await assetApi.create(projectId.value, formData)
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
    const res = await assetApi.checkReferences(projectId.value, asset.id)
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
    await assetApi.delete(projectId.value, currentDeleteAsset.value.id)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    fetchAssetList()
  } catch (err: any) {
    if (err.response?.data?.code === 40901) {
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
    await assetApi.confirm(projectId.value, asset.id)
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.asset-type-tabs {
  :deep(.el-radio-button__inner) {
    border-radius: 4px;
    margin-right: 8px;
    border: 1px solid $border-color;
    background: $bg-color;
    color: $text-secondary;

    &.is-active {
      background: $primary-color;
      border-color: $primary-color;
      color: #fff;
    }
  }
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.asset-card-list {
  min-height: 500px;
}

.empty-container {
  padding: 80px 0;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.warning-text {
  color: $danger-color;
  margin-top: 8px;
  font-size: 14px;
}
</style>

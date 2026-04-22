<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { assetApi } from '@/api/asset'
import type { AssetVO, AssetCreateRequest, AssetUpdateRequest, PageResult } from '@/types'

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

// 当前选中的资产类型
const assetType = ref<'all' | 'character' | 'scene' | 'prop' | 'voice'>('all')

// 资产类型列表
const assetTypeList = [
  { value: 'all', label: '全部' },
  { value: 'character', label: '角色' },
  { value: 'scene', label: '场景' },
  { value: 'prop', label: '物品' },
  { value: 'voice', label: '声音' }
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
      assetList.value = pageData.records || pageData.list
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
const handleFormSubmit = async (formData: any) => {
  submitLoading.value = true
  try {
    if (isEdit.value && currentEditAsset.value) {
      // 编辑资产
      await assetApi.update(currentEditAsset.value.id, formData)
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

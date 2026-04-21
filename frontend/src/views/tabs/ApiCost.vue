<!-- views/tabs/ApiCost.vue — API 消耗看板页面占位
     系分第 4.7 节：统计面板 + 趋势图表 -->
<template>
  <div class="api-cost-page">
    <h2>API 消耗看板</h2>
    <p class="placeholder-text">
      调用次数统计 + 费用趋势图 + 按模型/任务类型分解
    </p>
    <el-empty description="Sprint 4 实现 API 消耗看板" />
  </div>
</template>

<script setup lang="ts">
// TODO: Sprint 4 实现 API 消耗看板
</script>

<style scoped lang="scss">
.api-cost-page {
  h2 {
    color: $text-primary;
    margin-bottom: 8px;
  }
  .placeholder-text {
    color: $text-secondary;
    margin-bottom: 24px;
  }
}
</style>
